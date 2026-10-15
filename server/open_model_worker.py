"""One GPU process per open model; install each model in its own official environment.

No weights ship in the APK. Set VOICE_WORKER_ENGINE / VOICE_WORKER_TOKEN, then
uvicorn server.open_model_worker:app --host 127.0.0.1 --port 8770 --workers 1.
Model imports are lazy, so protocol checks do not download weights or need CUDA.
"""
from __future__ import annotations
import base64
import binascii
import os
from pathlib import Path
import tempfile
import threading

from fastapi import FastAPI, Header, HTTPException
from fastapi.responses import Response
from pydantic import Field
from server.cloud_gateway import SpeechRequest, check_wav, normalized_wav

# GPT-SoVITS uses server.gptsovits_bridge (native /tts), never Chatterbox.
WORKER_MODELS = ("qwen3tts", "cosyvoice3", "indextts25", "chatterboxv3")

app = FastAPI(title="VoiceStudio Open Model Worker", version="1.5.21")
lock = threading.Lock()
loaded = None
loaded_engine = None


class WorkerRequest(SpeechRequest):
    reference_audio_base64: str | None = Field(default=None, max_length=7 * 1024 * 1024)
    reference_text: str = Field(default="", max_length=2000)


def auth(authorization):
    # Same constant-time auth logic, with a separate upstream key.
    import hmac
    key = os.getenv("VOICE_WORKER_TOKEN", "")
    if not key or not authorization or not hmac.compare_digest(authorization, "Bearer " + key):
        raise HTTPException(401, "Invalid worker token")


def engine_name():
    name = os.getenv("VOICE_WORKER_ENGINE", "qwen3tts")
    if name not in WORKER_MODELS:
        raise HTTPException(503, "Unsupported worker engine")
    return name


def load_model(engine):
    device = os.getenv("VOICE_DEVICE", "cuda:0")
    directory = os.getenv("VOICE_MODEL_DIR", "")
    if engine == "qwen3tts":
        import torch
        from qwen_tts import Qwen3TTSModel
        model_name = directory or os.getenv("VOICE_QWEN_MODEL_ID", "Qwen/Qwen3-TTS-12Hz-1.7B-Base")
        # T4/P100-class free GPUs often cannot perform native BF16 operations.
        # Prefer BF16 on modern GPUs, FP16 elsewhere; still infer remotely.
        dtype = torch.bfloat16 if torch.cuda.is_available() and torch.cuda.is_bf16_supported() else torch.float16
        return Qwen3TTSModel.from_pretrained(model_name, device_map=device, dtype=dtype)
    if engine == "cosyvoice3":
        from cosyvoice.cli.cosyvoice import AutoModel
        if not directory:
            raise RuntimeError("VOICE_MODEL_DIR must contain Fun-CosyVoice3-0.5B")
        return AutoModel(model_dir=directory)
    if engine == "indextts25":
        from indextts.infer_v2_5 import IndexTTS2
        if not directory:
            raise RuntimeError("VOICE_MODEL_DIR must contain IndexTTS-2.5")
        import torch
        bf16 = device.startswith("cuda") and torch.cuda.is_available() and torch.cuda.is_bf16_supported()
        return IndexTTS2(cfg_path=str(Path(directory) / "config.yaml"), model_dir=directory,
                         device=device, use_bf16=bf16)
    from chatterbox.mtl_tts import ChatterboxMultilingualTTS
    return ChatterboxMultilingualTTS.from_pretrained(device=device, t3_model="v3")


def language(value, text):
    value = (value or "auto").lower().replace("_", "-").split("-")[0]
    if value in ("auto", "none"):
        return "zh" if any("\u3400" <= c <= "\u9fff" for c in text) else "en"
    return value


