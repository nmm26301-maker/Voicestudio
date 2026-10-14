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
    async def ready(provider): return True
    monkeypatch.setattr(g, "worker_ready", ready)
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


def test_health_auth_and_configured_not_fake(client, monkeypatch):
    assert client.get('/v1/health').status_code == 401
    for name in g.OPEN_MODELS:
        monkeypatch.delenv('VOICE_' + name.upper() + '_URL', raising=False)
    monkeypatch.setenv('VOICE_COSYVOICE3_URL', 'https://configured.invalid')
    result = client.get('/v1/health', headers=HEADERS)
    assert result.status_code == 200
    assert result.json()['voice_reference'] == ['cosyvoice3']
    assert result.json()['auto_fallback_after_submit'] is False


def test_truncated_declared_frames_rejected(client):
    full = wav()
    # Keeps the declared five-second RIFF/data header, discards actual tail.
    truncated = full[:-24000]
    response = client.post('/v1/voices/reference', headers=HEADERS,
                           files={'file': ('ref.wav', truncated, 'audio/wav')})
    assert response.status_code == 422


def test_expired_reference_not_sent_to_provider(client, monkeypatch):
    monkeypatch.setenv('VOICE_QWEN3TTS_URL', 'https://configured.invalid')
    identifier = client.post('/v1/voices/reference', headers=HEADERS,
                            files={'file': ('ref.wav', wav(), 'audio/wav')}).json()['voice']
    old = g.time.time() - g.TTL_SECONDS - 1
    os.utime(g.ref_path(identifier), (old, old))
    result = client.post('/v1/audio/speech', headers=HEADERS,
                         json={'input': '新的完整文本', 'voice': identifier, 'model': 'qwen3tts'})
    assert result.status_code == 410


def test_all_four_reference_models_receive_entire_new_text(client, monkeypatch):
    seen = []
    async def mock_call(method, url, **kwargs):
        seen.append(kwargs['json'])
        return httpx.Response(200, content=wav())
    monkeypatch.setattr(g, 'do_http', mock_call)
    ref = client.post('/v1/voices/reference', headers=HEADERS,
                      files={'file': ('ref.wav', wav(), 'audio/wav')}).json()['voice']
    for engine in g.OPEN_MODELS:
        monkeypatch.setenv('VOICE_' + engine.upper() + '_URL', 'https://configured.invalid')
        text = '这是新的文字，与参考录音无关。最后一句也要读完。'
        result = client.post('/v1/audio/speech', headers=HEADERS,
                             json={'input': text, 'voice': ref, 'model': engine})
        assert result.status_code == 200
        assert seen[-1]['input'] == text
        assert seen[-1]['model'] == engine


def test_auto_switches_before_post_when_first_worker_unreachable(client, monkeypatch):
    for p in g.OPEN_MODELS:
        monkeypatch.delenv('VOICE_' + p.upper() + '_URL', raising=False)
    monkeypatch.setenv('VOICE_QWEN3TTS_URL', 'https://down.invalid')
    monkeypatch.setenv('VOICE_COSYVOICE3_URL', 'https://ready.invalid')
    probes=[]; posts=[]
    async def ready(provider):
        probes.append(provider)
        return provider == 'cosyvoice3'
    async def send(method, url, **kwargs):
        posts.append((method,url))
        return httpx.Response(200, content=wav())
    monkeypatch.setattr(g, 'worker_ready', ready)
    monkeypatch.setattr(g, 'do_http', send)
    result=client.post('/v1/audio/speech', headers=HEADERS,
                       json={'input':'故障切换后的完整句子。', 'model':'auto'})
    assert result.status_code == 200
    assert result.headers['x-voicestudio-engine'] == 'cosyvoice3'
    assert len(posts) == 1 and posts[0][1] == 'https://ready.invalid/v1/audio/speech'
    assert set(probes) == {'qwen3tts', 'cosyvoice3'}


@pytest.mark.parametrize('provider,variable', [('fish','FISH_API_KEY'),('minimax','MINIMAX_API_KEY'),('elevenlabs','ELEVENLABS_API_KEY')])
def test_commercial_voice_id_routes_and_full_text(client, monkeypatch, provider, variable):
    monkeypatch.setenv(variable, 'fake-only-provider-key')
    seen=[]
    async def call(method, url, **kwargs):
        seen.append((method, url, kwargs))
        if provider == 'minimax':
            return httpx.Response(200, json={'base_resp': {'status_code': 0}, 'data': {'audio': wav().hex()}})
        return httpx.Response(200, content=wav())
    monkeypatch.setattr(g, 'do_http', call)
    text='商业平台也必须收到最后一句。'
    result=client.post('/v1/audio/speech', headers=HEADERS,
                       json={'input': text, 'voice': 'VoiceID123', 'model': provider, 'speed': 1.2})
    assert result.status_code == 200, result.text
    assert len(seen) == 1 and seen[0][0] == 'POST'
    payload=seen[0][2]['json']
    assert payload['text'] == text
    if provider == 'fish': assert payload['prosody']['speed'] == 1.2
    if provider == 'minimax': assert payload['voice_setting']['voice_id'] == 'VoiceID123'
    if provider == 'elevenlabs': assert seen[0][1].endswith('/VoiceID123')


def test_recording_not_silently_created_as_paid_commercial_voice(client, monkeypatch):
    voice=client.post('/v1/voices/reference', headers=HEADERS,
                      files={'file':('ref.wav',wav(),'audio/wav')}).json()['voice']
    async def forbidden(*args, **kwargs):
        pytest.fail('Reference-only voice made commercial call')
    monkeypatch.setattr(g, 'do_http', forbidden)
    for provider, key in [('minimax','MINIMAX_API_KEY'),('elevenlabs','ELEVENLABS_API_KEY')]:
        monkeypatch.setenv(key,'fake-key')
        response=client.post('/v1/audio/speech',headers=HEADERS,
                             json={'input':'新的文字','model':provider,'voice':voice})
        assert response.status_code == 422


def test_fish_inline_reference_msgpack_uses_new_text(client, monkeypatch):
    import msgpack
    monkeypatch.setenv('FISH_API_KEY','fake-fish-key')
    seen=[]
    async def call(method,url,**kwargs):
        seen.append(kwargs)
        return httpx.Response(200,content=wav())
    monkeypatch.setattr(g,'do_http',call)
    ref=client.post('/v1/voices/reference',headers=HEADERS,
        files={'file':('ref.wav',wav(),'audio/wav')},data={'transcript':'原录音内容'}).json()['voice']
    result=client.post('/v1/audio/speech',headers=HEADERS,
                       json={'input':'任意新的文字，最后一句。','model':'fish','voice':ref})
    assert result.status_code==200, result.text
    assert len(seen)==1
    body=msgpack.unpackb(seen[0]['content'],raw=False)
    assert body['text']=='任意新的文字，最后一句。'
    assert body['references'][0]['text']=='原录音内容'
    assert body['references'][0]['audio']==wav()
    assert 'reference_id' not in body
    assert seen[0]['headers']['Content-Type']=='application/msgpack'
