"""Official per-engine dependency recipes; run only inside a remote GPU image.

Pinned source revisions live in server/engine_recipes.py. Third-party dependencies
and Hub weights retain upstream version policies; this is not a frozen lockfile.
"""
import shlex
from server.engine_recipes import RECIPES
UPSTREAM = "/opt/voicestudio-upstream"
API_DEPS = '"fastapi>=0.115,<1" "httpx>=0.27,<1" "pydantic>=2,<3" "python-multipart>=0.0.9,<1" "uvicorn>=0.30,<1"'


def install_commands(engine, upstream=UPSTREAM, system=True):
    if engine not in RECIPES:
        raise ValueError("Unsupported deployment engine")
    recipe = RECIPES[engine]
    if engine == "qwen3tts":
        return ['python -m pip install qwen-tts ' + API_DEPS]
    upstream = shlex.quote(str(upstream))
    clone = ("git clone --no-checkout https://github.com/" + recipe["repo"] + ".git " + upstream +
             " && cd " + upstream + " && git checkout --detach " + recipe["revision"] +
             " && git submodule update --init --recursive")
    if engine == "indextts25":
        install = "python -m pip install uv && cd " + upstream + " && uv pip install " + ("--system" if system else '--python "$VIRTUAL_ENV/bin/python"') + " -e ."
    elif engine == "cosyvoice3":
        # Torch must exist before upstream deepspeed's package build starts.
        install = ("python -m pip install torch==2.3.1 torchaudio==2.3.1 && cd " + upstream +
                   " && python -m pip install -r requirements.txt")
    elif engine == "chatterboxv3":
        install = "python -m pip install -e " + upstream
    else:
        install = ("python -m pip install torch==2.6.0 torchaudio==2.6.0 && cd " + upstream +
                   " && python -m pip install -r extra-req.txt --no-deps && python -m pip install -r requirements.txt")
    return [clone, install, "python -m pip install " + API_DEPS]


def environment(engine):
    if engine not in RECIPES:
        raise ValueError("Unsupported deployment engine")
    return {"VOICE_DEPLOY_ENGINE": engine, "VOICE_WORKER_ENGINE": engine, "VOICE_DEVICE": "cuda:0",
            "VOICE_UPSTREAM_DIR": UPSTREAM, "GPT_SOVITS_NATIVE_URL": "http://127.0.0.1:9880",
            "VOICE_REFERENCE_DIR": "/references",
            "VOICE_QWEN_MODEL_ID": "Qwen/Qwen3-TTS-12Hz-0.6B-Base",
            "HF_HOME": "/root/.cache/huggingface",
            "PYTHONPATH": UPSTREAM + ":" + UPSTREAM + "/third_party/Matcha-TTS"}
