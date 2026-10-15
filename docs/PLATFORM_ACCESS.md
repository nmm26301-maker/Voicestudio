# VoiceStudio 1.5.27.2 · 独立平台设置

音色页分为官方 API 与独立云端模型。每家分别保存地址、密钥、音色 ID 和参考录音；切换平台不会覆盖另一家的连接。沿用 1.5.27 的界面和动效。

## 官方 API

| 平台 | API Key 页面 | 合成地址 | 音色 |
| --- | --- | --- | --- |
| Fish Audio | https://fish.audio/app/api-keys/ | `https://api.fish.audio/v1/tts` | 保留原有账户与音色入口 |
| ElevenLabs | https://elevenlabs.io/app/developers/api-keys | `https://api.elevenlabs.io/v1/text-to-speech` | 从本平台读取 Voice ID |
| MiniMax 中国站 | https://platform.minimax.cn/console/access?tab=api-keys | `https://api.minimax.cn/v1/t2a_v2` | 中国站自己的 Voice ID 与密钥 |
| MiniMax 国际站 | https://platform.minimax.io/console/access | `https://api.minimax.io/v1/t2a_v2` | 国际站自己的 Voice ID 与密钥 |
| Cartesia Sonic | https://play.cartesia.ai/keys | `https://api.cartesia.ai/tts/bytes` | Cartesia Voice ID |

1. 打开“音色”，点击具体平台的“独立配置”。MiniMax 先明确选择账号地区。
2. 填本平台 API Key，点“读取本平台音色 ID”选择声音，也可直接填已有 Voice ID。
3. 点“检查本平台认证与音色”，查看平台返回结果。此操作不提交合成。
4. 点“保存并使用这家”，回到合成页输入文字，再点“开始合成”。

ElevenLabs / MiniMax / Cartesia 的官方 TTS 只使用其平台 Voice ID，不继承手机的参考录音。Fish 原有入口继续保留。官方地址固定，不需要手填共享网关。

检查成功表示认证与音色可查询，不表示 TTS 权限、余额或音质已通过验收。任务失败时打开任务详情查看平台名、HTTP 状态、错误码和消息；已知密钥会隐藏。401 提示核对密钥与地区，403 提示核对权限，具体原因以平台原文为准。未确认提交状态的失败任务不会自动重复请求。

## 五款开源模型

IndexTTS 2.5、CosyVoice 3、Qwen3-TTS、Chatterbox V3、GPT-SoVITS 在云 GPU 运行。每个模型有自己的配置入口，并分别保存 REST、Modal、Lightning 连接；Qwen ZeroGPU Space 也单独保存。

1. 在“音色”页点击目标模型，选择已部署服务的类型。
2. 填该模型实际运行的 HTTPS API 地址及其自己的访问密钥。Modal 使用实际 `*.modal.run` Web Endpoint；不能填控制台页面。
3. 选择这个模型自己的 3—10 秒参考音色；GPT-SoVITS 还需要准确的录音原文。
4. 检测、保存并使用，再回到合成页开始生成。

Modal / Lightning 的 `VOICE_GATEWAY_TOKEN` 是你为服务设置的访问密钥，不是账号部署 Token。HF Token、百炼 API Key 也不能当作这些服务的网关密钥。Qwen ZeroGPU Space 支持匿名时可留空 HF Token。

[手机 Modal 部署](MODAL_PHONE_DEPLOY.md) · [五引擎部署](OPEN_MODEL_DEPLOY.md) · [免费网页试听](FREE_MOBILE_CLOUD.md)

## 请求格式与验证

ElevenLabs 使用 `eleven_multilingual_v2`，语速 0.7—1.2；MiniMax 使用 `speech-2.8-hd`；Cartesia 使用 `sonic-3` 与 `2026-08-14` API 版本，语速 0.6—1.5，输出 24kHz PCM16 WAV。超出支持范围会提示调整。

官方参考：[ElevenLabs TTS](https://elevenlabs.io/docs/api-reference/text-to-speech/convert)、[MiniMax 中国站 TTS](https://platform.minimax.cn/docs/api-reference/speech-t2a-http)、[MiniMax 国际站 TTS](https://platform.minimax.io/docs/api-reference/speech-t2a-http)、[Cartesia TTS](https://docs.cartesia.ai/api-reference/tts/bytes)。2026-10-15 核对。

已验证编译、签名、平台独立保存、任务切换后的凭据归属和模拟请求。尚未使用真实平台账号完成 TTS、手机安装/播放或 GPU 音质验收，详见 [当前验证记录](../validation/current-checks.md)。
