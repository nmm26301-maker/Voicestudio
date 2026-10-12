"""Deploy: modal deploy server/modal_app.py. No account key is embedded in Android."""
import modal

REV = "08be0b4ccbac3e13e374e86fbfead4b4cac343e2"
app = modal.App("voicestudio-omni")
cache = modal.Volume.from_name("voicestudio-model-cache", create_if_missing=True)
usage = modal.Dict.from_name("voicestudio-free-gpu-usage", create_if_missing=True)
image = (modal.Image.debian_slim(python_version="3.12")
         .apt_install("git", "ffmpeg", "libsndfile1")
         .pip_install("torch==2.8.0", "torchaudio==2.8.0", "fastapi[standard]", "soundfile",
                      f"git+https://github.com/k2-fsa/OmniVoice.git@{REV}")
         .env({"HF_HOME": "/cache", "VOICE_DAILY_MAX_CALLS": "10"}))

@app.cls(image=image, gpu="L4", memory=16384, timeout=600,
         min_containers=0, max_containers=1, scaledown_window=15,
         volumes={"/cache": cache})
class Speech:
    @modal.enter()
    def load(self):
        import torch
        from omnivoice import OmniVoice
        self.model = OmniVoice.from_pretrained("k2-fsa/OmniVoice", device_map="cuda", dtype=torch.float16)
        cache.commit()

    @modal.method()
    def generate(self, task: dict) -> bytes:
        import io
        import torch
        import soundfile as sf
        from omnivoice import OmniVoiceGenerationConfig
        torch.manual_seed(task["seed"])
        audio = self.model.generate(text=task["input"], language=task.get("language") or None,
                                    speed=task["speed"], generation_config=OmniVoiceGenerationConfig(num_step=task["num_step"]))
        buf = io.BytesIO()
        sf.write(buf, audio[0], self.model.sampling_rate, format="WAV", subtype="PCM_16")
        return buf.getvalue()

# Authentication and daily admission happen on CPU before any GPU allocation.
def create_api():
    import os
    import hmac
    import datetime
    from fastapi import FastAPI, Request, HTTPException
    from fastapi.responses import Response
    from pydantic import BaseModel, Field, ConfigDict

    class Task(BaseModel):
        model_config = ConfigDict(extra="ignore")
        input: str = Field(min_length=1, max_length=2000)
        model: str = "k2-fsa/OmniVoice"
        voice: str = "auto"
        language: str | None = None
        speed: float = Field(default=1, ge=0.5, le=2)
        num_step: int = 32
        seed: int = Field(default=42, ge=0, le=2147483647)
        response_format: str = "wav"

    web = FastAPI()

    @web.post("/v1/audio/speech")
    async def speech(request: Request):
        key = os.environ.get("VOICE_API_TOKEN", "")
        header = request.headers.get("authorization", "")
        if not key or not hmac.compare_digest(header, "Bearer " + key):
            raise HTTPException(401, "Invalid service token")
        body = await request.body()
        if len(body) > 32768:
            raise HTTPException(413, "Request too large")
        try:
            task = Task.model_validate_json(body)
        except ValueError:
            raise HTTPException(422, "Invalid speech request")
        if not task.input.strip() or task.num_step not in (8, 16, 32) or task.model != "k2-fsa/OmniVoice" or task.voice != "auto" or task.response_format != "wav":
            raise HTTPException(422, "Use OmniVoice, auto voice, WAV and 8/16/32 steps")
        today = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d")
        count = usage.get(today, 0)
        limit = int(os.environ.get("VOICE_DAILY_MAX_CALLS", "10"))
        if limit < 1 or count >= limit:
            raise HTTPException(429, "Daily application call limit reached")
        usage[today] = count + 1  # Reserve before GPU; failed runs still count.
        try:
            audio = await Speech().generate.remote.aio(task.model_dump())
        except Exception:
            raise HTTPException(503, "GPU generation failed; retry after checking server logs and credits")
        return Response(audio, media_type="audio/wav")

    return web


@app.function(image=image, secrets=[modal.Secret.from_name("voicestudio-api")],
              min_containers=0, max_containers=1, scaledown_window=15, timeout=600)
@modal.concurrent(max_inputs=1)
@modal.asgi_app()
def api():
    return create_api()
