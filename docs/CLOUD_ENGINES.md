# VoiceStudio 云端语音架构

这是云端改造开发分支：界面不再提供手机 OmniVoice、本地模型安装、本地 TTS。已有参考 WAV、作品与历史任务沿用原数据目录，手动确认后才能清除旧模型目录。

## 七种引擎

| 引擎 | 连接方式 | 参考录音 |
| --- | --- | --- |
| Fish Audio | 原有 Android API 或云端网关 | 平台已有 Voice ID |
| IndexTTS 2.5 | 自部署推理服务 | 支持上游适配的参考 WAV |
| CosyVoice 3 | 自部署推理服务 | 支持上游适配的参考 WAV |
| Qwen3-TTS | 自部署推理服务 | 支持上游适配的参考 WAV |
| Chatterbox V3 | 自部署推理服务 | 支持上游适配的参考 WAV |
| ElevenLabs | 网关调用官方 REST API | 平台已注册 Voice ID |
| MiniMax | 网关调用官方 REST API | 平台已注册 Voice ID |

表中“连接方式”不是已经部署好的云 GPU。开源模型的推理后端、权重、GPU 要自行部署；商业服务需要自己的 API Key，可能计费。自动选择仅在请求提交前挑选已配置的引擎，超时后不自动重复提交，避免重复计费。

## 自部署网关

需要 Python 3.11+、FFmpeg 和可用的 HTTPS 反代。

1. pip install -r server/cloud_requirements.txt
2. 设置环境变量 VOICE_GATEWAY_TOKEN 为随机高强度密钥，VOICE_REFERENCE_DIR 为有权限的持久化存储目录
3. 为开源模型设置 VOICE_QWEN3TTS_URL、VOICE_COSYVOICE3_URL、VOICE_INDEXTTS25_URL、VOICE_CHATTERBOXV3_URL；上游有密钥时再设置对应的 _TOKEN
4. 商业服务分别设置 ELEVENLABS_API_KEY、MINIMAX_API_KEY、FISH_API_KEY
5. 执行 uvicorn server.cloud_gateway:app --host 127.0.0.1 --port 8765
6. 经 HTTPS 反代后，在 VoiceStudio 输入网关 HTTPS 根地址或 /v1 路径，以及 VOICE_GATEWAY_TOKEN

### 开源推理服务的标准契约

网关对配置的上游根地址发出 POST /v1/audio/speech，JSON 包括 input、model、voice、language、speed、response_format=wav、num_step、seed。选用克隆录音时加上 reference_audio_base64（单声道 PCM16 WAV）和 reference_text（可以为空）。上游应返回**完整 WAV 字节流**；失败返回非200。**这些模型的权重和后端包装器未随本次改造部署，接口协议可对接不等于已能运行**。

### 语音与费用边界

- 输入自动无损分段，保存已完成 WAV，取消/网络中断后仅继续未完成部分。
- 只有用户显式选择录音时才向自己的 HTTPS 网关发送参考录音；服务器侧默认保留 72 小时，需自行管理数据删除。
- 检查 PCM16、时长、静音和不完整文件，不能代替逐字 ASR 识别和真实音质验收。
- 已提交的未知状态请求不会自动重新计费；要重新提交必须由用户确认。
- 不能通过“移除按钮”释放旧模型文件空间，要在“工具与任务 > 任务 > 清理旧离线模型”主动确认。
- APK 仍须原签名才可覆盖升级；不要删除旧版数据或将密钥提交到公开仓库。

## 回归

python -m compileall -q server/cloud_gateway.py
pytest -q server/test_cloud_gateway.py

云端实际端到端验收需要部署后的真实 GPU/API、本人授权的参考录音及手机测试，此开发分支不声明已完成。
