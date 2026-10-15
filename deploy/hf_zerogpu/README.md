---
title: VoiceStudio Qwen3 TTS ZeroGPU
emoji: 🎙️
colorFrom: indigo
colorTo: purple
sdk: gradio
app_file: app.py
pinned: false
license: apache-2.0
short_description: VoiceStudio voice cloning (GPU quota and consent apply)
---

# VoiceStudio · Qwen3-TTS ZeroGPU

Uses the Qwen3-TTS Base voice-clone model in a Hugging Face ZeroGPU
Gradio Space. Provides a named `/_clone_fn` endpoint compatible with
VoiceStudio's Gradio synthesis client, but requires a user reference WAV
and is not a public unlimited TTS service.

- Select **ZeroGPU** hardware in the Space Settings.
- Free HF accounts need to meet ZeroGPU hosting eligibility.
- ZeroGPU calls count against the caller's quota, can queue or fail.
- Use only your own/authorized reference recordings.
- First-load dependency/model download, Gradio queue behavior,
  and model-specific speed remain subject to live integration tests.
- For in-app use enter the **running Space** URL
  `https://<owner>-<space>.hf.space`, **not** the Hub repository URL.
