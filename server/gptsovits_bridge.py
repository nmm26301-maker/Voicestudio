"""GPT-SoVITS api_v2.py adapter for VoiceStudio's normalized HTTPS gateway.

Run this bridge on the SAME trusted machine / filesystem namespace as GPT-SoVITS.
Its native /tts endpoint reads a server-side path; never allow a mobile client to
supply that path. Bind the native GPT-SoVITS server to 127.0.0.1, behind bridge.
This bridge does not bundle or install GPU weights.
"""
from __future__ import annotations

import base64
import binascii
import hmac
import os
from pathlib import Path
import socket
import tempfile
from urllib.parse import urlsplit

import httpx
from fastapi import FastAPI, Header, HTTPException
from fastapi.responses import Response
from pydantic import Field

from server.cloud_gateway import SpeechRequest, check_wav, normalized_wav

app = FastAPI(title="VoiceStudio GPT-SoVITS bridge")
MAX_REFERENCE_B64 = 7 * 1024 * 1024


class GPTRequest(SpeechRequest):
    reference_audio_base64: str | None = Field(default=None, max_length=MAX_REFERENCE_B64)
    reference_text: str = Field(default="", max_length=2000)


def authenticate(value: str | None) -> None:
    key = os.getenv("VOICE_GPTSOVITS_TOKEN", "")
    if not key or not value or not hmac.compare_digest(value, "Bearer " + key):
        raise HTTPException(401, "Unauthorized GPT-SoVITS bridge")


def native() -> str:
    """Disallow SSRF and non-loopback provider addresses."""
    value = os.getenv("GPT_SOVITS_NATIVE_URL", "http://127.0.0.1:9880").rstrip("/")
    parts = urlsplit(value)
    if parts.scheme != "http" or parts.hostname != "127.0.0.1" or parts.username or parts.password or parts.query or parts.fragment or parts.path:
        raise HTTPException(503, "GPT native service must bind only to http://127.0.0.1:<port>")
    if not parts.port or not 1 <= parts.port <= 65535:
        raise HTTPException(503, "GPT native port invalid")
    return value


def translated_language(raw: str | None, text: str) -> str:
    value=(raw or "auto").lower().replace("_","-").split("-")[0]
    if value in ("auto","none"):
        return "zh" if any("\u3400"<=c<="\u9fff" for c in text) else "en"
    if value not in ("zh","en","ja","ko","yue"):
        raise HTTPException(422, "Unsupported GPT-SoVITS language")
    return value


@app.get("/v1/health")
def health(authorization: str | None = Header(None)):
    authenticate(authorization)
    root = native()
    parsed=urlsplit(root)
    try:
        with socket.create_connection(("127.0.0.1", parsed.port), timeout=1.5):
            pass
    except OSError as e:
        raise HTTPException(503, "GPT-SoVITS is not accepting connections") from e
    return {"status": "ok", "engine": "gptsovits", "model_loaded": False}


@app.post("/v1/audio/speech")
async def speech(task: GPTRequest, authorization: str | None = Header(None)):
    authenticate(authorization)
    endpoint = native()
    if task.model not in ("gptsovits", "auto"):
        raise HTTPException(422, "Wrong GPT-SoVITS engine")
    if not task.reference_audio_base64:
        raise HTTPException(422, "GPT-SoVITS requires a reference recording")
    if not task.reference_text.strip():
        raise HTTPException(422, "GPT-SoVITS requires the exact reference transcript")
    try:
        reference = base64.b64decode(task.reference_audio_base64, validate=True)
    except (ValueError,binascii.Error) as e:
        raise HTTPException(422, "Malformed base64 reference") from e
    check_wav(reference, reference=True)
    lang=translated_language(task.language,task.input)
    # Native api_v2.py reads ref_audio_path from its own filesystem. Shared mount
    # and strict loopback binding are REQUIRED; do not expose /tts publicly.
    root=Path(os.getenv("GPT_SOVITS_SHARED_DIR", "/tmp"))
    if not root.is_dir() or not root.resolve().is_dir():
        raise HTTPException(503, "GPT shared directory unavailable")
    with tempfile.TemporaryDirectory(prefix="vs-gpt-", dir=root) as temp:
        wav=Path(temp)/"reference.wav"
        wav.write_bytes(reference)
        os.chmod(wav,0o600)
        body={"text":task.input, "text_lang":lang, "ref_audio_path":str(wav),
              "prompt_text":task.reference_text, "prompt_lang":translated_language(None, task.reference_text),
              "speed_factor":task.speed, "text_split_method":"cut5",
              "media_type":"wav", "streaming_mode":False, "batch_size":1}
        async with httpx.AsyncClient(timeout=httpx.Timeout(180,connect=10),follow_redirects=False) as client:
            try:
                response=await client.post(endpoint+"/tts",json=body)
            except httpx.TimeoutException as e:
                raise HTTPException(504,"Native synthesis timed out, submission state unknown") from e
            except httpx.HTTPError as e:
                raise HTTPException(502,"Native service connection failed") from e
        if response.status_code != 200:
            raise HTTPException(502,f"Native GPT-SoVITS rejected synthesis HTTP {response.status_code}")
        if len(response.content)>48*1024*1024:
            raise HTTPException(502,"Native GPT-SoVITS response too large")
        output=normalized_wav(response.content,"wav")
        return Response(output,media_type="audio/wav")
