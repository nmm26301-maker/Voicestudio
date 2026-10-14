"""Worker protocol/adapter tests without weights, CUDA, or provider billing."""
import base64
from pathlib import Path
import sys
import types
from fastapi.testclient import TestClient
import pytest
from server import open_model_worker as w
from server.test_cloud_gateway import wav

@pytest.fixture
def client(monkeypatch):
    monkeypatch.setenv('VOICE_WORKER_TOKEN', 'worker-only-fixture')
    monkeypatch.setenv('VOICE_WORKER_ENGINE', 'qwen3tts')
    monkeypatch.setattr(w, 'loaded', None)
    monkeypatch.setattr(w, 'loaded_engine', None)
    return TestClient(w.app)

HEADERS={'Authorization': 'Bearer worker-only-fixture'}

def test_auth_validation_before_load(client, monkeypatch):
    def forbidden(*args):
        pytest.fail('Bad request loaded weights')
    monkeypatch.setattr(w, 'load_model', forbidden)
    assert client.get('/v1/health').status_code == 401
    assert client.get('/v1/health', headers=HEADERS).json()['model_loaded'] is False
    assert client.post('/v1/audio/speech', headers=HEADERS,
                       json={'input': 'new text', 'reference_audio_base64': '!invalid'}).status_code == 422


def test_full_input_model_loaded_once_reference_removed(client, monkeypatch):
    loads=[]; paths=[]; seen=[]
    monkeypatch.setattr(w, 'load_model', lambda engine: loads.append(engine) or object())
    def inference(model, engine, task, reference, output):
        assert reference.read_bytes() == wav()
        paths.append(reference)
        seen.append(task.input)
        output.write_bytes(wav())
    monkeypatch.setattr(w, 'infer', inference)
    for text in ('新的文本最后一句。', 'another completely new sentence'):
        result=client.post('/v1/audio/speech', headers=HEADERS,
            json={'input': text, 'model': 'qwen3tts', 'reference_text': 'only the reference',
                  'reference_audio_base64': base64.b64encode(wav()).decode()})
        assert result.status_code == 200, result.text
        assert seen[-1] == text
        assert not paths[-1].exists()
    assert loads == ['qwen3tts']


def test_busy_worker_does_not_submit(client):
    w.lock.acquire()
    try:
        result=client.post('/v1/audio/speech', headers=HEADERS,
            json={'input': 'new text', 'reference_audio_base64': base64.b64encode(wav()).decode()})
        assert result.status_code == 429
    finally:
        w.lock.release()


def test_cosyvoice_keeps_all_chunks(monkeypatch, tmp_path):
    class Tensor:
        ndim=2
        def __init__(self, value): self.value=value
        def detach(self): return self
        def cpu(self): return self
    cat=[]; saved=[]
    torch=types.SimpleNamespace(cat=lambda parts, dim: cat.append([p.value for p in parts]) or Tensor('all'))
    audio=types.SimpleNamespace(save=lambda *args, **kwargs: saved.append(args))
    monkeypatch.setitem(sys.modules,'torch',torch)
    monkeypatch.setitem(sys.modules,'torchaudio',audio)
    class Model:
        sample_rate=24000
        def inference_zero_shot(self, text, reftext, reference, stream):
            assert text == '末句也要读出来。'
            assert reftext.endswith('参考文字') and stream is False
            return iter([{'tts_speech':Tensor(1)}, {'tts_speech':Tensor(2)}, {'tts_speech':Tensor(3)}])
    task=w.WorkerRequest(input='末句也要读出来。', reference_text='参考文字')
    w.infer(Model(), 'cosyvoice3', task, tmp_path/'ref.wav', tmp_path/'out.wav')
    assert cat == [[1,2,3]]
    assert len(saved) == 1


def test_chatterbox_bounded_text_is_lossless():
    text = '新文字。' * 40 + '不能丢掉末句🙂。'
    parts = w.bounded_text_parts(text)
    assert ''.join(parts) == text
    assert all(len(part) <= 60 for part in parts)
    assert parts[-1].endswith('🙂。')
