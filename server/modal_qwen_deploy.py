"""Modal GPU deployment for VoiceStudio Qwen3-TTS (single-model starter).

No local model weights and no public unauthenticated synthesis.
The caller must create a Modal Secret named 'voicestudio-cloud' containing
VOICE_GATEWAY_TOKEN, then deploy using the manual GitHub Actions workflow.

This is a deployment recipe, not proof of successful remote GPU synthesis.
GPU use and model cache/storage can incur Modal charges.
"""
import modal

app = modal.App("voicestudio-qwen3tts")

image = (
    modal.Image.debian_slim(python_version="3.12")
    .apt_install("ffmpeg", "libsndfile1", "libgomp1")
    .pip_install(
        "qwen-tts",
        "torch",
        "torchaudio",
        "fastapi>=0.115,<1",
        "httpx>=0.27,<1",
        "pydantic>=2,<3",
        "python-multipart>=0.0.9,<1",
        "uvicorn>=0.30,<1",
    )
    .env({
        "VOICE_REFERENCE_DIR": "/references",
        "VOICE_WORKER_ENGINE": "qwen3tts",
        "VOICE_DEVICE": "cuda:0",
        "HF_HOME": "/root/.cache/huggingface",
    })
    .add_local_python_source("server")
)

references = modal.Volume.from_name("voicestudio-voice-references", create_if_missing=True)
model_cache = modal.Volume.from_name("voicestudio-qwen-model-cache", create_if_missing=True)


@app.function(
    image=image,
    gpu="L4",
    max_containers=1,
    scaledown_window=60,
    timeout=900,
    volumes={
        "/references": references,
        "/root/.cache/huggingface": model_cache,
    },
    secrets=[modal.Secret.from_name("voicestudio-cloud")],
)
@modal.asgi_app()
def voice_api():
    import base64
    import os
    from fastapi import FastAPI, File, Form, Header, HTTPException, UploadFile
    from server import cloud_gateway, open_model_worker

    access_key = os.environ.get("VOICE_GATEWAY_TOKEN", "")
    if not access_key or len(access_key) < 24:
        raise RuntimeError("Modal secret voicestudio-cloud needs a strong VOICE_GATEWAY_TOKEN")

    # Reuse the existing, independently authenticated, GPU worker in process.
    # No second publicly exposed worker or HTTPS reverse proxy is necessary.
    os.environ["VOICE_WORKER_TOKEN"] = access_key
    api = FastAPI(title="VoiceStudio Qwen3-TTS cloud worker")

    @api.get("/v1/health")
    def health(authorization: str | None = Header(default=None)):
        cloud_gateway.authenticate(authorization)
        return {
            "status": "ok",
            "configured": ["qwen3tts"],
            "voice_reference": ["qwen3tts"],
            "model_loaded": open_model_worker.loaded is not None,
            "auto_fallback_after_submit": False,
        }

    @api.post("/v1/voices/reference")
    async def upload_reference(
        file: UploadFile = File(...),
        transcript: str = Form(""),
        authorization: str | None = Header(default=None),
    ):
        result = await cloud_gateway.upload_reference(
            file=file, transcript=transcript, authorization=authorization
        )
        # Modal containers scale to zero. Explicitly persist the WAV and metadata.
        references.commit()
        return result

    @api.post("/v1/audio/speech")
    def synthesize(
        request: cloud_gateway.SpeechRequest,
        authorization: str | None = Header(default=None),
    ):
        cloud_gateway.authenticate(authorization)
        if request.model not in ("qwen3tts", "auto"):
            raise HTTPException(422, "This endpoint hosts Qwen3-TTS only")
        if not request.voice.startswith("ref-"):
            raise HTTPException(422, "First select a 3-10 second reference WAV in VoiceStudio")
        audio, original_text = cloud_gateway.read_reference(request.voice)
        parameters = request.model_dump()
        parameters.update({
            "model": "qwen3tts",
            "voice": "auto",
            "reference_audio_base64": base64.b64encode(audio).decode("ascii"),
            "reference_text": original_text,
        })
        worker_request = open_model_worker.WorkerRequest(**parameters)
        # Model runs on the remote L4 GPU, not on the Android phone.
        response = open_model_worker.speech(
            worker_request, authorization="Bearer " + access_key
        )
        # Retain downloaded public model weights across cold starts.
        model_cache.commit()
        return response

    return api
