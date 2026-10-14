"""VoiceStudio cloud-only gateway. Run behind HTTPS with pinned provider secrets.

The four OSS backends must provide the documented normalized inference contract;
this gateway does NOT download model weights or grant free GPU access.
No automatic retry/fallback after a synthesis request is sent (possible billing).
"""
from __future__ import annotations

import base64
import hmac
import io
import json
import os
import pathlib
import subprocess
import tempfile
import time
import uuid
import wave
from typing import Literal

import httpx
from fastapi import FastAPI, File, Form, Header, HTTPException, UploadFile
from fastapi.responses import Response
from pydantic import BaseModel, Field

app = FastAPI(title="VoiceStudio Cloud Gateway", version="0.1")
OPEN_MODELS = ("indextts25", "cosyvoice3", "qwen3tts", "chatterboxv3")
REFERENCE_MODELS = OPEN_MODELS + ("fish",)
PROVIDERS = OPEN_MODELS + ("elevenlabs", "minimax", "fish")
PREFER = ("qwen3tts", "cosyvoice3", "indextts25", "chatterboxv3", "fish", "minimax", "elevenlabs")
STORE = pathlib.Path(os.getenv("VOICE_REFERENCE_DIR", "/tmp/voicestudio-references"))
TTL_SECONDS = 3 * 86400
MAX_AUDIO = 48 * 1024 * 1024
MAX_REFERENCE = 5 * 1024 * 1024


def authenticate(auth: str | None) -> None:
    token = os.getenv("VOICE_GATEWAY_TOKEN", "")
    if not token or not auth or not hmac.compare_digest(auth, "Bearer " + token):
        raise HTTPException(401, "Invalid gateway token")


def configured(provider: str) -> bool:
    if provider in OPEN_MODELS:
        return bool(os.getenv("VOICE_" + provider.upper() + "_URL"))
    if provider == "elevenlabs":
        return bool(os.getenv("ELEVENLABS_API_KEY"))
    if provider == "minimax":
        return bool(os.getenv("MINIMAX_API_KEY"))
    if provider == "fish":
        return bool(os.getenv("FISH_API_KEY"))
    return False


def ref_path(identifier: str) -> pathlib.Path:
    if not identifier.startswith("ref-"):
        raise HTTPException(422, "Invalid reference identifier")
    try:
        uuid.UUID(identifier[4:])
    except ValueError:
        raise HTTPException(422, "Invalid reference identifier")
    return STORE / (identifier + ".wav")


def read_reference(identifier: str) -> tuple[bytes, str]:
    location = ref_path(identifier)
    info = location.with_suffix(".json")
    if not location.is_file() or not info.is_file():
        raise HTTPException(410, "Reference expired; upload recording again")
    if time.time() - location.stat().st_mtime > TTL_SECONDS:
        raise HTTPException(410, "Reference expired; upload recording again")
    audio = location.read_bytes()
    transcript = json.loads(info.read_text(encoding="utf-8")).get("transcript", "")
    check_wav(audio, reference=True)
    return audio, transcript


def check_wav(audio: bytes, reference: bool = False) -> float:
    if not audio or len(audio) > (MAX_REFERENCE if reference else MAX_AUDIO):
        raise HTTPException(422 if reference else 502, "Audio empty or too large")
    try:
        with wave.open(io.BytesIO(audio), "rb") as f:
            rate = f.getframerate()
            frames = f.getnframes()
            if f.getnchannels() != 1 or f.getsampwidth() != 2 or rate < 8000 or rate > 48000:
                raise ValueError("Expected mono PCM16 WAV")
            samples = f.readframes(frames)
        if len(samples) != frames * 2:
            raise ValueError("Truncated PCM data")
        if not samples or not any(samples):
            raise ValueError("Silent audio")
        seconds = frames / rate
        if seconds < (3 if reference else .12) or (reference and seconds > 10.01):
            raise ValueError("Invalid audio duration")
        return seconds
    except (wave.Error, EOFError, ValueError, ZeroDivisionError) as e:
        raise HTTPException(422 if reference else 502, "Invalid, silent or truncated WAV") from e


