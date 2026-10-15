"""Authenticated single-engine API, shared by isolated Modal/Lightning containers.

Health does not download weights or submit inference. GPT's native API is a
private child process, started lazily after validating a synthesis request.
"""
from __future__ import annotations
import base64
import os
import threading
from fastapi import FastAPI, File, Form, Header, HTTPException, UploadFile
from starlette.concurrency import run_in_threadpool
from server import cloud_gateway as gateway, open_model_worker as worker

ENGINES = (*worker.WORKER_MODELS, "gptsovits")


def create_app(engine: str, persist_reference=None, persist_cache=None) -> FastAPI:
    if engine not in ENGINES:
        raise ValueError("Unknown deployment engine")
    app = FastAPI(title="VoiceStudio cloud · " + engine)
    busy = threading.Lock()

    def auth(value):
        key = os.getenv("VOICE_GATEWAY_TOKEN", "")
        if len(key) < 24 or any(c.isspace() for c in key):
            raise HTTPException(503, "Configure a strong VOICE_GATEWAY_TOKEN")
        gateway.authenticate(value)
        return key

    @app.get("/v1/health")
    def health(authorization: str | None = Header(None)):
        auth(authorization)
        return {"status": "ok", "configured": [engine], "voice_reference": [engine],
                "model_loaded": worker.loaded is not None and worker.loaded_engine == engine,
                "auto_fallback_after_submit": False}

    @app.post("/v1/voices/reference")
    async def reference(file: UploadFile = File(...), transcript: str = Form(""),
                        authorization: str | None = Header(None)):
        auth(authorization)
        result = await gateway.upload_reference(file=file, transcript=transcript, authorization=authorization)
        if persist_reference:
            await run_in_threadpool(persist_reference)
        return result

    @app.post("/v1/audio/speech")
    async def speech(request: gateway.SpeechRequest, authorization: str | None = Header(None)):
        key = auth(authorization)
        if not request.input.strip():
            raise HTTPException(422, "Text is empty")
        if request.model not in (engine, "auto"):
            raise HTTPException(422, "This endpoint hosts " + engine + " only")
        if not request.voice.startswith("ref-"):
            raise HTTPException(422, "First upload a 3-10 second reference WAV")
        audio, transcript = gateway.read_reference(request.voice)
        if engine == "gptsovits" and not transcript.strip():
            raise HTTPException(422, "GPT-SoVITS requires the exact reference transcript")
        if not busy.acquire(blocking=False):
            raise HTTPException(429, "Worker busy; no synthesis submitted")
        try:
            parameters = request.model_dump()
            parameters.update(model=engine, voice="auto", reference_text=transcript,
                              reference_audio_base64=base64.b64encode(audio).decode("ascii"))
            if engine == "gptsovits":
                from server import gptsovits_bridge, prepare_cloud_model
                os.environ["VOICE_GPTSOVITS_TOKEN"] = key
                await run_in_threadpool(prepare_cloud_model.ensure_gpt_native)
                result = await gptsovits_bridge.speech(gptsovits_bridge.GPTRequest(**parameters),
                                                      authorization="Bearer " + key)
            else:
                from server.prepare_cloud_model import prepare_worker
                os.environ["VOICE_WORKER_ENGINE"] = engine
                os.environ["VOICE_WORKER_TOKEN"] = key
                await run_in_threadpool(prepare_worker, engine)
                result = await run_in_threadpool(worker.speech, worker.WorkerRequest(**parameters),
                                                 authorization="Bearer " + key)
            if persist_cache:
                await run_in_threadpool(persist_cache)
            return result
        except HTTPException:
            raise
        except Exception as exc:
            import logging
            logging.exception("Isolated engine request failed")
            raise HTTPException(503, "Cloud engine failed; inspect server logs before resubmitting") from exc
        finally:
            busy.release()

    return app
