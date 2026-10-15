# Lightning AI Studio · VoiceStudio Qwen3-TTS

1. On Android open https://lightning.ai/ and create/verify an account.
2. Launch a new **GPU Studio** (not the always-free CPU Studio). T4 or L4
   may work if enough free credits remain; verify the GPU is available.
3. In the Studio's browser terminal:
   `git clone -b feature/cloud-voice-20261014 https://github.com/nmm26301-maker/Voicestudio.git`
   and `cd Voicestudio`.
4. In Lightning's **Studio Secrets** add `VOICE_GATEWAY_TOKEN` with a
   **random 24+ ASCII character string**, and restart the Studio so the
   secret becomes available as an environment variable.
5. In the GPU Studio terminal run `bash deploy/lightning/run_qwen.sh`.
6. In Lightning's **Ports** plugin expose port `8000`, obtain the
   actual public HTTPS endpoint, and restrict visibility if the platform
   supports it. All VoiceStudio endpoints require Bearer token.
7. Back in VoiceStudio select **Qwen3-TTS > My cloud gateway**,
   paste that HTTPS URL and the **same VOICE_GATEWAY_TOKEN**.
   Record an authorized 3—10 second reference WAV, test health,
   then synthesize a short sentence.

The Studio must remain running while VoiceStudio sends requests.
Credits can expire/consume; free CPU Studio does NOT provide a GPU.
A restarted Studio may lose temporary uploaded reference files;
you may need to upload the audio again. Network endpoints may change.
Use only authorized recordings. Real GPU inference is not verified yet.
Official platform instructions: https://lightning.ai/docs/ and
https://api.lightning.ai/docs/platform/build/host-web-apps
