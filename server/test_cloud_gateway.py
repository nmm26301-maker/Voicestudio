import io
import math
import os
import struct
import wave

import httpx
import pytest
from fastapi.testclient import TestClient

from server import cloud_gateway as g


def wav(seconds=5, rate=24000, silence=False):
    out=io.BytesIO()
    with wave.open(out,"wb") as writer:
        writer.setnchannels(1);writer.setsampwidth(2);writer.setframerate(rate)
        frames=rate*seconds
        audio=[0 if silence else int(7000*math.sin(2*math.pi*220*i/rate)) for i in range(frames)]
        writer.writeframes(struct.pack("<"+"h"*frames,*audio))
    return out.getvalue()


@pytest.fixture
def client(tmp_path, monkeypatch):
    monkeypatch.setattr(g, "STORE", tmp_path)
    monkeypatch.setenv("VOICE_GATEWAY_TOKEN", "test-gateway-only")
    return TestClient(g.app)


HEADERS={"Authorization": "Bearer test-gateway-only"}


def test_requires_gateway_token(client):
    assert client.post("/v1/voices/reference", files={"file":("voice.wav", wav(), "audio/wav")}).status_code==401
    assert client.post("/v1/audio/speech", json={"input":"你好"}).status_code==401


def test_reference_upload_and_persistence(client):
    response=client.post("/v1/voices/reference",headers=HEADERS,
                         files={"file":("voice.wav",wav(),"audio/wav")},
                         data={"transcript":"参考文字"})
    assert response.status_code==200,response.text
    voice=response.json()["voice"]
    assert voice.startswith("ref-")
    recording,text=g.read_reference(voice)
    assert text=="参考文字" and recording==wav()
    assert g.check_wav(recording,reference=True)==5


def test_silent_and_invalid_reference_rejected(client):
    for audio in (b"not a wav",wav(silence=True),wav(seconds=2)):
        res=client.post("/v1/voices/reference",headers=HEADERS,
                        files={"file":("voice.wav",audio,"audio/wav")})
        assert res.status_code==422,res.text


def test_auto_requires_preconfigured_candidate(client,monkeypatch):
    for engine in g.OPEN_MODELS:monkeypatch.delenv("VOICE_"+engine.upper()+"_URL",raising=False)
    for engine in ("ELEVENLABS_API_KEY","MINIMAX_API_KEY","FISH_API_KEY"):
        monkeypatch.delenv(engine,raising=False)
    assert client.post("/v1/audio/speech",headers=HEADERS,json={"input":"你好","model":"auto"}).status_code==503
    monkeypatch.setenv("ELEVENLABS_API_KEY","fake")
    assert client.post("/v1/audio/speech",headers=HEADERS,json={"input":"你好","model":"auto"}).status_code==503


def test_open_model_receives_same_reference_and_text(client,monkeypatch):
    monkeypatch.setenv("VOICE_QWEN3TTS_URL","https://test-backend.invalid")
    incoming=[]
    async def mock_call(method,url,**kwargs):
        incoming.append((method,url,kwargs))
        return httpx.Response(200,content=wav(seconds=6))
    monkeypatch.setattr(g,"do_http",mock_call)
    voice=client.post("/v1/voices/reference",headers=HEADERS,
                      files={"file":("voice.wav",wav(),"audio/wav")},
                      data={"transcript":"参考声音"}).json()["voice"]
    response=client.post("/v1/audio/speech",headers=HEADERS,
                         json={"input":"这是一句话。"*18,"model":"auto","voice":voice,"response_format":"wav"})
    assert response.status_code==200,response.text
    assert response.headers["content-type"].startswith("audio/wav")
    assert response.headers["x-voicestudio-engine"]=="qwen3tts"
    assert g.check_wav(response.content)==6
    body=incoming[0][2]["json"]
    assert body["reference_text"]=="参考声音"
    assert body["reference_audio_base64"] and body["input"].endswith("。")
    assert incoming[0][1]=="https://test-backend.invalid/v1/audio/speech"


def test_bad_provider_does_not_silently_fallback(client,monkeypatch):
    monkeypatch.setenv("VOICE_COSYVOICE3_URL","https://voice-backend.invalid")
    async def fail(*args,**kwargs):
        raise g.HTTPException(504,"Mock server timeout: possibly billed")
    monkeypatch.setattr(g,"do_http",fail)
    result=client.post("/v1/audio/speech",headers=HEADERS,
                       json={"input":"不可重发的付费任务","model":"cosyvoice3"})
    assert result.status_code==504


def test_rejects_fake_audio(client,monkeypatch):
    monkeypatch.setenv("VOICE_INDEXTTS25_URL","https://test.invalid")
    async def mock_call(method,url,**kwargs):
        return httpx.Response(200,content=b"This is not audio!")
    monkeypatch.setattr(g,"do_http",mock_call)
    result=client.post("/v1/audio/speech",headers=HEADERS,
                       json={"input":"你好","model":"indextts25"})
    assert result.status_code==502,result.text
