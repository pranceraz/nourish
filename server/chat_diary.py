"""OAuth-protected draft/confirm MCP resource and append-only phone delivery feed."""

import csv
import hashlib
import json
import os
import sqlite3
import time
import uuid
from contextlib import closing, contextmanager
from datetime import date
from decimal import Decimal, ROUND_HALF_UP
from functools import lru_cache
from pathlib import Path
from typing import Annotated, Literal

import jwt
from fastapi import Depends, HTTPException, Request
from fastapi.responses import JSONResponse
from mcp.server.fastmcp import Context, FastMCP
from mcp.server.transport_security import TransportSecuritySettings
from mcp.types import ToolAnnotations
from pydantic import BaseModel, ConfigDict, Field, model_validator
from starlette.concurrency import run_in_threadpool

SCOPE = "nourish.diary.write"
RESOURCE_PATH = "/chat/mcp"
METADATA_PATH = "/.well-known/oauth-protected-resource/chat/mcp"
Amount = Annotated[float, Field(ge=0, le=99999, allow_inf_nan=False)]
READ = ToolAnnotations(readOnlyHint=True, destructiveHint=False, openWorldHint=False)


def resource():
    origin = os.getenv("NOURISH_PUBLIC_ORIGIN", "").rstrip("/")
    if not origin.startswith("https://"):
        raise ValueError("Configure HTTPS resource origin")
    return origin + RESOURCE_PATH


@lru_cache
def key_client(url):
    if not url.startswith("https://"):
        raise ValueError("Configure an HTTPS JWKS endpoint")
    return jwt.PyJWKClient(url, timeout=5)


def subject(header):
    scheme, _, token = header.partition(" ")
    if scheme.lower() != "bearer" or len(token) > 16000:
        raise ValueError("Bearer token required")
    issuer = os.environ["NOURISH_OAUTH_ISSUER"]
    if not issuer.startswith("https://"):
        raise ValueError("HTTPS issuer required")
    signing = key_client(os.environ["NOURISH_OAUTH_JWKS"]).get_signing_key_from_jwt(token)
    claims = jwt.decode(
        token,
        signing.key,
        algorithms=["RS256"],
        audience=resource(),
        issuer=issuer,
        options={"require": ["exp", "iss", "aud", "sub"]},
    )
    user = claims["sub"]
    if SCOPE not in str(claims.get("scope", "")).split() or user not in json.loads(
        os.getenv("NOURISH_ACCESS", "{}")
    ):
        raise ValueError("Account or scope unavailable")
    return user


@lru_cache
def nutrient_definitions():
    path = Path(__file__).resolve().parents[1] / "app/src/main/assets/nutrients.tsv"
    with path.open() as table:
        return {r["number"]: r for r in csv.DictReader(table, delimiter="\t")}


MICRONUTRIENTS = {k: v for k, v in nutrient_definitions().items() if v["group"] in ("Vitamins", "Minerals")}
TOOL_META = {"securitySchemes": [{"type": "oauth2", "scopes": [SCOPE]}]}


class Estimate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    food: str = Field(min_length=1, max_length=100)
    kcal: Amount
    protein_g: Amount
    carbs_g: Amount
    fat_g: Amount
    micronutrients: dict[str, Amount] = Field(
        default_factory=dict,
        description="Optional known per-100 g values. Omit unknowns. "
        + ", ".join(f"{k} {v['name']} ({v['unit']})" for k, v in MICRONUTRIENTS.items()),
    )
    basis: str = Field(
        min_length=1,
        max_length=240,
        description="Where these per-100 g values came from; identify GPT estimates.",
    )


class Ingredient(BaseModel):
    model_config = ConfigDict(extra="forbid")
    grams: float = Field(gt=0, le=10000, multiple_of=0.001)
    fdc_id: str | None = None
    estimate: Estimate | None = None
    assumption: str = Field(default="", max_length=200)

    @model_validator(mode="after")
    def one_source(self):
        if (self.fdc_id is None) == (self.estimate is None):
            raise ValueError("Use one catalog food or one sourced estimate")
        return self


