"""Report an already-deployed Modal Web Function URL, without invoking its GPU."""
from __future__ import annotations
import json
import os
from pathlib import Path
from urllib.parse import urlsplit


def write_report(url: str, destination: Path, summary: Path | None = None, engine: str = "qwen3tts") -> dict:
    if engine not in ("qwen3tts", "indextts25", "cosyvoice3", "chatterboxv3", "gptsovits"):
        raise ValueError("Unsupported deployment engine")
    parsed = urlsplit(url or "")
    if (parsed.scheme != "https" or not (parsed.hostname or "").endswith(".modal.run")
            or parsed.username or parsed.password or parsed.query or parsed.fragment
            or parsed.path not in ("", "/") or parsed.port not in (None, 443)):
        raise ValueError("Modal did not return a valid HTTPS Web Endpoint URL")
    config = {"schema_version": 1, "provider": "modal", "engine": engine,
              "protocol": "rest", "url": url.rstrip("/")}
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(config, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if summary is not None:
        with summary.open("a", encoding="utf-8") as f:
            f.write("\n## VoiceStudio · 实际部署地址\n\n")
            f.write("引擎：`" + engine + "`。App 中选择同一个引擎。\n\n")
            f.write("复制下面的 Web Endpoint 到 App：**音色 → Modal → 实际 HTTPS 运行地址**。\n\n")
            f.write("```text\n" + config["url"] + "\n```\n\n")
            f.write("网关密钥填写 Modal Secret `voicestudio-cloud` 中的 `VOICE_GATEWAY_TOKEN`；不要填写 Modal 账号部署 Token。\n\n")
            f.write("也可下载本页 Artifacts 中的 `VoiceStudio-Modal-connection`，复制其中 JSON 到地址框。\n\n")
            f.write("本步骤仅查询已部署 Function 的地址，没有提交 GPU 合成，不能证明模型已成功加载或音质通过。\n")
    return config


def main():
    import modal
    engine = os.getenv("VOICE_DEPLOY_ENGINE", "qwen3tts")
    if engine not in ("qwen3tts", "indextts25", "cosyvoice3", "chatterboxv3", "gptsovits"):
        raise ValueError("Unsupported deployment engine")
    function = modal.Function.from_name("voicestudio-" + engine, "voice_api")
    url = function.get_web_url()
    if not url:
        raise RuntimeError("voice_api has no Web Endpoint; check Modal app/workspace and deployment logs")
    summary = os.getenv("GITHUB_STEP_SUMMARY")
    config = write_report(url, Path("deployment-output/VoiceStudio-Modal-Connection.json"),
                          Path(summary) if summary else None, engine=engine)
    print("VoiceStudio Web Endpoint:", config["url"])


if __name__ == "__main__":
    main()