def normalized_wav(data: bytes, kind: Literal["wav", "mp3"]) -> bytes:
    if not data or len(data) > MAX_AUDIO:
        raise HTTPException(502, "Provider returned empty or oversized audio")
    # FFmpeg normalizes provider-dependent MP3/WAV to mono 24 kHz PCM16 for Android.
    try:
        proc = subprocess.run(
            ["ffmpeg", "-v", "error", "-nostdin", "-i", "pipe:0",
             "-f", "wav", "-acodec", "pcm_s16le", "-ar", "24000", "-ac", "1", "pipe:1"],
            input=data, capture_output=True, timeout=90, check=False
        )
    except (FileNotFoundError, subprocess.TimeoutExpired) as e:
        raise HTTPException(503, "FFmpeg unavailable or conversion timed out") from e
    if proc.returncode != 0 or not proc.stdout:
        raise HTTPException(502, "Provider did not return decodable audio")
    # ffmpeg pipe output carries an unknown-size WAV header (0xffffffff).
    # Reheader the actual frames; otherwise Android can report hours of silence.
    try:
        with wave.open(io.BytesIO(proc.stdout), "rb") as reader:
            if reader.getnchannels()!=1 or reader.getsampwidth()!=2 or reader.getframerate()!=24000:
                raise ValueError("Unexpected decoded PCM")
            samples=reader.readframes(reader.getnframes())
        out=io.BytesIO()
        with wave.open(out,"wb") as writer:
            writer.setnchannels(1)
            writer.setsampwidth(2)
            writer.setframerate(24000)
            writer.writeframes(samples)
        result=out.getvalue()
    except (wave.Error,ValueError) as e:
        raise HTTPException(502,"Audio reheader failed") from e
    check_wav(result)
    return result


class SpeechRequest(BaseModel):
    input: str = Field(min_length=1, max_length=180)
    model: str = "auto"
    voice: str = "auto"
    language: str | None = None
    speed: float = Field(default=1, ge=.5, le=2)
    response_format: Literal["wav"] = "wav"
    num_step: int = 16
    seed: int = 42


def choose(model: str, has_reference: bool, voice: str = "auto") -> str:
    if model != "auto":
        if model not in PROVIDERS:
            raise HTTPException(422, "Unsupported provider")
        if not configured(model):
            raise HTTPException(503, f"{model} is not configured")
        if has_reference and model not in REFERENCE_MODELS:
            raise HTTPException(422, "Selected provider needs a registered voice ID; choose an OSS reference-enabled engine")
        return model
    for provider in PREFER:
        if configured(provider) and (not has_reference or provider in OPEN_MODELS) and (provider in OPEN_MODELS or voice not in ("", "auto")):
            return provider
    raise HTTPException(503, "No suitable cloud model configured")


async def do_http(method: str, url: str, **kwargs) -> httpx.Response:
    # Provider URLs are fixed in server configuration, never taken from user input.
    async with httpx.AsyncClient(timeout=httpx.Timeout(180, connect=15), follow_redirects=False) as client:
        try:
            async with client.stream(method, url, **kwargs) as incoming:
                if incoming.status_code != 200:
                    raise HTTPException(502, f"Provider rejected synthesis (HTTP {incoming.status_code})")
                data = bytearray()
                async for block in incoming.aiter_bytes():
                    if len(data) + len(block) > MAX_AUDIO:
                        raise HTTPException(502, "Provider response too large")
                    data.extend(block)
                resp = httpx.Response(incoming.status_code, headers=incoming.headers, content=bytes(data))
        except httpx.TimeoutException as e:
            # Request may already have been billed. Never silently resubmit.
            raise HTTPException(504, "Provider timed out; submission may have been billed") from e
        except httpx.HTTPError as e:
            raise HTTPException(502, "Provider connection failed; submission state unknown") from e
    if resp.status_code != 200:
        raise HTTPException(502, f"Provider rejected synthesis (HTTP {resp.status_code})")
    return resp


async def worker_ready(provider: str) -> bool:
    """Safe GET preflight only; never retry speech or probe with paid synthesis."""
    root = os.getenv("VOICE_" + provider.upper() + "_URL", "").rstrip("/")
    if not root.startswith("https://") and not root.startswith("http://127.0.0.1:"):
        return False
    key = os.getenv("VOICE_" + provider.upper() + "_TOKEN", "")
    try:
        async with httpx.AsyncClient(timeout=httpx.Timeout(4, connect=2), follow_redirects=False) as client:
            response = await client.get(root + "/v1/health", headers={"Authorization": "Bearer " + key})
            if response.status_code != 200 or len(response.content) > 32768:
                return False
            info = response.json()
            return info.get("status") == "ok" and info.get("engine") == provider
    except (httpx.HTTPError, ValueError, TypeError):
        return False


