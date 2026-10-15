"""Remote Lightning Studio launcher. Never run on the Android phone.

Usage: VOICE_GATEWAY_TOKEN=... python tools/run_cloud_engine.py indextts25
Requires remote Linux GPU, ffmpeg, git, build tools, libsndfile, libsox and uv.
Every engine has its own Python version, virtualenv and source checkout.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from server.cloud_recipes import RECIPES, install_commands


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("engine", choices=sorted(RECIPES))
    args = parser.parse_args()
    key = os.getenv("VOICE_GATEWAY_TOKEN", "")
    if len(key) < 24 or any(c.isspace() for c in key):
        parser.error("Set a separate 24+ character VOICE_GATEWAY_TOKEN Studio secret")
    for binary in ("uv", "git", "ffmpeg", "nvidia-smi"):
        if not shutil.which(binary):
            parser.error("Missing remote dependency: " + binary)
    subprocess.run(["nvidia-smi", "--query-gpu=name", "--format=csv,noheader"], check=True)
    base = ROOT / ".cloud-envs" / args.engine
    source, venv = base / "upstream", base / "venv"
    commands = install_commands(args.engine, source, system=False)
    fingerprint = hashlib.sha256(json.dumps([RECIPES[args.engine], commands]).encode()).hexdigest()
    marker = base / "installed.json"
    base.mkdir(parents=True, exist_ok=True)
    subprocess.run(["uv", "venv", "--allow-existing", "--seed", "--python", RECIPES[args.engine]["python"], str(venv)], check=True)
    env = dict(os.environ, VIRTUAL_ENV=str(venv), PATH=str(venv / "bin") + os.pathsep + os.environ["PATH"],
               PYTHONPATH=os.pathsep.join((str(ROOT), str(source), str(source / "third_party/Matcha-TTS"))))
    if not marker.exists() or marker.read_text() != fingerprint:
        if source.exists():
            # A partial clone is not silently deleted or overwritten.
            parser.error("Incomplete or changed setup; rename/remove this isolated engine directory before retrying: " + str(base))
        for command in commands:
            subprocess.run(["bash", "-euc", command], env=env, check=True)
        marker.write_text(fingerprint)
    env.update(VOICE_DEPLOY_ENGINE=args.engine, VOICE_WORKER_ENGINE=args.engine,
               VOICE_UPSTREAM_DIR=str(source), VOICE_DEVICE="cuda:0", GPT_SOVITS_NATIVE_URL="http://127.0.0.1:9880",
               VOICE_QWEN_MODEL_ID="Qwen/Qwen3-TTS-12Hz-0.6B-Base",
               VOICE_REFERENCE_DIR=str(base / "references"))
    subprocess.run([str(venv / "bin/python"), "-c", "import torch; assert torch.cuda.is_available(), 'Enable a GPU Studio'"], env=env, check=True)
    os.execve(str(venv / "bin/python"), [str(venv / "bin/python"), "-m", "uvicorn",
              "server.lightning_engine_app:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"], env)


if __name__ == "__main__":
    main()
