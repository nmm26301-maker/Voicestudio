# VoiceStudio · Kaggle 免费 GPU（仅任务式合成）

* 从手机打开 https://www.kaggle.com/code → New Notebook。
* 把 [VoiceStudio-Qwen3-TTS.ipynb](VoiceStudio-Qwen3-TTS.ipynb)
  下载并通过 Kaggle 的 **File → Import Notebook** 导入（操作名称可能随 UI 改变）。
* Kaggle **Notebook Settings → Accelerator → GPU**。确保 Internet 已开启，以便首次安装 qwen-tts 和下载 Qwen 模型（Kaggle 配额/验证要求以账号实际显示为准）。
* 上传自己录制的 3—10 秒参考 WAV 至 Kaggle Dataset / Input。
* 修改 `REFERENCE_WAV`、`REFERENCE_TRANSCRIPT`、`TARGET_TEXT`；
  Run All，待完成后下载 `/kaggle/working/VoiceStudio-Qwen3-TTS.wav`。
* 回到 VoiceStudio → 作品 → 导入 WAV，试听和保存。

**不提供常驻 HTTPS API**。Notebook 容器临时运行，
GPU/网络额度与队列受 Kaggle 控制；没有一键跨平台永久部署。
请勿在公开 Notebook 输入 API Key 或上传未经授权的声音。

官方说明：https://www.kaggle.com/docs/notebooks