@contextmanager
def database():
    with closing(sqlite3.connect(os.environ["NOURISH_DIARY_PATH"], timeout=10)) as db, db:
        db.row_factory = sqlite3.Row
        db.execute(
            "CREATE TABLE IF NOT EXISTS drafts(id TEXT PRIMARY KEY, owner TEXT, expires INTEGER, payload TEXT)"
        )
        db.execute(
            "CREATE TABLE IF NOT EXISTS meals(seq INTEGER PRIMARY KEY AUTOINCREMENT, id TEXT UNIQUE, owner TEXT, payload TEXT)"
        )
        db.execute("CREATE INDEX IF NOT EXISTS meal_owner_seq ON meals(owner,seq)")
        yield db


def rounded(value):
    return float(value.quantize(Decimal(".001"), rounding=ROUND_HALF_UP))


def build_meal(name, day, meal, mode, ingredients, foods):
    name = name.strip()
    if not name or len(name.encode("utf-16-le")) > 200:
        raise ValueError("Choose a dish name up to 100 characters")
    items = []
    totals = {}
    for ingredient in ingredients:
        if ingredient.fdc_id is not None:
            row = foods.get(ingredient.fdc_id)
            if row is None:
                raise ValueError("Unknown FDC ID. Search common foods first.")
            label = row["name"][:100]
            source = "USDA FDC " + row["fdc_id"]
            values = {k: float(v) for k, v in row.items() if k.isdigit() and v}
        else:
            e = ingredient.estimate
            label, source = e.food.strip(), e.basis.strip()
            if not label or not source or e.micronutrients.keys() - MICRONUTRIENTS.keys():
                raise ValueError("Provide a food name, source and supported nutrient codes")
            values = (
                dict(zip(("208", "203", "205", "204"), (e.kcal, e.protein_g, e.carbs_g, e.fat_g)))
                | e.micronutrients
            )
        if len(label.encode("utf-16-le")) > 200:
            raise ValueError("Choose a shorter ingredient name")
        for key, amount in values.items():
            totals[key] = (
                totals.get(key, Decimal(0)) + Decimal(str(amount)) * Decimal(str(ingredient.grams)) / 100
            )
        items.append(
            {
                "food": label,
                "grams": ingredient.grams,
                "source": source,
                "assumption": ingredient.assumption,
                "per100": values,
            }
        )
    if any(v > 99999 for k, v in totals.items() if k in ("208", "203", "205", "204")):
        raise ValueError("Portion exceeds supported diary range")
    return {
        "name": name,
        "date": str(day),
        "meal": meal,
        "mode": mode,
        "ingredients": items,
        "totals": {k: rounded(v) for k, v in totals.items()},
        "partial": [k for k in totals if any(k not in item["per100"] for item in items)],
    }


def save_draft(user, payload, replaces=None):
    draft_id = str(uuid.uuid4())
    with database() as db:
        db.execute("DELETE FROM drafts WHERE expires < ?", (int(time.time()),))
        if replaces is not None:
            old = db.execute("DELETE FROM drafts WHERE owner=? AND id=?", (user, str(replaces)))
            if old.rowcount != 1:
                raise ValueError("The earlier draft has expired or was already confirmed")
        db.execute(
            "INSERT INTO drafts VALUES(?,?,?,?)",
            (draft_id, user, int(time.time()) + 1800, json.dumps(payload)),
        )
    summary = {
        row["name"]: {
            "value": payload["totals"][key],
            "unit": row["unit"],
            "partial": key in payload["partial"],
        }
        for key, row in nutrient_definitions().items()
        if key in payload["totals"]
    }
    return {
        "draft_id": draft_id,
        "review": payload,
        "nutrition": summary,
        "status": "awaiting confirmation",
    }


def confirm(user, draft_id):
    with database() as db:
        db.execute("BEGIN IMMEDIATE")
        row = db.execute(
            "SELECT seq,payload FROM meals WHERE owner=? AND id=?", (user, draft_id)
        ).fetchone()
        if row is None:
            draft = db.execute(
                "SELECT payload FROM drafts WHERE owner=? AND id=? AND expires>=?",
                (user, draft_id, int(time.time())),
            ).fetchone()
            if not draft:
                raise ValueError("Draft unavailable. Prepare and review a new draft.")
            seq = db.execute(
                "INSERT INTO meals(id,owner,payload) VALUES(?,?,?)", (draft_id, user, draft["payload"])
            ).lastrowid
            db.execute("DELETE FROM drafts WHERE id=?", (draft_id,))
            payload = json.loads(draft["payload"])
        else:
            seq, payload = row["seq"], json.loads(row["payload"])
        return {
            "status": "saved",
            "receipt": draft_id,
            "sequence": seq,
            "date": payload["date"],
            "meal": payload["name"],
        }


