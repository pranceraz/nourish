import asyncio
import hashlib
import json
import os
import tempfile
import time
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

import httpx
import jwt
from mcp import ClientSession
from mcp.client.streamable_http import streamable_http_client
from cryptography.hazmat.primitives.asymmetric import rsa
from fastapi.testclient import TestClient
from server import chat_diary as diary
from server.service import create_app, matches

FIXTURE = Path(__file__).resolve().parents[1] / "app/src/androidTest/assets/confirmed-chat-meals.json"


class ChatDiaryTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.tokens = {user: user * 40 for user in ["alice", "bob"]}
        settings = {
            "NOURISH_ACCESS": json.dumps(
                {u: {"app": hashlib.sha256(t.encode()).hexdigest()} for u, t in self.tokens.items()}
            ),
            "NOURISH_PUBLIC_ORIGIN": "https://nourish.example",
            "NOURISH_OAUTH_ISSUER": "https://identity.example",
            "NOURISH_OAUTH_JWKS": "https://identity.example/keys",
            "NOURISH_DIARY_PATH": tmp.name + "/meals.db",
        }
        self.enterContext(patch.dict(os.environ, settings))
        self.key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        keys = self.enterContext(patch.object(diary, "key_client"))
        keys.return_value.get_signing_key_from_jwt.return_value = SimpleNamespace(key=self.key.public_key())
        self.app = create_app()
        self.client = self.enterContext(TestClient(self.app, base_url="http://localhost:8000"))

    def token(self, user, **overrides):
        claims = {
            "sub": user,
            "aud": diary.resource(),
            "iss": os.environ["NOURISH_OAUTH_ISSUER"],
            "exp": int(time.time()) + 120,
            "scope": diary.SCOPE,
        }
        return jwt.encode(claims | overrides, self.key, algorithm="RS256")

    def rpc(self, name, args, user="alice"):
        headers = {
            "Authorization": "Bearer " + self.token(user),
            "Accept": "application/json, text/event-stream",
        }
        response = self.client.post(
            "/chat/mcp",
            headers=headers,
            json={
                "jsonrpc": "2.0",
                "id": 1,
                "method": "tools/call",
                "params": {"name": name, "arguments": args},
            },
        )
        self.assertEqual(response.status_code, 200, response.text)
        return response.json()["result"]

    def feed(self, user, after=0):
        return self.client.get(
            "/v1/diary/incoming",
            params={"after": after},
            headers={"Authorization": "Bearer " + self.tokens[user]},
        ).json()

    def test_sdk_client_discovers_and_calls_tools(self):
        async def check():
            transport = httpx.ASGITransport(app=self.app)
            headers = {"Authorization": "Bearer " + self.token("alice")}
            async with httpx.AsyncClient(transport=transport, headers=headers) as http:
                url = "http://localhost:8000/chat/mcp"
                async with streamable_http_client(url, http_client=http) as (read, write, _):
                    async with ClientSession(read, write) as client:
                        await client.initialize()
                        tools = await client.list_tools()
                        self.assertEqual(
                            {t.name for t in tools.tools},
                            {"prepare_meal", "confirm_meal", "search_common_foods"},
                        )
                        foods = await client.call_tool("search_common_foods", {"query": "goat milk"})
                        self.assertIn("Goat milk", foods.content[0].text)

        asyncio.run(check())

    def test_oauth_discovery_and_isolation(self):
        self.assertIn(diary.METADATA_PATH, self.client.post("/chat/mcp").headers["www-authenticate"])
        self.assertEqual(self.client.get(diary.METADATA_PATH).json()["resource"], diary.resource())
        for change in [
            {"aud": "another-resource"},
            {"scope": "lookup"},
            {"exp": 1},
            {"iss": "https://other.example"},
            {"sub": "unknown"},
        ]:
            self.assertEqual(
                self.client.post(
                    "/chat/mcp", headers={"Authorization": "Bearer " + self.token("alice", **change)}
                ).status_code,
                401,
            )
        self.assertEqual(
            self.client.get(
                "/v1/diary/incoming", headers={"Authorization": "Bearer " + self.token("alice")}
            ).status_code,
            401,
        )

    def test_corrected_portions_replace_old_draft_and_expired_review_cannot_save(self):
        args = {
            "name": "A bowl",
            "day": "2026-06-10",
            "meal": "Lunch",
            "mode": "dish",
            "ingredients": [
                {
                    "grams": 100,
                    "estimate": {
                        "food": "Soup",
                        "basis": "label",
                        "kcal": 100,
                        "carbs_g": 10,
                        "fat_g": 4,
                        "protein_g": 6,
                        "micronutrients": {"301": 50},
                    },
                }
            ],
        }

        def prepare(**extra):
            return self.rpc("prepare_meal", args | extra)["structuredContent"]

        original = prepare()
        args["ingredients"][0]["grams"] = 150
        revised = prepare(replaces_draft_id=original["draft_id"])
        self.assertEqual(revised["nutrition"]["Calcium"]["value"], 75)
        self.assertTrue(
            self.rpc("confirm_meal", {"draft_id": original["draft_id"], "confirmed": True})["isError"]
        )
        with diary.database() as db:
            db.execute("UPDATE drafts SET expires=0")
        self.assertTrue(
            self.rpc("confirm_meal", {"draft_id": revised["draft_id"], "confirmed": True})["isError"]
        )
        self.assertEqual(self.feed("alice")["meals"], [])

    def test_confirmed_meals_reach_phone_feed_once(self):
        food_id = matches("goat milk")[0]
        for index, mode in enumerate(["dish", "ingredients"]):
            args = {
                "name": "Morning bowl",
                "day": "2026-06-10",
                "meal": "Breakfast",
                "mode": mode,
                "ingredients": [
                    {"grams": 100, "fdc_id": food_id, "assumption": "100 g milk"},
                    {
                        "grams": 50,
                        "estimate": {
                            "food": "Granola",
                            "kcal": 420,
                            "protein_g": 10,
                            "carbs_g": 60,
                            "fat_g": 16,
                            "basis": "GPT estimate from the photo",
                        },
                    },
                ],
            }
            result = self.rpc("prepare_meal", args)
            self.assertFalse(result.get("isError"), result)
            review = result["structuredContent"]
            self.assertEqual(review["review"]["totals"]["208"], 279)
            self.assertEqual(
                review["nutrition"]["Energy"], {"value": 279, "unit": "kcal", "partial": False}
            )
            draft_id = review["draft_id"]
            self.assertEqual(len(self.feed("alice")["meals"]), index)
            confirm = {"draft_id": draft_id, "confirmed": True}
            self.assertTrue(self.rpc("confirm_meal", confirm, "bob")["isError"])
            self.assertTrue(self.rpc("confirm_meal", confirm | {"confirmed": False})["isError"])
            receipt = self.rpc("confirm_meal", confirm)["structuredContent"]
            self.assertEqual(self.rpc("confirm_meal", confirm)["structuredContent"], receipt)
            self.assertEqual(receipt["sequence"], index + 1)
        self.assertEqual(self.feed("alice", 2)["meals"], [])
        self.assertEqual(self.feed("bob")["meals"], [])
        feed = self.feed("alice")
        for i, row in enumerate(feed["meals"]):
            row["id"] = f"00000000-0000-4000-8000-{i + 1:012d}"
        if os.getenv("REBUILD_CHAT_FIXTURE") == "1":
            FIXTURE.parent.mkdir(parents=True, exist_ok=True)
            FIXTURE.write_text(json.dumps(feed, indent=2) + "\n")
        self.assertEqual(feed, json.loads(FIXTURE.read_text()))
