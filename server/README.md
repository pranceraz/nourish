# Private meal draft service

Run from the repository root with Python 3.12. The server reads the same USDA survey-food index as Android. Offline search and meal composition work on a fresh install without this service.

```sh
python -m venv .venv
. .venv/bin/activate
pip install -r server/requirements.txt
python -m unittest server.test_service
uvicorn server.service:app --host 127.0.0.1 --port 8000
```

## Private deployment

Place the service behind HTTPS on a host you control. Configure the reverse proxy to cap request bodies at 1.25 MB, disable body and Authorization-header logging, and restrict access to the app and MCP routes. The reference rate limiter is per user and worker; use a shared limit at the proxy for multiple workers. Configure environment values using your deployment's secret store.

| Setting | Use |
|---|---|
| `OPENAI_API_KEY` | Server credential for optional OpenAI Responses text and image identification or requested goal suggestions. |
| `NOURISH_MODEL` | Optional vision-capable Responses model supporting structured output. Default `gpt-4.1-mini`. |
| `NOURISH_ACCESS` | JSON object mapping local owner IDs to distinct SHA-256 token digests, e.g. `{"alice":{"app":"<digest>","lookup":"<different digest>"}}`. Generate separate random tokens of at least 32 characters for every owner and role. Removing a digest revokes that grant. |
| `NOURISH_VAULT_KEY` | Base64-encoded random 32-byte AES key held separately from the SQLite file. Preserve this secret across restarts. |
| `NOURISH_VAULT_PATH` | Absolute path in a private, persistent server directory writable by the service account. |
| `NOURISH_HOSTS`, `NOURISH_ORIGINS` | Comma-separated exact public host names (with port where applicable) and HTTPS origins allowed by the MCP transport. |

Generate a bearer token privately with `python -c 'import secrets; print(secrets.token_urlsafe(32))'`. Hash its literal UTF-8 bytes without a trailing newline. Generate the vault secret with `python -c 'import base64,secrets; print(base64.b64encode(secrets.token_bytes(32)).decode())'`. Supply the original **app** token and HTTPS origin in Android Settings → AI connection & personal USDA key.

## Personal USDA lookup

A user can obtain a [USDA key](https://fdc.nal.usda.gov/api-key-signup/) and enter it in Settings. The app sends it only when they tap Save USDA key. The service encrypts it using AES-GCM bound to the authenticated owner. Checking status returns a boolean. Replace or remove the key while connected. Disconnecting a device clears its local access credential. Revoking a compromised USDA key is done with its issuer.

The official MCP SDK serves Streamable HTTP at `https://your-host/mcp/`. Configure a compatible MCP client with that owner's **lookup** bearer credential. The single read-only tool `search_usda_foods(query)` sends the food name and that owner's key to USDA over HTTPS. It returns FDC IDs, names, nutrient amounts and units. The result never includes the key, and the tool cannot read or write the diary. Android drafts use the bundled dataset independently. Hosted ChatGPT sign-in, OAuth account linking and diary writes are P4.

The service stores personal lookup keys, not meal drafts. On an explicit Generate request it sends the description and any selected meal photo to OpenAI with `store: false`. Applies are saved locally in Android only after user confirmation. Provider processing is subject to the operator's provider account terms. Backend tests substitute deterministic model and USDA responses. Use real credentials on a private TLS deployment to exercise live upstream requests.
