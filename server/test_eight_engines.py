"""No-charge adapter checks: real wire shape but mocked provider audio."""
import base64
import io
import math
import struct
import wave

import httpx
import pytest
from fastapi.testclient import TestClient
from server import cloud_gateway as g
from server import gptsovits_bridge as bridge


def sample_wav(duration=4, rate=24000):
    stream=io.BytesIO()
    with wave.open(stream,"wb") as audio:
        audio.setnchannels(1);audio.setsampwidth(2);audio.setframerate(rate)
        count=rate*duration
        audio.writeframes(struct.pack("<"+"h"*count,*[
            int(2800*math.sin(2*math.pi*220*i/rate)) for i in range(count)]))
    return stream.getvalue()


@pytest.fixture
def gateway(monkeypatch,tmp_path):
    monkeypatch.setenv("VOICE_GATEWAY_TOKEN","unit-test-only")
    monkeypatch.setattr(g,"STORE",tmp_path)
    return TestClient(g.app)


def test_requested_eight_plus_fish_catalog(gateway,monkeypatch):
    assert set(g.PROVIDERS)=={"indextts25","cosyvoice3","qwen3tts","chatterboxv3",
                             "gptsovits","elevenlabs","minimax","cartesia","fish"}
    monkeypatch.setenv("VOICE_GPTSOVITS_URL","https://gpt-bridge.test")
    monkeypatch.setenv("CARTESIA_API_KEY","fake")
    r=gateway.get("/v1/health",headers={"Authorization":"Bearer unit-test-only"})
    assert r.status_code==200
    assert "gptsovits" in r.json()["voice_reference"]
    assert "cartesia" in r.json()["configured"]
    assert "cartesia" not in r.json()["voice_reference"]


def test_cartesia_official_wire_shape_full_text(gateway,monkeypatch):
    monkeypatch.setenv("CARTESIA_API_KEY","fake-key")
    received=[]
    async def mock(method,url,**kw):
        received.append((method,url,kw))
        return httpx.Response(200,content=sample_wav())
    monkeypatch.setattr(g,"do_http",mock)
    full="这是一段完整的新文字，最后一句必须朗读。"
    response=gateway.post("/v1/audio/speech",headers={"Authorization":"Bearer unit-test-only"},
        json={"input":full,"voice":"123e4567-e89b-12d3-a456-426614174000",
              "model":"cartesia","language":"zh","speed":1})
    assert response.status_code==200,response.text
    assert g.check_wav(response.content)>=3
    method,url,kw=received[0]
    assert method=="POST" and url=="https://api.cartesia.ai/tts/bytes"
    assert kw["headers"]["Authorization"]=="Bearer fake-key"
    assert kw["headers"]["Cartesia-Version"]=="2026-08-14"
    assert kw["json"]["model_id"]=="sonic-3"
    assert kw["json"]["transcript"]==full
    assert kw["json"]["output_format"]["container"]=="wav"


def test_cartesia_no_direct_reference_without_platform_voice(gateway,monkeypatch):
    monkeypatch.setenv("CARTESIA_API_KEY","fake-key")
    reference=gateway.post("/v1/voices/reference",
        headers={"Authorization":"Bearer unit-test-only"},
        files={"file":("reference.wav",sample_wav(),"audio/wav")}).json()["voice"]
    async def forbidden(*args,**kwargs):
        pytest.fail("Cartesia direct voice cloning wasn't consented to")
    monkeypatch.setattr(g,"do_http",forbidden)
    response=gateway.post("/v1/audio/speech",
        headers={"Authorization":"Bearer unit-test-only"},
        json={"input":"不应该调用平台","voice":reference,"model":"cartesia"})
    assert response.status_code==422