def bounded_text_parts(text: str, limit: int = 60) -> list[str]:
    # Chatterbox's upstream generation has a fixed token budget per call.
    # Bound inner chunks rather than feeding 180 characters into that budget.
    parts = []
    at = 0
    while at < len(text):
        end = min(len(text), at + limit)
        if end < len(text):
            for i in range(end - 1, at + limit // 2 - 1, -1):
                if text[i] in "。！？；.!?;\n":
                    end = i + 1
                    break
        parts.append(text[at:end])
        at = end
    return parts


def infer(model, engine, task, reference, output):
    """Full new text is independent from reference transcript. Keep every output chunk."""
    import torch
    import torchaudio
    lang = language(task.language, task.input)
    if engine == "indextts25":
        model.infer(spk_audio_prompt=str(reference), text=task.input, lang=lang.upper(),
                    output_path=str(output), verbose=False)
        return
    if engine == "qwen3tts":
        names = {"zh": "Chinese", "en": "English", "ja": "Japanese", "ko": "Korean",
                 "de": "German", "fr": "French", "ru": "Russian", "pt": "Portuguese",
                 "es": "Spanish", "it": "Italian"}
        if lang not in names:
            raise HTTPException(422, "Qwen3-TTS does not support the selected language")
        waves, rate = model.generate_voice_clone(text=task.input, language=names[lang],
                          ref_audio=str(reference), ref_text=task.reference_text or None,
                          x_vector_only_mode=not bool(task.reference_text))
        if len(waves) != 1:
            raise RuntimeError("Unexpected Qwen output count")
        audio = torch.as_tensor(waves[0]).float().reshape(1, -1)
    elif engine == "cosyvoice3":
        prefix = "You are a helpful assistant.<|endofprompt|>"
        if task.reference_text:
            chunks = model.inference_zero_shot(task.input, prefix + task.reference_text, str(reference), stream=False)
        else:
            chunks = model.inference_cross_lingual(prefix + task.input, str(reference), stream=False)
        audio_parts = [chunk["tts_speech"].detach().cpu() for chunk in chunks]
        if not audio_parts:
            raise RuntimeError("Empty CosyVoice output")
        audio = torch.cat(audio_parts, dim=-1)
        rate = model.sample_rate
    else:
        parts = [part for part in bounded_text_parts(task.input) if part.strip()]
        audio_parts = [model.generate(part, language_id=lang,
                           audio_prompt_path=str(reference) if i == 0 else None).detach().cpu()
                       for i, part in enumerate(parts)]
        if not audio_parts:
            raise HTTPException(422, "Text contains no speech")
        audio = torch.cat(audio_parts, dim=-1)
        rate = model.sr
    if audio.ndim == 1:
        audio = audio.unsqueeze(0)
    torchaudio.save(str(output), audio.detach().cpu(), rate, encoding="PCM_S", bits_per_sample=16)


def reference_bytes(task):
    if task.reference_audio_base64:
        try:
            data = base64.b64decode(task.reference_audio_base64, validate=True)
        except (ValueError, binascii.Error) as e:
            raise HTTPException(422, "Invalid reference base64") from e
    else:
        # Fixed server-owned reference only, never an arbitrary client path.
        default = os.getenv("VOICE_DEFAULT_REFERENCE", "")
        if not default or task.voice not in ("auto", "default"):
            raise HTTPException(422, "Record a reference voice or configure a default reference")
        path = Path(default)
        if not path.is_file() or path.stat().st_size > 5 * 1024 * 1024:
            raise HTTPException(503, "Default reference unavailable")
        data = path.read_bytes()
    check_wav(data, reference=True)
    return data


@app.get("/v1/health")
def health(authorization: str | None = Header(default=None)):
    auth(authorization)
    return {"status": "ok", "engine": engine_name(), "model_loaded": loaded is not None}


@app.post("/v1/audio/speech")
def speech(task: WorkerRequest, authorization: str | None = Header(default=None)):
    global loaded, loaded_engine
    auth(authorization)
    engine = engine_name()
    if task.model not in (engine, "auto") or not task.input.strip():
        raise HTTPException(422, "Invalid model or empty input")
    data = reference_bytes(task)
    # One model request at a time: conditioning in Chatterbox is mutable.
    if not lock.acquire(blocking=False):
        raise HTTPException(429, "Worker busy; no synthesis submitted")
    try:
        if loaded is None or loaded_engine != engine:
            loaded = load_model(engine)
            loaded_engine = engine
        with tempfile.TemporaryDirectory(prefix="voicestudio-") as tmp:
            reference, output = Path(tmp) / "reference.wav", Path(tmp) / "output.wav"
            reference.write_bytes(data)
            infer(loaded, engine, task, reference, output)
            if not output.is_file() or output.stat().st_size > 48 * 1024 * 1024:
                raise HTTPException(502, "Inference returned no usable output")
            wav = normalized_wav(output.read_bytes(), "wav")
            if task.speed != 1:
                import subprocess
                proc = subprocess.run(["ffmpeg", "-v", "error", "-nostdin", "-i", "pipe:0",
                    "-af", f"atempo={task.speed}", "-f", "wav", "pipe:1"],
                    input=wav, capture_output=True, timeout=90, check=False)
                if proc.returncode:
                    raise HTTPException(502, "Speed adjustment failed")
                wav = normalized_wav(proc.stdout, "wav")
            return Response(wav, media_type="audio/wav")
    except HTTPException:
        raise
    except Exception as e:
        # Keep raw exception/server paths and dependency diagnostics in server logs.
        import logging
        logging.exception("VoiceStudio worker inference failed")
        raise HTTPException(503, "Model load or synthesis failed; check worker logs") from e
    finally:
        lock.release()
