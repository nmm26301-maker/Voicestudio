# Generation wrapper adapted from k2-fsa/OmniVoice, commit 08be0b4ccbac3e13e374e86fbfead4b4cac343e2.
# Copyright 2026 Xiaomi Corp. (Han Zhu). Licensed under Apache-2.0; see LICENSE.
import numpy as np
import spaces
import torch
from omnivoice import OmniVoice, OmniVoiceGenerationConfig
from omnivoice.cli.demo import build_demo

model = OmniVoice.from_pretrained("k2-fsa/OmniVoice", device_map="cuda", dtype=torch.float16, load_asr=True)

@spaces.GPU(duration=60)
def generate(text, language, ref_audio, instruct, num_step, guidance_scale, denoise,
             speed, duration, preprocess_prompt, postprocess_output, mode, ref_text=None):
    if not text or not text.strip():
        return None, "Please enter text."
    config = OmniVoiceGenerationConfig(num_step=int(num_step or 32), guidance_scale=float(guidance_scale),
                                      denoise=bool(denoise), preprocess_prompt=bool(preprocess_prompt),
                                      postprocess_output=bool(postprocess_output))
    kwargs = {"text": text.strip(), "language": language if language and language != "Auto" else None,
              "generation_config": config, "speed": float(speed or 1)}
    if duration is not None and float(duration) > 0:
        kwargs["duration"] = float(duration)
    if mode == "clone":
        if not ref_audio:
            return None, "Please select a reference recording."
        kwargs["voice_clone_prompt"] = model.create_voice_clone_prompt(ref_audio=ref_audio, ref_text=ref_text)
    if instruct and instruct.strip():
        kwargs["instruct"] = instruct.strip()
    audio = model.generate(**kwargs)
    return (model.sampling_rate, (audio[0] * 32767).astype(np.int16)), "Done."

build_demo(model, "k2-fsa/OmniVoice", generate_fn=generate).queue().launch()
