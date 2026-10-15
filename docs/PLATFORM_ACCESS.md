# 1.5.27.1 修复提示

MiniMax 现可明确选择中国站 `api.minimax.cn` 或国际站 `api.minimax.io`，对应不同的密钥范围；不要混用。音色读取增加实际 get_voice 查询。Cartesia 直连速度现支持 0.6—1.5，并传 generation_config.speed。旧文下述 1.0× 限制已取消。

官方平台切换、录音继承和错误诊断的修复详见 [1.5.27.1](../CHANGELOG-1.5.27.1.md)。以下保留原平台操作参考。

# 平台申请、密钥与自动地址

VoiceStudio 1.5.25 在“音色 > 九引擎 / 自动”内添加账号申请、API Key 页面、粘贴密钥和恢复自动地址。Fish、ElevenLabs、MiniMax、Cartesia 可直接调用官方 API，无需另填自建网关。开源模型使用保存过的自建网关，并自动复用该地址。

## 注册和密钥入口

| 引擎 | 账号 / 资源申请 | 密钥入口 | 本版连接方式 |
| --- | --- | --- | --- |
| Fish Audio | https://fish.audio/auth/signup/ | https://fish.audio/app/api-keys/ | 官方直连或自建网关；录音克隆需准确原文 |
| ElevenLabs | https://elevenlabs.io/app/sign-up | https://elevenlabs.io/app/developers/api-keys | 官方直连或自建网关；使用平台 Voice ID |
| MiniMax | https://platform.minimax.io/login | https://platform.minimax.io/console/access | 官方直连或自建网关；使用普通语音 API Key 和平台 Voice ID |
| Cartesia Sonic | https://play.cartesia.ai/ | https://play.cartesia.ai/keys | 官方 `/tts/bytes`；使用已注册的 Cartesia Voice ID |
| GPT-SoVITS | https://github.com/RVC-Boss/GPT-SoVITS | 自建桥接器密钥 | 官方原生 `/tts` 经服务器上的安全桥接器连接，不需要手机本地模型 |
| IndexTTS 2.5 | https://huggingface.co/join | https://huggingface.co/settings/tokens | 申请 GPU/部署 Token，部署模型与网关后使用 |
| CosyVoice 3 开源模型 | https://huggingface.co/join | https://huggingface.co/settings/tokens | 申请 GPU/部署 Token，部署模型与网关后使用 |
| Qwen3-TTS 开源模型 | https://huggingface.co/join | https://huggingface.co/settings/tokens | 申请 GPU/部署 Token，部署模型与网关后使用 |
| Chatterbox V3 | https://huggingface.co/join | https://huggingface.co/settings/tokens | 申请 GPU/部署 Token，部署模型与网关后使用 |

账号登录、验证码、资格/额度审批和生成密钥需要用户在官方平台完成。App 只提供入口，用户复制密钥后主动点“粘贴已领取的密钥”；不会自动读取剪贴板或冒充账号申请成功。

Qwen/CosyVoice 另有阿里云百炼托管服务，申请入口 https://bailian.console.aliyun.com/，密钥说明 https://help.aliyun.com/zh/model-studio/get-api-key 。其模型、地域/业务空间地址和接口与本版开源 worker 不同，本版未接入百炼直连；不要把百炼 API Key 当作自建网关密钥。

## 地址怎样自动填写

| 官方直连平台 | 自动填写的实际地址 |
| --- | --- |
| Fish | `https://api.fish.audio/v1/tts` |
| ElevenLabs | `https://api.elevenlabs.io/v1/text-to-speech`（生成时追加选定 Voice ID） |
| MiniMax | `https://api.minimax.io/v1/t2a_v2` |
| Cartesia Sonic | `https://api.cartesia.ai/tts/bytes` |

地址对应各平台真实请求格式，不能在自建网关模式把官方地址拼成 `/v1/audio/speech`。此版本遇到这种混填会直接提示选择官方直连。

- 切换商业平台后会自动填地址，并读取该平台独立的加密密钥和音色设置。
- 切回自建网关会恢复保存的网关地址和网关密钥，不把平台 API Key 自动带到网关。
- 项目没有已部署的公共网关。自建网关域名由部署结果决定，不能通过模型名字自动推导。部署后填写一次，之后切换开源引擎即可自动复用。
- HF/Modal 的部署 Token 是平台访问凭据，自建网关的 `VOICE_GATEWAY_TOKEN` 是部署时设置的独立访问密钥，二者不能混用。
- 官方直连“检查”只验证地址和密钥格式，不提交付费合成，也不宣称密钥权限/额度已验证。网关模式可实际请求认证健康接口。

## 使用

1. 选择平台。Fish / ElevenLabs / MiniMax / Cartesia 的平台行会切换为官方直连；也可手动选择自建网关。
2. 点注册 / 申请账号，登录后点对应 API Key 页面，生成密钥并复制。
3. 回到应用，点粘贴密钥。选择该平台音色 ID；Fish 可改为已录制参考声音，录音原文须准确。
4. 保存并使用。直连地址已经自动填好；自建网关填写部署地址和网关 token 后保存一次即可。

ElevenLabs 直连使用 `eleven_multilingual_v2`，官方语速参数 0.7—1.2；超范围会明确提示调整。Fish 使用 `s2.1-pro-free`；MiniMax 使用 `speech-2.8-hd`；Cartesia 直连使用 `sonic-3`、WAV 输出，手机端语速设为 1.0×。GPT-SoVITS 必须选择 3—10 秒参考录音并填准确原声文本。平台费用、资格与限额由官方账号决定，不承诺免费额度。

## 验证范围

官方直连加入注入式请求/解码器测试，验证完整文本、认证头、固定地址、响应边界、Fish MessagePack WAV 与准确原文、分段合并、续传不重发，以及未知计费状态时停止。Host 使用模拟请求和解码器，没有真实账号/GPU或手机 codec 音质验收。

官方参考：[Fish API](https://docs.fish.audio/api-reference/endpoint/openapi-v1/text-to-speech)、[ElevenLabs Quickstart](https://elevenlabs.io/docs/eleven-api/quickstart)、[MiniMax HTTP Speech](https://platform.minimax.io/docs/api-reference/speech-t2a-http)。

**最新八款模型 + Fish 的全部连接方式和实际合成入口，见 [八引擎合成说明](EIGHT_ENGINES_SYNTHESIS.md)。**
