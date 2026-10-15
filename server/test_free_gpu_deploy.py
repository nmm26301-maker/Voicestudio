"""No-GPU regression tests for VoiceStudio's four cloud deployment templates.

These tests check request/auth/audio flow and notebook syntax only.
They do NOT claim the remote Qwen weights or GPUs have been tested.
"""
from __future__ import annotations

import ast
import io
import json
from pathlib import Path
import struct
import wave

from fastapi.testclient import TestClient
from fastapi.responses import Response

from server import cloud_gateway, lightning_qwen_app, open_model_worker


ROOT = Path(__file__).resolve().parents[1]


def pcm16_wav(seconds=4, sample_rate=16000):
    wav = io.BytesIO()
    with wave.open(wav, "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(sample_rate)
        f.writeframes(struct.pack("<h", 768) * int(seconds * sample_rate))
    return wav.getvalue()


def test_lightning_health_auth_and_invalid_requests(monkeypatch):
    key = "voice-studio-integration-key-123456789"
    monkeypatch.setenv("VOICE_GATEWAY_TOKEN", key)
    client = TestClient(lightning_qwen_app.app)
    assert client.get("/v1/health").status_code == 401
    response = client.get("/v1/health", headers={"Authorization": "Bearer " + key})
    assert response.status_code == 200
    assert response.json()["configured"] == ["qwen3tts"]

    invalid = client.post(
        "/v1/audio/speech",
        json={"input": "hi", "model": "chatterboxv3", "voice": "auto"},
        headers={"Authorization": "Bearer " + key},
    )
    assert invalid.status_code == 422
    missing = client.post(
        "/v1/audio/speech",
        json={"input": "hi", "model": "qwen3tts", "voice": "auto"},
        headers={"Authorization": "Bearer " + key},
    )
    assert missing.status_code == 422


def test_lightning_upload_and_mocked_synthesis(monkeypatch, tmp_path):
    key = "voice-studio-integration-key-123456789"
    monkeypatch.setenv("VOICE_GATEWAY_TOKEN", key)
    monkeypatch.setattr(cloud_gateway, "STORE", tmp_path)
    hdr = {"Authorization": "Bearer " + key}
    client = TestClient(lightning_qwen_app.app)
    audio = pcm16_wav()
    upload = client.post(
        "/v1/voices/reference",
        headers=hdr,
        files={"file": ("reference.wav", audio, "audio/wav")},
        data={"transcript": "这是参考录音"},
    )
    assert upload.status_code == 200, upload.text
    voice = upload.json()["voice"]
    assert voice.startswith("ref-")

    def fake_gpu(request, authorization):
        assert request.model == "qwen3tts"
        assert request.reference_text == "这是参考录音"
        assert request.reference_audio_base64
        assert authorization == "Bearer " + key
        return Response(audio, media_type="audio/wav")

    monkeypatch.setattr(open_model_worker, "speech", fake_gpu)
    result = client.post(
        "/v1/audio/speech",
        json={"input": "你好", "model": "qwen3tts", "voice": voice},
        headers=hdr,
    )
    assert result.status_code == 200, result.text
    assert result.content == audio


def test_free_gpu_templates_compile_without_gpu():
    source = (ROOT / "deploy/hf_zerogpu/app.py").read_text(encoding="utf-8")
    compile(source, "deploy/hf_zerogpu/app.py", "exec")
    notebook = json.loads(
        (ROOT / "deploy/kaggle/VoiceStudio-Qwen3-TTS.ipynb").read_text(encoding="utf-8")
    )
    assert notebook["nbformat"] == 4
    for cell in notebook["cells"]:
        if cell["cell_type"] == "code":
            ast.parse("".join(cell["source"]))
    compile(
        (ROOT / "server/modal_qwen_deploy.py").read_text(encoding="utf-8"),
        "server/modal_qwen_deploy.py",
        "exec",
    )
