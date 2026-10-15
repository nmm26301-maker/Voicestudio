"""Host protocol tests. No GPU, model downloads, accounts or paid deployments."""
import base64
from pathlib import Path
import sys
import types
import pytest
from fastapi.testclient import TestClient
from fastapi.responses import Response
from server import cloud_gateway, open_model_worker, prepare_cloud_model, gptsovits_bridge
from server.cloud_recipes import RECIPES, install_commands, environment
from server.single_engine_app import create_app, ENGINES
from server.test_free_gpu_deploy import pcm16_wav
from tools.report_modal_endpoint import write_report


@pytest.mark.parametrize("engine", ENGINES)
def test_selected_engine_protocol_and_full_reference(monkeypatch, tmp_path, engine):
    key = "isolated-model-test-key-1234567890"
    monkeypatch.setenv("VOICE_GATEWAY_TOKEN", key)
    monkeypatch.setattr(cloud_gateway, "STORE", tmp_path)
    calls = []
    monkeypatch.setattr(prepare_cloud_model, "prepare_worker", lambda e: calls.append(e))
    monkeypatch.setattr(prepare_cloud_model, "ensure_gpt_native", lambda: calls.append("gptsovits"))
    audio = pcm16_wav()
    full = "这是完整的新文字，不是参考录音原文。" * 8 + "最后一句。"

    def check(task, authorization):
        assert task.input == full and task.model == engine
        assert task.reference_text == "这是录音原文"
        assert base64.b64decode(task.reference_audio_base64) == audio
        assert authorization == "Bearer " + key
        return Response(audio, media_type="audio/wav")

    async def check_gpt(task, authorization):
        return check(task, authorization)

    monkeypatch.setattr(open_model_worker, "speech", check)
    monkeypatch.setattr(gptsovits_bridge, "speech", check_gpt)
    persisted = []
    client = TestClient(create_app(engine, lambda: persisted.append("ref"), lambda: persisted.append("cache")))
    headers = {"Authorization": "Bearer " + key}
    assert client.get("/v1/health").status_code == 401
    assert client.get("/v1/health", headers=headers).json()["configured"] == [engine]
    assert calls == []  # Health never downloads or starts a model/native process.
    upload = client.post("/v1/voices/reference", headers=headers,
                         files={"file": ("ref.wav", audio, "audio/wav")},
                         data={"transcript": "这是录音原文"})
    assert upload.status_code == 200
    voice = upload.json()["voice"]
    wrong = "qwen3tts" if engine != "qwen3tts" else "gptsovits"
    assert client.post("/v1/audio/speech", headers=headers, json={"input": full, "voice": voice, "model": wrong}).status_code == 422
    assert calls == []
    result = client.post("/v1/audio/speech", headers=headers, json={"input": full, "voice": voice, "model": engine})
    assert result.status_code == 200, result.text
    assert result.content == audio
    assert calls == [engine] and persisted == ["ref", "cache"]


def test_gpt_missing_transcript_never_starts_native(monkeypatch, tmp_path):
    monkeypatch.setenv("VOICE_GATEWAY_TOKEN", "unit-test-independent-token-123456")
    monkeypatch.setattr(cloud_gateway, "STORE", tmp_path)
    monkeypatch.setattr(prepare_cloud_model, "ensure_gpt_native", lambda: pytest.fail("Native must not start"))
    client = TestClient(create_app("gptsovits"))
    headers = {"Authorization": "Bearer unit-test-independent-token-123456"}
    upload = client.post("/v1/voices/reference", headers=headers, files={"file": ("ref.wav", pcm16_wav(), "audio/wav")})
    result = client.post("/v1/audio/speech", headers=headers,
                         json={"input": "新文字", "voice": upload.json()["voice"], "model": "gptsovits"})
    assert result.status_code == 422


