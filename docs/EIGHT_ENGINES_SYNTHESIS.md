# VoiceStudio 1.5.27.2 · 八款指定引擎接入与合成入口

保留原有 Fish Audio，因此 **总共九个引擎，每次明确选择一家**。手机客户端只提供录音、音色/接口管理、文字输入、合成队列、校验和作品；**APK 不包含本地模型**。本文件中“已接入”指 Android/网关的请求路由与模拟测试，不代表相关云 GPU 或付费额度已经提供。

## 八款指定引擎的真实连接方式

| 引擎 | Android 连接方式 | 音色克隆/请求约束 |
| --- | --- | --- |
| IndexTTS 2.5 | HTTPS VoiceStudio 网关 → 独立云 GPU worker | WAV 参考声音、对应文字（按模型需要） |
| CosyVoice 3 | HTTPS 网关 → 云 GPU worker | WAV 参考声音，完整输出段拼接 |
| Qwen3-TTS | HTTPS 网关 → 云 GPU worker | Base 模型、参考 WAV 和可选原文 |
| ElevenLabs | 官方 API 独立直连 | 必须先从 ElevenLabs 平台取得 Voice ID；需 API Key |
| MiniMax Speech | 官方 T2A API 独立直连 | 平台 Voice ID / API Key |
| Chatterbox V3 | HTTPS 网关 → 云 GPU worker | 参考 WAV，worker 内部进一步分句避免截断 |
| GPT-SoVITS | HTTPS 网关 → `server/gptsovits_bridge.py` → 官方本机 `api_v2.py /tts` | 3—10 秒 WAV + **准确参考原文**，原生服务不得公开暴露 |
| Cartesia Sonic | 官方 `POST https://api.cartesia.ai/tts/bytes` 独立直连 | 需 Cartesia API Key 和平台 Voice ID，官方 WAV 输出；Android 直连支持 0.6—1.5× 语速 |

加上原有 **Fish Audio**，可以使用已注册的 Voice ID，或在支持的线路中上传参考 WAV + 准确原文。不同商业平台的 Voice ID 不通用。

## 缺少的合成入口已修复

主导航固定显示 **音色 / 合成 / 任务 / 作品**，设置在右上角。

1. **音色**：选择具体平台，填写该平台自己的密钥与音色；开源模型还需配置自己的服务地址和参考 WAV。
2. **合成**：使用同一份草稿，输入全文（最多 12000 字），点击底部固定的 **开始合成**。默认费用保护仍生效，按钮会说明 API 授权或提供免费网页入口。
3. **任务**：查看分段进度、失败原因，按实际提交状态继续任务。
4. **作品**：试听、导出、收藏、保存与多段 WAV 合并。

“声音实验室”的合成标签也打开同一工作台，不再保留一份可能丢失或截短的独立草稿。批量入口保留在任务工作区。

## 账户、地址、密钥的步骤

- **第一步：选择引擎。** 在“音色”页选择一个明确的引擎。
- **第二步：选择连接类型。** Fish、ElevenLabs、MiniMax、Cartesia 可以用官方固定 HTTPS 地址；另外五款开源引擎必须提供**实际运行的**自建或受托云端网关地址，不能只填 Hugging Face Token。
- **第三步：选择声音。** 开源模型可使用本机保存的合法参考录音（3—10 秒）；GPT-SoVITS 需要录音原文。ElevenLabs / MiniMax / Cartesia 的 TTS 直连使用平台已注册的 Voice ID。
- **第四步：检测与保存。** 官方直连只读查询认证和音色（不触发合成），网关使用有鉴权的健康查询。查询成功不代表 TTS 权限、余额或实际生成已验收。
- **第五步：打开合成工作台**，输入任意文字、生成、检查段落/错误、试听/导出作品。未知是否收费的失败请求不会自动重交；超短/静音/不完整 WAV 拒绝发布。音频检查不等于逐字 ASR 验收。

## 服务端 GPT-SoVITS

在**云 GPU 机器**运行官方 [GPT-SoVITS api_v2.py](https://github.com/RVC-Boss/GPT-SoVITS/blob/main/api_v2.py)，仅监听 loopback `127.0.0.1:9880`，并运行：

```bash
export VOICE_GPTSOVITS_TOKEN='自己的强随机桥接密钥'
export GPT_SOVITS_NATIVE_URL='http://127.0.0.1:9880'
export GPT_SOVITS_SHARED_DIR='/tmp'
uvicorn server.gptsovits_bridge:app --host 127.0.0.1 --port 8771
```

桥接器和原生 GPT-SoVITS API 必须在同机、**同一共享文件系统命名空间**；这是因为官方 /tts 只接受服务器端 `ref_audio_path`。桥接器在临时目录安全落地录音，合成结束即清除；不接受客户端指定文件路径。将桥接服务放在有 TLS 的可信反代后，再在主网关设置：

```bash
export VOICE_GPTSOVITS_URL='https://你的桥接域名'
export VOICE_GPTSOVITS_TOKEN='同一桥接密钥'
```

不可将官方原生 /tts 或临时目录直接开放给互联网。未部署 GPU 的情况下，这种接口**不可直接合成**，会显示服务不可用，而不是返回假音频。

## 服务端 Cartesia

主网关设置 `CARTESIA_API_KEY`、可选 `CARTESIA_MODEL=sonic-3`；或手机官方直连自动用 Cartesia 地址。参见 [Cartesia TTS bytes](https://docs.cartesia.ai/api-reference/tts/bytes)。当前接法按 `2026-08-14` 版本、Bearer 鉴权和字符串 Voice ID 请求 `sonic-3`、标准 WAV/PCM16/24000，再做有效音频检查，前端保存由 Android Keystore 加密的 API Key。不包含自动创建 Cartesia Voice ID，也不保证免费额度。

## 费用与验证边界

- 用户希望手机独立工作，但目前**没有提供无需注册就可无限调用的免费云后台**。ZeroGPU 等免费演示属于浏览器试玩，可用额度和排队由提供者决定。
- Android APK 的模型入口不再安装本地权重。本地多段 WAV 合并只负责已有音频拼接，不是手机 TTS 推理。
- 网关的真实计费、GPU 稳定性、模型音色相似度和逐字朗读准确率，必须用真实凭据、经本人授权的参考录音进行端到端验证；无密钥的 Mock 和编译成功不能替代这些测试。
- 1.5.27.2 APK 已通过编译、原签名及升级身份检查；手机安装、播放和真实账号合成仍待验收。
