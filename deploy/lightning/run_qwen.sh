#!/usr/bin/env bash
# Run INSIDE a Lightning GPU Studio terminal, never on your Android device.
set -euo pipefail
cd "${VOICE_SOURCE_ROOT:-$HOME/Voicestudio}"
if [[ ! -f server/lightning_qwen_app.py ]]; then
  echo "Missing VoiceStudio source. Clone the feature/cloud-voice-20261014 branch first." >&2
  exit 1
fi
if [[ -z "${VOICE_GATEWAY_TOKEN:-}" || "${#VOICE_GATEWAY_TOKEN}" -lt 24 ]]; then
  echo "Set an independent, 24+ character VOICE_GATEWAY_TOKEN as a Lightning Studio secret." >&2
  exit 1
fi
python - <<'PY'
import torch
assert torch.cuda.is_available(), "GPU is not enabled: switch the Lightning Studio to a GPU machine"
print("GPU:", torch.cuda.get_device_name())
PY
python -m pip install "qwen-tts" "fastapi>=0.115,<1" "httpx>=0.27,<1" "pydantic>=2,<3" \
  "python-multipart>=0.0.9,<1" "uvicorn>=0.30,<1" "soundfile"
export VOICE_REFERENCE_DIR="${VOICE_REFERENCE_DIR:-/tmp/voicestudio-references}"
export VOICE_WORKER_ENGINE=qwen3tts
export VOICE_QWEN_MODEL_ID=Qwen/Qwen3-TTS-12Hz-0.6B-Base
export VOICE_DEVICE=cuda:0
exec python -m uvicorn server.lightning_qwen_app:app --host 0.0.0.0 --port 8000 --workers 1
