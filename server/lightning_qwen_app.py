"""Single-Qwen VoiceStudio API for a Lightning AI GPU Studio.

Start: uvicorn server.lightning_qwen_app:app --host 0.0.0.0 --port 8000
Use Lightning's Ports plugin to expose HTTPS; the Studio must be running.
Requires VOICE_GATEWAY_TOKEN and a real GPU. No computer/Android models.
"""
from __future__ import annotations

import base64
import os

from fastapi import FastAPI, File, Header, HTTPException, UploadFile, Form

from server import cloud_gateway, open_model_worker

app = FastAPI(title="VoiceStudio Lightning GPU · Qwen3-TTS")


def _key() -> str:
    key = os.getenv("VOICE_GATEWAY_TOKEN", "")
    if len(key) < 24 or any(c.isspace() for c in key):
        raise HTTPException(503, "Server VOICE_GATEWAY_TOKEN is missing or too weak")
    return key


def _auth(authorization: str | None):
    _key()
    cloud_gateway.authenticate(authorization)


@app.get("/v1/health")
def health(authorization: str | None = Header(default=None)):
    _auth(authorization)
    return {
        "status": "ok",
        "configured": ["qwen3tts"],
        "voice_reference": ["qwen3tts"],
        "model_loaded": open_model_worker.loaded is not None,
        "auto_fallback_after_submit": False,
    }


@app.post("/v1/voices/reference")
async def reference(
    file: UploadFile = File(...),
    transcript: str = Form(""),
    authorization: str | None = Header(default=None),
):
    _auth(authorization)
    return await cloud_gateway.upload_reference(
        file=file, transcript=transcript, authorization=authorization
    )


@app.post("/v1/audio/speech")
def synthesize(
    request: cloud_gateway.SpeechRequest,
    authorization: str | None = Header(default=None),
):
    _auth(authorization)
    if request.model not in ("qwen3tts", "auto"):
        raise HTTPException(422, "Only Qwen3-TTS is deployed in this Studio")
    if not request.voice.startswith("ref-"):
        raise HTTPException(422, "Record/select a 3-10 second reference WAV")
    audio, reference_text = cloud_gateway.read_reference(request.voice)
    properties = request.model_dump()
    properties.update({
        "model": "qwen3tts",
        "voice": "auto",
        "reference_audio_base64": base64.b64encode(audio).decode("ascii"),
        "reference_text": reference_text,
    })
    worker_request = open_model_worker.WorkerRequest(**properties)
    # The remote Studio's GPU does the actual inference. 
    # The mobile app receives just the synthesized WAV bytes.
    os.environ["VOICE_WORKER_ENGINE"] = "qwen3tts"
    os.environ["VOICE_WORKER_TOKEN"] = _key()
    return open_model_worker.speech(
        worker_request, authorization="Bearer " + _key()
    )