async def automatic_model(has_reference: bool, voice: str, reference_text: str = "") -> str:
    import asyncio
    candidates = [p for p in PREFER if configured(p) and
                  (not has_reference or p in OPEN_MODELS or p == "fish" and bool(reference_text.strip())) and
                  (p in OPEN_MODELS or voice not in ("", "auto"))]
    # Concurrent read-only probes bound overall selection latency to one timeout.
    open_candidates = [p for p in candidates if p in OPEN_MODELS]
    readiness = dict(zip(open_candidates, await asyncio.gather(*(worker_ready(p) for p in open_candidates))))
    for provider in candidates:
        if provider not in OPEN_MODELS or readiness.get(provider):
            return provider
    raise HTTPException(503, "No reachable cloud worker or configured suitable provider")


@app.get("/v1/health")
def health(authorization: str | None = Header(default=None)):
    authenticate(authorization)
    return {"status": "ok", "configured": [p for p in PROVIDERS if configured(p)],
            "voice_reference": [p for p in REFERENCE_MODELS if configured(p)],
            "auto_fallback_after_submit": False}


@app.post("/v1/voices/reference")
async def upload_reference(
    file: UploadFile = File(...),
    transcript: str = Form(""),
    authorization: str | None = Header(default=None),
):
    authenticate(authorization)
    if len(transcript) > 2000:
        raise HTTPException(422, "Transcript too long")
    data = await file.read(MAX_REFERENCE + 1)
    check_wav(data, reference=True)
    STORE.mkdir(parents=True, exist_ok=True)
    for stale in STORE.glob("ref-*.wav"):
        try:
            if time.time() - stale.stat().st_mtime > TTL_SECONDS:
                stale.unlink(missing_ok=True)
                stale.with_suffix(".json").unlink(missing_ok=True)
        except OSError:
            pass
    identifier = "ref-" + str(uuid.uuid4())
    target = ref_path(identifier)
    # Atomic publication; no model run or paid call during upload.
    with tempfile.NamedTemporaryFile(dir=STORE, delete=False) as temp:
        temp.write(data)
        staging = pathlib.Path(temp.name)
    try:
        os.chmod(staging, 0o600)
        staging.replace(target)
        target.with_suffix(".json").write_text(json.dumps({"transcript": transcript}, ensure_ascii=False), encoding="utf-8")
    finally:
        staging.unlink(missing_ok=True)
    return {"voice": identifier, "expires_in_seconds": TTL_SECONDS}


