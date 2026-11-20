import base64
import hashlib
import json
import os
import tempfile
import unittest
from unittest.mock import AsyncMock, patch

from fastapi.testclient import TestClient
from server import service


class DraftServiceTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.app_token, self.tool_token, other = "a"*40, "t"*40, "b"*40
        digest = lambda s: hashlib.sha256(s.encode()).hexdigest()
        env = patch.dict(os.environ, {
            "NOURISH_ACCESS":json.dumps({"one":{"app":digest(self.app_token),"lookup":digest(self.tool_token)},
                                         "two":{"app":digest(other)}}),
            "NOURISH_VAULT_KEY":base64.b64encode(os.urandom(32)).decode(),
            "NOURISH_VAULT_PATH":self.tmp.name + "/keys.db",
        })
        env.start()
        self.addCleanup(env.stop)
        self.client = TestClient(service.app)
        self.headers = {"Authorization":"Bearer " + self.app_token}

    def test_draft_has_real_usda_sources_and_no_write(self):
        identified = service.Identification(name="Soup and rice",question="Was oil added?",
            ingredients=[service.Ingredient(food="lentil soup",grams=180,assumption="One small bowl")])
        with patch.object(service,"generate",AsyncMock(return_value=identified)):
            res = self.client.post("/v1/drafts",json={"description":"soup and rice"},headers=self.headers)
        self.assertEqual(res.status_code,200,res.text)
        item = res.json()["ingredients"][0]
        self.assertEqual(item["grams"],180)
        self.assertTrue(item["candidates"])
        self.assertIn("lentil",next(r["name"].lower() for r in service.catalog() if r["fdc_id"] == item["candidates"][0]))

    def test_personal_key_is_scoped_encrypted_replaceable_and_deletable(self):
        url = "/v1/settings/usda"
        secret = "example-private-usda-key"
        self.assertEqual(self.client.put(url,json={"key":secret},headers=self.headers).status_code,204)
        self.assertNotIn(secret.encode(),service.Path(os.environ["NOURISH_VAULT_PATH"]).read_bytes())
        self.assertEqual(self.client.get(url,headers=self.headers).json(),{"configured":True})
        self.assertFalse(self.client.get(url,headers={"Authorization":"Bearer "+"b"*40}).json()["configured"])
        self.assertEqual(self.client.get(url,headers={"Authorization":"Bearer "+self.tool_token}).status_code,401)
        self.client.put(url,json={"key":"replacement-key"},headers=self.headers)
        self.assertEqual(service.vault("one","read"),"replacement-key")
        self.assertEqual(self.client.delete(url,headers=self.headers).status_code,204)
        self.assertIsNone(service.vault("one","read"))

    def test_mcp_read_only_tool_uses_connected_users_key(self):
        with TestClient(service.app,base_url="http://localhost:8000") as client:
            headers = {"Authorization":"Bearer "+self.tool_token,"Accept":"application/json, text/event-stream"}
            def rpc(method,params):
                return client.post("/mcp/",headers=headers,json={"jsonrpc":"2.0","id":1,"method":method,"params":params})
            self.assertEqual(client.get("/mcp/",headers=self.headers).status_code,401)
            listed = rpc("tools/list",{}).json()["result"]["tools"]
            self.assertEqual([t["name"] for t in listed],["search_usda_foods"])
            data = {"name":"search_usda_foods","arguments":{"query":"rice"}}
            self.assertIn("personal USDA key",rpc("tools/call",data).text)
            service.vault("one","save","test-user-secret")
            answer = service.httpx.Response(200,request=service.httpx.Request("POST","https://api.nal.usda.gov"),
                json={"foods":[{"fdcId":12,"description":"Rice","foodNutrients":[{"nutrientName":"Protein","unitName":"G","value":2.7}]}]})
            with patch.object(service.httpx.AsyncClient,"post",AsyncMock(return_value=answer)) as upstream:
                result = rpc("tools/call",data)
                self.assertIn("Rice",result.text)
                self.assertNotIn("test-user-secret",result.text)
                self.assertEqual(upstream.await_args.kwargs["headers"]["X-Api-Key"],"test-user-secret")

    def test_inputs_and_unconfigured_provider(self):
        self.assertEqual(self.client.post("/v1/drafts",json={"description":"rice"}).status_code,401)
        res = self.client.post("/v1/drafts",json={"description":"","jpeg":"not-an-image"},headers=self.headers)
        self.assertEqual(res.status_code,422)
        with patch.dict(os.environ,{"OPENAI_API_KEY":""}):
            self.assertEqual(self.client.post("/v1/drafts",json={"description":"rice"},headers=self.headers).status_code,503)
        self.assertEqual(self.client.post("/v1/goals",json={"nutrient":"bad","context":"a"},headers=self.headers).status_code,422)
        proposed = service.GoalSuggestion(amount=15,rationale="Requested personal target.")
        with patch.object(service,"generate",AsyncMock(return_value=proposed)):
            self.assertEqual(self.client.post("/v1/goals",headers=self.headers,json={"nutrient":"303","context":"15 mg"}).json(),proposed.model_dump())
