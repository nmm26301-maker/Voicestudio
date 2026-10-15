"""Qwen3-TTS reference voice clone on HF ZeroGPU.

For compatible VoiceStudio Gradio client, the named API is /_clone_fn,
with the same 12-position input order as the existing Gradio protocol.
Use your OWN/authorized audio only. HF ZeroGPU use is quota-limited.
"""
import os
import gradio as gr
import numpy as np
import spaces
import torch

MODEL_ID = os.getenv("VOICE_QWEN_MODEL", "Qwen/Qwen3-TTS-12Hz-0.6B-Base")
_model = None


def _lang(name, text):
    values = {
        "zh": "Chinese", "chinese": "Chinese", "中文": "Chinese",
        "en": "English", "english": "English", "ja": "Japanese",
        "japanese": "Japanese", "ko": "Korean", "korean": "Korean",
        "es": "Spanish", "fr": "French", "de": "German",
        "it": "Italian", "ru": "Russian", "pt": "Portuguese",
    }
    normalized = (name or "auto").lower().split("-")[0]
    if normalized in values:
        return values[normalized]
    return "Chinese" if any("\u3400" <= c <= "\u9fff" for c in text) else "English"


def _get_model():
    global _model
    if _model is None:
        from qwen_tts import Qwen3TTSModel
        # Matches Qwen's official ZeroGPU demonstration pattern: lazy
        # construction inside a GPU-decorated handler.
        _model = Qwen3TTSModel.from_pretrained(
            MODEL_ID, device_map="cuda", dtype=torch.bfloat16,
        )
    return _model


@spaces.GPU(duration=60)
def _clone_fn(text, language, reference, reference_text, unused_prompt,
              steps, sampling_scale, do_sample, speed, seed,
              normalize, long_mode):
    if not text or not text.strip():
        raise gr.Error("请先输入要合成的文字")
    if len(text) > 120:
        raise gr.Error("ZeroGPU 单次最多 120 字；请分成短句，节省免费额度")
    if not reference:
        raise gr.Error("请先录制并上传自己有权使用的 3—10 秒参考 WAV")
    if abs(float(speed) - 1.0) > .001:
        raise gr.Error("ZeroGPU 本版仅支持 1.0× 语速；请在 VoiceStudio 调整")
    from pathlib import Path
    if not Path(reference).is_file():
        raise gr.Error("参考音频文件不存在，请重新上传")
    import soundfile as sf
    info = sf.info(reference)
    if info.duration < 3 or info.duration > 10.01:
        raise gr.Error("参考录音必须为 3—10 秒")
    if not 1 <= info.channels <= 2:
        raise gr.Error("参考音频声道数不支持")
    model = _get_model()
    sample, sample_rate = model.generate_voice_clone(
        text=text.strip(), language=_lang(language, text),
        ref_audio=reference,
        ref_text=(reference_text or "").strip() or None,
        x_vector_only_mode=not bool((reference_text or "").strip()),
    )
    if len(sample) != 1:
        raise gr.Error("模型未返回完整语音")
    waveform = sample[0]
    if hasattr(waveform, "detach"):
        waveform = waveform.detach().float().cpu().numpy()
    x = np.asarray(waveform, dtype=np.float32).reshape(-1)
    if not len(x) or not np.all(np.isfinite(x)):
        raise gr.Error("模型音频无效")
    x = np.clip(x, -1, 1)
    return (int(sample_rate), (x * 32767.0).astype(np.int16))


def _design_fn(*unused):
    raise gr.Error("本 Space 使用 Qwen3-TTS Base，需要先选择 3—10 秒参考录音")


with gr.Blocks(title="VoiceStudio · Qwen3-TTS ZeroGPU") as demo:
    gr.Markdown("# VoiceStudio · Qwen3-TTS 云端免费试听\n"
                "ZeroGPU 按 Hugging Face 每日配额排队；不保证随时可用。"
                "上传自己的或已获授权的录音。")
    text = gr.Textbox(label="合成文字（120 字以内）", lines=3)
    lang = gr.Dropdown(
        label="语言", choices=["Auto", "zh", "en", "ja", "ko", "fr", "de", "es", "ru", "pt", "it"],
        value="Auto",
    )
    audio = gr.Audio(label="参考 WAV（3—10 秒）", type="filepath", sources=["upload", "microphone"])
    original = gr.Textbox(label="参考录音准确原文（推荐填写）", lines=2)
    hidden_prompt = gr.Textbox(value="", visible=False)
    steps = gr.Number(value=16, visible=False)
    scale = gr.Number(value=2.0, visible=False)
    do_sample = gr.Checkbox(value=True, visible=False)
    speed = gr.Number(value=1.0, visible=False)
    seed = gr.Number(value=42, visible=False)
    normalize = gr.Checkbox(value=True, visible=False)
    long_mode = gr.Checkbox(value=True, visible=False)
    output = gr.Audio(label="合成声音", type="numpy", format="wav")
    run = gr.Button("♫ 开始合成", variant="primary")
    inputs = [text, lang, audio, original, hidden_prompt, steps, scale,
              do_sample, speed, seed, normalize, long_mode]
    run.click(_clone_fn, inputs=inputs, outputs=output,
              api_name="_clone_fn", show_progress="full")
    # The legacy Android client chooses this when no reference is selected.
    # Return an explicit error, never claim a clone without a reference.
    no_ref = gr.Button("缺少参考录音时的提示", visible=False)
    no_ref.click(_design_fn, inputs=[text], outputs=output, api_name="_design_fn")

demo.queue(default_concurrency_limit=1)
demo.launch()