def test_busy_engine_does_not_submit_twice_and_recovers(monkeypatch, tmp_path):
    import concurrent.futures
    import threading
    key = "separate-concurrent-test-key-12345"
    monkeypatch.setenv("VOICE_GATEWAY_TOKEN", key)
    monkeypatch.setattr(cloud_gateway, "STORE", tmp_path)
    monkeypatch.setattr(prepare_cloud_model, "prepare_worker", lambda e: None)
    started, release = threading.Event(), threading.Event()
    submitted = []

    def slow(task, authorization):
        submitted.append(task.input)
        started.set()
        assert release.wait(5)
        return Response(pcm16_wav(), media_type="audio/wav")

    monkeypatch.setattr(open_model_worker, "speech", slow)
    client = TestClient(create_app("qwen3tts"))
    headers = {"Authorization": "Bearer " + key}
    upload = client.post("/v1/voices/reference", headers=headers, files={"file": ("ref.wav", pcm16_wav(), "audio/wav")})
    task = {"model": "qwen3tts", "voice": upload.json()["voice"], "input": "新文字"}
    with concurrent.futures.ThreadPoolExecutor(1) as pool:
        pending = pool.submit(client.post, "/v1/audio/speech", headers=headers, json=task)
        try:
            assert started.wait(5)
            assert client.post("/v1/audio/speech", headers=headers, json=task).status_code == 429
            assert len(submitted) == 1
        finally:
            release.set()
        assert pending.result().status_code == 200

    def fail(task, authorization):
        raise RuntimeError("private-cloud-path")

    monkeypatch.setattr(open_model_worker, "speech", fail)
    result = client.post("/v1/audio/speech", headers=headers, json=task)
    assert result.status_code == 503 and "private-cloud-path" not in result.text
    monkeypatch.setattr(open_model_worker, "speech", slow)
    assert client.post("/v1/audio/speech", headers=headers, json=task).status_code == 200


@pytest.mark.parametrize("text", [" ", "长" * 181])
def test_invalid_text_is_rejected_without_preparing_gpu(monkeypatch, text):
    monkeypatch.setenv("VOICE_GATEWAY_TOKEN", "independent-prevalidation-test-key-123")
    monkeypatch.setattr(prepare_cloud_model, "prepare_worker", lambda e: pytest.fail("Do not download"))
    client = TestClient(create_app("indextts25"))
    result = client.post("/v1/audio/speech", headers={"Authorization": "Bearer independent-prevalidation-test-key-123"},
                         json={"model": "indextts25", "voice": "ref-invalid", "input": text})
    assert result.status_code == 422


def test_index_precision_falls_back_and_chatter_uses_v3(monkeypatch):
    calls = []
    module = types.ModuleType("indextts.infer_v2_5")
    module.IndexTTS2 = lambda **kw: calls.append(kw)
    monkeypatch.setitem(sys.modules, "indextts.infer_v2_5", module)
    torch = types.SimpleNamespace(cuda=types.SimpleNamespace(is_available=lambda: True, is_bf16_supported=lambda: False))
    monkeypatch.setitem(sys.modules, "torch", torch)
    monkeypatch.setenv("VOICE_MODEL_DIR", "/test/weights")
    monkeypatch.setenv("VOICE_DEVICE", "cuda:0")
    open_model_worker.load_model("indextts25")
    assert calls[-1]["use_bf16"] is False and calls[-1]["device"] == "cuda:0"
    torch.cuda.is_bf16_supported = lambda: True
    open_model_worker.load_model("indextts25")
    assert calls[-1]["use_bf16"] is True
    module = types.ModuleType("chatterbox.mtl_tts")
    module.ChatterboxMultilingualTTS = types.SimpleNamespace(from_pretrained=lambda **kw: calls.append(kw))
    monkeypatch.setitem(sys.modules, "chatterbox.mtl_tts", module)
    open_model_worker.load_model("chatterboxv3")
    assert calls[-1]["t3_model"] == "v3"


def test_recipes_have_distinct_pinned_sources_and_engine_receipts(tmp_path):
    for engine, recipe in RECIPES.items():
        assert environment(engine)["VOICE_DEPLOY_ENGINE"] == engine
        commands = install_commands(engine)
        if engine != "qwen3tts":
            assert len(recipe["revision"]) == 40
            assert recipe["revision"] in commands[0] and recipe["repo"] in commands[0]
        result = write_report("https://example--voice.modal.run", tmp_path / (engine + ".json"), engine=engine)
        assert result["engine"] == engine
    local = " ".join(install_commands("indextts25", tmp_path / "with spaces", system=False))
    assert '--python "$VIRTUAL_ENV/bin/python"' in local and "--system" not in local
    with pytest.raises(ValueError):
        create_app("fish")
    with pytest.raises(ValueError):
        write_report("https://example.modal.run", tmp_path / "bad.json", engine="fish")
