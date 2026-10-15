"""Manual-only isolated GPU deployment. Select VOICE_DEPLOY_ENGINE first.

This module builds a remote image when a user runs modal deploy. Importing it
does not attest that the dependency image or actual GPU inference has passed.
"""
import os
import modal
from server.cloud_recipes import RECIPES, environment, install_commands

engine = os.environ.get("VOICE_DEPLOY_ENGINE", "qwen3tts")
if engine not in RECIPES:
    raise ValueError("Unsupported VOICE_DEPLOY_ENGINE")
app = modal.App("voicestudio-" + engine)
image = (modal.Image.debian_slim(python_version=RECIPES[engine]["python"])
         .apt_install("git", "ffmpeg", "libsndfile1", "libgomp1", "sox", "libsox-dev", "build-essential", "cmake", "pkg-config")
         .run_commands(*install_commands(engine))
         .env(environment(engine))
         .add_local_python_source("server"))
references = modal.Volume.from_name("voicestudio-" + engine + "-references", create_if_missing=True)
cache = modal.Volume.from_name("voicestudio-" + engine + "-models", create_if_missing=True)


@app.function(image=image, gpu="L4", max_containers=1, scaledown_window=60, timeout=900,
              volumes={"/references": references, "/root/.cache/huggingface": cache},
              secrets=[modal.Secret.from_name("voicestudio-cloud")])
@modal.asgi_app()
def voice_api():
    from server.single_engine_app import create_app
    return create_app(engine, persist_reference=references.commit, persist_cache=cache.commit)