def install(app, app_user, catalog, matches, within_budget):
    chat = FastMCP(
        "Nourish diary",
        stateless_http=True,
        json_response=True,
        instructions="Review food, sources, portions, date, meal and totals with the user. After corrections prepare a fresh draft. Only commit the latest draft after explicit user approval.",
        streamable_http_path="/mcp",
        transport_security=TransportSecuritySettings(
            enable_dns_rebinding_protection=True,
            allowed_hosts=os.getenv("NOURISH_HOSTS", "localhost:*,127.0.0.1:*").split(","),
            allowed_origins=os.getenv("NOURISH_ORIGINS", "https://localhost").split(","),
        ),
    )

    @lru_cache
    def foods():
        return {r["fdc_id"]: r for r in catalog()}

    @chat.tool(meta=TOOL_META, annotations=READ)
    def search_common_foods(query: Annotated[str, Field(min_length=1, max_length=120)]) -> list[dict]:
        """Find ordinary food and dish matches for a description or foods identified in a photo."""
        return [{"fdc_id": i, "food": foods()[i]["name"]} for i in matches(query)]

    @chat.tool(meta=TOOL_META, annotations=ToolAnnotations(destructiveHint=False, openWorldHint=False))
    def prepare_meal(
        name: Annotated[str, Field(min_length=1, max_length=100)],
        day: date,
        meal: Literal["Breakfast", "Lunch", "Dinner", "Snack"],
        mode: Literal["ingredients", "dish"],
        ingredients: Annotated[list[Ingredient], Field(min_length=1, max_length=20)],
        ctx: Context,
        replaces_draft_id: uuid.UUID | None = None,
    ) -> dict[str, object]:
        """Prepare a temporary review. For corrections, supply replaces_draft_id to retire the old review. Ask before saving."""
        user = ctx.request_context.request.state.diary_owner
        if not within_budget(user, "chat_draft", 30):
            raise ValueError("Try again in a minute")
        return save_draft(user, build_meal(name, day, meal, mode, ingredients, foods()), replaces_draft_id)

    @chat.tool(
        meta=TOOL_META,
        annotations=ToolAnnotations(
            readOnlyHint=False, destructiveHint=False, idempotentHint=True, openWorldHint=False
        ),
    )
    def confirm_meal(draft_id: uuid.UUID, confirmed: Literal[True], ctx: Context) -> dict[str, object]:
        """Write the exact reviewed draft only after the user explicitly confirms. Retrying returns the same receipt."""
        return confirm(ctx.request_context.request.state.diary_owner, str(draft_id))

    @app.get(METADATA_PATH)
    def metadata():
        return {
            "resource": resource(),
            "authorization_servers": [os.environ["NOURISH_OAUTH_ISSUER"]],
            "scopes_supported": [SCOPE],
            "bearer_methods_supported": ["header"],
        }

    @app.middleware("http")
    async def protect_chat(request: Request, call_next):
        if request.url.path.startswith("/chat/"):
            try:
                request.state.diary_owner = await run_in_threadpool(
                    subject, request.headers.get("authorization", "")
                )
            except (ValueError, KeyError, jwt.PyJWTError):
                origin = os.getenv("NOURISH_PUBLIC_ORIGIN", "").rstrip("/")
                challenge = f'Bearer resource_metadata="{origin}{METADATA_PATH}", scope="{SCOPE}"'
                return JSONResponse(
                    {"detail": "Connect your Nourish account"}, 401, headers={"WWW-Authenticate": challenge}
                )
        return await call_next(request)

    @app.get("/v1/diary/incoming")
    def incoming(after: Annotated[int, Field(ge=0)] = 0, user: str = Depends(app_user)):
        with database() as db:
            rows = db.execute(
                "SELECT seq,id,payload FROM meals WHERE owner=? AND seq>? ORDER BY seq LIMIT 20",
                (user, after),
            ).fetchall()
        account = hashlib.sha256((resource() + "\0" + user).encode()).hexdigest()
        return {
            "account": account,
            "meals": [{"seq": r["seq"], "id": r["id"], "meal": json.loads(r["payload"])} for r in rows],
        }

    app.mount("/chat", chat.streamable_http_app())
    return chat