def test_gptsovits_gateway_receives_reference_unchanged(gateway,monkeypatch):
    monkeypatch.setenv("VOICE_GPTSOVITS_URL","https://secure.bridge.test")
    called=[]
    async def mock(method,url,**kw):
        called.append((method,url,kw["json"]))
        return httpx.Response(200,content=sample_wav())
    monkeypatch.setattr(g,"do_http",mock)
    ref=gateway.post("/v1/voices/reference",
       headers={"Authorization":"Bearer unit-test-only"},
       files={"file":("reference.wav",sample_wav(),"audio/wav")},
       data={"transcript":"这是参考音频原文"}).json()["voice"]
    result=gateway.post("/v1/audio/speech",
       headers={"Authorization":"Bearer unit-test-only"},
       json={"input":"需要说出来的新句子","voice":ref,"model":"gptsovits"})
    assert result.status_code==200,result.text
    body=called[0][2]
    assert called[0][1]=="https://secure.bridge.test/v1/audio/speech"
    assert base64.b64decode(body["reference_audio_base64"])==sample_wav()
    assert body["reference_text"]=="这是参考音频原文"
    assert body["input"]=="需要说出来的新句子"


def test_gpt_bridge_native_tts_and_temporary_reference(monkeypatch,tmp_path):
    monkeypatch.setenv("VOICE_GPTSOVITS_TOKEN","bridge-key")
    monkeypatch.setenv("GPT_SOVITS_SHARED_DIR",str(tmp_path))
    monkeypatch.setenv("GPT_SOVITS_NATIVE_URL","http://127.0.0.1:9880")
    captured=[]
    async def native_call(self,url,**kwargs):
        captured.append((url,kwargs["json"]))
        assert kwargs["json"]["ref_audio_path"].startswith(str(tmp_path))
        assert __import__("pathlib").Path(kwargs["json"]["ref_audio_path"]).read_bytes()==sample_wav()
        return httpx.Response(200,content=sample_wav())
    monkeypatch.setattr(httpx.AsyncClient,"post",native_call)
    c=TestClient(bridge.app)
    request={"input":"全新的句子","model":"gptsovits",
             "reference_audio_base64":base64.b64encode(sample_wav()).decode(),
             "reference_text":"参考录音的原文","language":"zh"}
    r=c.post("/v1/audio/speech",json=request,headers={"Authorization":"Bearer bridge-key"})
    assert r.status_code==200,r.text
    assert captured[0][0]=="http://127.0.0.1:9880/tts"
    assert captured[0][1]["text"]=="全新的句子"
    assert captured[0][1]["prompt_text"]=="参考录音的原文"
    assert not list(tmp_path.glob("vs-gpt-*"))


def test_gpt_bridge_rejects_arbitrary_upstream(monkeypatch):
    monkeypatch.setenv("VOICE_GPTSOVITS_TOKEN","bridge-key")
    monkeypatch.setenv("GPT_SOVITS_NATIVE_URL","https://attacker.example")
    r=TestClient(bridge.app).post("/v1/audio/speech",
         headers={"Authorization":"Bearer bridge-key"},
         json={"input":"不能访问远程主机","model":"gptsovits"})
    assert r.status_code==503

def test_gptsovits_cannot_be_automatically_selected_without_reference(gateway,monkeypatch):
    for engine in g.OPEN_MODELS:
        monkeypatch.delenv("VOICE_"+engine.upper()+"_URL",raising=False)
    for key in ("FISH_API_KEY","MINIMAX_API_KEY","ELEVENLABS_API_KEY","CARTESIA_API_KEY"):
        monkeypatch.delenv(key,raising=False)
    monkeypatch.setenv("VOICE_GPTSOVITS_URL","https://gpt-bridge.test")
    async def forbidden(*args,**kw):
        pytest.fail("GPT-SoVITS must not be called without a reference and transcript")
    monkeypatch.setattr(g,"do_http",forbidden)
    h={"Authorization":"Bearer unit-test-only"}
    assert gateway.post("/v1/audio/speech",headers=h,
           json={"input":"无参考声音","model":"auto"}).status_code==503
    assert gateway.post("/v1/audio/speech",headers=h,
           json={"input":"无参考声音","model":"gptsovits"}).status_code==422
    ref=gateway.post("/v1/voices/reference",headers=h,
       files={"file":("reference.wav",sample_wav(),"audio/wav")}).json()["voice"]
    assert gateway.post("/v1/audio/speech",headers=h,
           json={"input":"缺少准确原文","model":"auto","voice":ref}).status_code==503
