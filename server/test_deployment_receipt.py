from pathlib import Path
import json
import pytest
from tools.report_modal_endpoint import write_report


def test_actual_endpoint_report_has_no_credentials(tmp_path, monkeypatch):
    monkeypatch.setenv("MODAL_TOKEN_SECRET", "never-print-this")
    target=tmp_path/"connection.json";summary=tmp_path/"summary.md"
    result=write_report("https://demo--voicestudio-qwen3tts-voice-api.modal.run/",target,summary)
    assert json.loads(target.read_text())==result
    assert result["url"]=="https://demo--voicestudio-qwen3tts-voice-api.modal.run"
    assert result["engine"]=="qwen3tts" and result["provider"]=="modal"
    assert "never-print-this" not in summary.read_text()+target.read_text()
    assert "VOICE_GATEWAY_TOKEN" in summary.read_text()


@pytest.mark.parametrize("url",[None,"https://modal.com/apps","http://x.modal.run","https://x.modal.run?token=secret","https://key@x.modal.run","https://x.modal.run/path","https://not-modal.invalid"])
def test_report_refuses_dashboard_or_credential_url(tmp_path,url):
    with pytest.raises(ValueError):write_report(url,tmp_path/"not-created.json")
    assert not (tmp_path/"not-created.json").exists()
