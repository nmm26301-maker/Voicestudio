"""Exercise deployed CPU handler with a fake GPU worker; no credits/GPU claimed."""
import importlib.util
import os
import sys
from pathlib import Path
from fastapi.testclient import TestClient
root = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("voicestudio_modal", root / "server/modal_app.py")
m = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = m
spec.loader.exec_module(m)
m.usage = {}
os.environ["VOICE_API_TOKEN"] = "fixture-only-token"
os.environ["VOICE_DAILY_MAX_CALLS"] = "2"
calls = []
fail = [False]
class Remote:
    async def aio(self, task):
        calls.append(task)
        if fail[0]:
            raise RuntimeError("GPU unavailable")
        return b"fixture-WAV"
class Worker:
    class Method:
        remote = Remote()
    generate = Method()
m.Speech = Worker
web = m.create_api()
client = TestClient(web)
header = {"Authorization": "Bearer fixture-only-token"}
task = {"input": "全文第一句。\n必须包括最后一句。", "num_step": 16, "speed": 1.25, "seed": 123}
checks = 0
def check(value):
    global checks
    checks += 1
    assert value
check(client.post("/v1/audio/speech", json=task).status_code == 401)
check(not calls and not m.usage)
for change in [{"input": " "}, {"input": "x" * 2001}, {"num_step": 7}, {"voice": "person"}, {"model": "other"}, {"seed": -1}, {"response_format": "mp3"}, {"speed": 3}]:
    check(client.post("/v1/audio/speech", json={**task, **change}, headers=header).status_code == 422)
check(not calls and not m.usage)
response = client.post("/v1/audio/speech", json=task, headers=header)
check(response.status_code == 200 and response.headers["content-type"] == "audio/wav")
check(calls[0]["input"] == task["input"] and calls[0]["seed"] == 123 and calls[0]["num_step"] == 16)
fail[0] = True
check(client.post("/v1/audio/speech", json=task, headers=header).status_code == 503)
check(client.post("/v1/audio/speech", json=task, headers=header).status_code == 429)
check(len(calls) == 2 and list(m.usage.values()) == [2])
print(f"Modal CPU handler: {checks} auth/validation/full-text/quota/failure checks passed; fake GPU only.")