@app.post("/v1/audio/speech")
async def synthesize(task: SpeechRequest, authorization: str | None = Header(default=None)):
    authenticate(authorization)
    if not task.input.strip():
        raise HTTPException(422, "Text is empty")
    reference_id = task.voice if task.voice.startswith("ref-") else None
    reference_audio, reference_text = read_reference(reference_id) if reference_id else (None, None)
    model = await automatic_model(reference_id is not None, task.voice, reference_text or "") if task.model == "auto" else choose(task.model, reference_id is not None, task.voice)

    if model in OPEN_MODELS:
        url = os.environ["VOICE_" + model.upper() + "_URL"].rstrip("/")
        if not url.startswith("https://") and not url.startswith("http://127.0.0.1:"):
            raise HTTPException(503, "OSS upstream URL must be HTTPS or loopback")
        body = {
            "input": task.input, "voice": "auto" if reference_audio else task.voice,
            "language": task.language, "speed": task.speed,
            "response_format": "wav", "model": model,
            "num_step": task.num_step, "seed": task.seed,
        }
        if reference_audio:
            body["reference_audio_base64"] = base64.b64encode(reference_audio).decode("ascii")
            body["reference_text"] = reference_text
        upstream_key = os.getenv("VOICE_" + model.upper() + "_TOKEN", "")
        headers = {"Authorization": "Bearer " + upstream_key} if upstream_key else {}
        resp = await do_http("POST", url + "/v1/audio/speech", json=body, headers=headers)
        wav = normalized_wav(resp.content, "wav")
    elif model == "elevenlabs":
        if task.voice in ("auto", ""):
            raise HTTPException(422, "Enter ElevenLabs voice ID")
        # No silent creation of an expensive cloned voice.
        safe_voice = task.voice.strip()
        if not safe_voice.isalnum() or len(safe_voice) > 120:
            raise HTTPException(422, "Invalid ElevenLabs voice ID")
        resp = await do_http(
            "POST", "https://api.elevenlabs.io/v1/text-to-speech/" + safe_voice,
            params={"output_format": "mp3_44100_128"},
            headers={"xi-api-key": os.environ["ELEVENLABS_API_KEY"]},
            json={"text": task.input, "model_id": os.getenv("ELEVENLABS_MODEL", "eleven_multilingual_v2")},
        )
        wav = normalized_wav(resp.content, "mp3")
    elif model == "minimax":
        if task.voice in ("auto", ""):
            raise HTTPException(422, "Enter MiniMax preset or cloned voice ID")
        resp = await do_http(
            "POST", "https://api.minimax.io/v1/t2a_v2",
            headers={"Authorization": "Bearer " + os.environ["MINIMAX_API_KEY"]},
            json={"model": os.getenv("MINIMAX_MODEL", "speech-2.8-hd"),
                  "text": task.input,
                  "voice_setting": {"voice_id": task.voice, "speed": task.speed, "vol": 1, "pitch": 0},
                  "audio_setting": {"sample_rate": 32000, "bitrate": 128000, "format": "mp3", "channel": 1},
                  "stream": False, "output_format": "hex"},
        )
        try:
            result = resp.json()
            if result.get("base_resp", {}).get("status_code") not in (0, None):
                raise ValueError("Provider status")
            raw = bytes.fromhex(result["data"]["audio"])
        except (ValueError, KeyError, TypeError) as e:
            raise HTTPException(502, "Invalid MiniMax audio response") from e
        wav = normalized_wav(raw, "mp3")
    else:
        if task.voice in ("auto", ""):
            raise HTTPException(422, "Enter Fish Audio reference/voice ID")
        headers = {"Authorization": "Bearer " + os.environ["FISH_API_KEY"],
                   "model": os.getenv("FISH_MODEL", "s2.1-pro-free")}
        body = {"text": task.input, "format": "mp3", "prosody": {"speed": task.speed}}
        if reference_audio:
            if not reference_text or not reference_text.strip():
                raise HTTPException(422, "Fish inline cloning requires the exact reference transcript")
            import msgpack
            body["references"] = [{"audio": reference_audio, "text": reference_text}]
            headers["Content-Type"] = "application/msgpack"
            resp = await do_http("POST", "https://api.fish.audio/v1/tts", headers=headers,
                                 content=msgpack.packb(body, use_bin_type=True))
        else:
            body["reference_id"] = task.voice
            resp = await do_http("POST", "https://api.fish.audio/v1/tts", headers=headers, json=body)
        wav = normalized_wav(resp.content, "mp3")

    if model == "elevenlabs" and task.speed != 1:
        proc = subprocess.run(["ffmpeg", "-v", "error", "-nostdin", "-i", "pipe:0",
                "-af", f"atempo={task.speed}", "-f", "wav", "pipe:1"],
                input=wav, capture_output=True, timeout=90, check=False)
        if proc.returncode:
            raise HTTPException(502, "Speed adjustment failed")
        wav = normalized_wav(proc.stdout, "wav")

    # Basic truncation guard: duration alone cannot prove every word was pronounced.
    duration = check_wav(wav)
    significant = sum(1 for char in task.input if char.isalnum())
    # Conservative duration floor; still cannot verify actual spoken words.
    syllables = sum(1 for char in task.input if "\u3400" <= char <= "\u9fff")
    lower_bound = max(.25, syllables * .03 + (significant - syllables) * .011) if significant else 0
    if duration < lower_bound:
        raise HTTPException(502, "Audio unexpectedly short; partial output rejected")
    return Response(wav, media_type="audio/wav", headers={"X-VoiceStudio-Engine": model})
