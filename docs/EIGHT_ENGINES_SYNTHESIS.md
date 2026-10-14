# VoiceStudio 1.5.24 · 八款指定引擎接入与合成入口

保留原有 Fish Audio，因此 **总共九个引擎 + 自动选择**。手机客户端只提供录音、音色/接口管理、文字输入、合成队列、校验和作品；**APK 不包含本地模型**。本文件中“已接入”指 Android/网关的请求路由与模拟测试，不代表相关云 GPU 或付费额度已经提供。

## 八款指定引擎的真实连接方式

| 引擎 | Android 连接方式 | 音色克隆/请求约束 |
| --- | --- | --- |
| IndexTTS 2.5 | HTTPS VoiceStudio 网关 → 独立云 GPU worker | WAV 参考声音、对应文字（按模型需要） |
| CosyVoice 3 | HTTPS 网关 → 云 GPU worker | WAV 参考声音，完整输出段拼接 |
| Qwen3-TTS | HTTPS 网关 → 云 GPU worker | Base 模型、参考 WAV 和可选原文 |
| ElevenLabs | 官方 API 直连或网关 | 必须先从 ElevenLabs 平台取得 Voice ID；需 API Key |
| MiniMax Speech | 官方 T2A API 直连或网关 | 平台 Voice ID / API Key |
| Chatterbox V3 | HTTPS 网关 → 云 GPU worker | 参考 WAV，worker 内部进一步分句避免截断 |
| GPT-SoVITS | HTTPS 网关 → `server/gptsovits_bridge.py` → 官方本机 `api_v2.py /tts` | 3—10 秒 WAV + **准确参考原文**，原生服务不得公开暴露 |
| Cartesia Sonic | 官方 `POST https://api.cartesia.ai/tts/bytes` 或 HTTPS 网关 | 需 Cartesia API Key 和平台 Voice ID，官方 WAV 输出；Android 直连 1.0× 语速 |

加上原有 **Fish Audio**，可以使用已注册的 Voice ID，或在支持的线路中上传参考 WAV + 准确原文。不同商业平台的 Voice ID 不通用。

## 缺少的合成入口已修复

现在有两种完整操作路径：

1. **创作首页** → “云端文字合成工作台” → “选择引擎 / 配置音色” → 输入全文 → 页面底部 **生成声音**（默认费用保护开启时显示免费云端试玩入口）→ 任务队列与作品
2. **工具与任务** → “音色”录制/选择参考声音 → **“合成”标签** → 选择九引擎及参考音色 → 输入文字（最多 12000 字）→ **“开始云端语音合成”** → “任务”查看进度和故障说明

另可从音色页点击 **“文字合成工作台”** 直接进入，不再只有“保存并使用”。批量任务仍有独立入口。

## 账户、地址、密钥的步骤

- **第一步：选择引擎。** 在“音色”页选择一个明确的引擎或者“自动选择”。
- **第二步：选择连接类型。** Fish、ElevenLabs、MiniMax、Cartesia 可以用官方固定 HTTPS 地址；另外五款开源引擎必须提供**实际运行的**自建或受托云端网关地址，不能只填 Hugging Face Token。
- **第三步：选择声音。** 开源模型可使用本机保存的合法参考录音（3—10 秒）；GPT-SoVITS 需要录音原文。ElevenLabs / MiniMax / Cartesia 的 TTS 直连使用平台已注册的 Voice ID。
- **第四步：检测与保存。** 官方直连只做格式检查（不触发付费合成），网关使用有鉴权的健康查询。标注“已配置”不是验证账户可用或生成成功。
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

主网关设置 `CARTESIA_API_KEY`、可选 `CARTESIA_MODEL=sonic-3`；或手机官方直连自动用 Cartesia 地址。参见 [Cartesia TTS bytes](https://docs.cartesia.ai/api-reference/tts/bytes)。当前接法请求 `sonic-3`、标准 WAV/PCM16/24000，再做有效音频检查，前端保存由 Android Keystore 加密的 API Key。不包含自动创建 Cartesia Voice ID，也不保证免费额度。

## 费用与验证边界

- 用户希望手机独立工作，但目前**没有提供无需注册就可无限调用的免费云后台**。ZeroGPU 等免费演示属于浏览器试玩，可用额度和排队由提供者决定。
- Android APK 的模型入口不再安装本地权重。本地多段 WAV 合并只负责已有音频拼接，不是手机 TTS 推理。
- 网关的真实计费、GPU 稳定性、模型音色相似度和逐字朗读准确率，必须用真实凭据、经本人授权的参考录音进行端到端验证；无密钥的 Mock 和编译成功不能替代这些测试。
- 变更保留在开发 PR 中，正式升级 APK 需要已交付旧包的相同签名并在真实手机验收。
