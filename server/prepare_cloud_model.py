"""Remote-only model preparation. No downloads at import or health time."""
from __future__ import annotations
import os
from pathlib import Path
import socket
import subprocess
import sys
import threading
import time
import zipfile
from fastapi import HTTPException

_native = None
_native_lock = threading.Lock()


def prepare_worker(engine):
    if engine not in ("indextts25", "cosyvoice3"):
        return
    from huggingface_hub import snapshot_download
    models = {"indextts25": "IndexTeam/IndexTTS-2.5",
              "cosyvoice3": "FunAudioLLM/Fun-CosyVoice3-0.5B-2512"}
    # Use Hub's revision-aware cache; do not mistake an incomplete local folder
    # for a successful model download after a cancelled/failed cold start.
    directory = snapshot_download(models[engine])
    os.environ["VOICE_MODEL_DIR"] = directory


def _gpt_assets(root: Path):
    from huggingface_hub import snapshot_download, hf_hub_download
    import yaml
    import nltk
    model = Path(snapshot_download("lj1995/GPT-SoVITS", allow_patterns=[
        "chinese-roberta-wwm-ext-large/*", "chinese-hubert-base/*",
        "gsv-v2final-pretrained/s1bert25hz-5kh-longer-epoch=12-step=369668.ckpt",
        "gsv-v2final-pretrained/s2G2333k.pth"]))
    # Official V2 baseline: no training, ASR, UVR5 or extra vocoder weights.
    config = {"device": "cuda", "is_half": True, "version": "v2",
              "bert_base_path": str(model / "chinese-roberta-wwm-ext-large"),
              "cnhuhbert_base_path": str(model / "chinese-hubert-base"),
              "t2s_weights_path": str(model / "gsv-v2final-pretrained/s1bert25hz-5kh-longer-epoch=12-step=369668.ckpt"),
              "vits_weights_path": str(model / "gsv-v2final-pretrained/s2G2333k.pth")}
    for name in ("bert_base_path", "cnhuhbert_base_path", "t2s_weights_path", "vits_weights_path"):
        if not Path(config[name]).exists():
            raise RuntimeError("Missing GPT-SoVITS pretrained asset: " + name)
    text = root / "GPT_SoVITS/text"
    if not (text / "G2PWModel/g2pW.onnx").is_file():
        archive = hf_hub_download("XXXXRT/GPT-SoVITS-Pretrained", "G2PWModel.zip")
        with zipfile.ZipFile(archive) as z:
            for entry in z.infolist():
                target = (text / entry.filename).resolve()
                if not target.is_relative_to(text.resolve()):
                    raise RuntimeError("Invalid G2PW archive path")
            z.extractall(text)
    for package in ("cmudict", "averaged_perceptron_tagger", "averaged_perceptron_tagger_eng"):
        if not nltk.download(package, quiet=True):
            raise RuntimeError("GPT language data download failed")
    path = root / "voicestudio-tts.yaml"
    path.write_text(yaml.safe_dump({"custom": config}), encoding="utf-8")
    return path


def ensure_gpt_native():
    global _native
    root = Path(os.getenv("VOICE_UPSTREAM_DIR", "/opt/voicestudio-upstream"))
    with _native_lock:
        if _native is None or _native.poll() is not None:
            config = _gpt_assets(root)
            # Fixed child command, fixed loopback port; never accept client paths.
            _native = subprocess.Popen([sys.executable, "api_v2.py", "-a", "127.0.0.1",
                                       "-p", "9880", "-c", str(config)], cwd=root)
        deadline = time.monotonic() + 180
        while time.monotonic() < deadline:
            if _native.poll() is not None:
                raise HTTPException(503, "GPT native process exited; inspect cloud logs")
            try:
                with socket.create_connection(("127.0.0.1", 9880), timeout=1):
                    return
            except OSError:
                time.sleep(0.25)
        # No synthesis was sent; future calls may wait for this same process.
        raise HTTPException(503, "GPT model still starting; no synthesis submitted")
