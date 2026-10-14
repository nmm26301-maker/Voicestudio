# VoiceStudio 1.5.21 云端语音

手机端只负责录音、保存作品和请求云端服务。APK 不包含本地推理原生库、ONNX 测试图、模型下载清单或安装资源；语言目录是文本元数据。旧版录音、参考 WAV、作品和任务目录继续沿用。旧模型文件由“工具 > 任务 > 清理旧离线模型”确认后删除。

## 七种引擎

| 引擎 | 连接方式 | 参考录音 |
| --- | --- | --- |
| Fish Audio | 原有 Android API 或网关调用官方 REST | Voice ID 或 WAV + 准确原文（MessagePack） |
| IndexTTS 2.5 | `server/open_model_worker.py` / `indextts.infer_v2_5.IndexTTS2` | WAV 录音克隆 |
| CosyVoice 3 | `server/open_model_worker.py` / `cosyvoice.cli.cosyvoice.AutoModel` | WAV 与可选参考文字 |
| Qwen3-TTS | `server/open_model_worker.py` / `Qwen3TTSModel` Base | WAV 与可选参考文字 |
| Chatterbox V3 | `server/open_model_worker.py` / 多语言 V3 权重 | WAV 录音克隆 |
| ElevenLabs | 网关调用官方 REST | 平台已注册 Voice ID |
| MiniMax | 网关调用官方 REST | 平台已注册 Voice ID |

界面“已配置”表示网关填有该引擎配置，不表示已验收音质或有免费额度。Fish 网关支持直接传 WAV/原文进行零样本克隆，不自动创建平台模型。四款开源模型有实际 Python 适配代码，但必须在服务器安装对应官方环境、权重并取得 GPU。商业服务须自己的 API Key 和音色 ID。此次交付没有云账号密钥/GPU 资源，未运行七引擎真实合成验收。

## 启动网关

需要 Python 3.11+、FFmpeg 和 HTTPS。

```bash
python -m pip install -r server/cloud_requirements.txt
export VOICE_GATEWAY_TOKEN='你的随机网关密钥'
export VOICE_REFERENCE_DIR='/安全持久化目录/references'
# 填你自己部署的服务根地址（不包含 /v1）：
export VOICE_QWEN3TTS_URL='https://你的推理服务域名'
export VOICE_QWEN3TTS_TOKEN='独立推理服务密钥'
uvicorn server.cloud_gateway:app --host 127.0.0.1 --port 8765
```

其它引擎分别使用 `VOICE_COSYVOICE3_URL/TOKEN`、`VOICE_INDEXTTS25_URL/TOKEN`、`VOICE_CHATTERBOXV3_URL/TOKEN`，以及 `FISH_API_KEY`、`ELEVENLABS_API_KEY`、`MINIMAX_API_KEY`。不要把实际密钥提交到 Git。

Docker HTTPS 网关：复制 `server/.env.cloud.example` 为 `server/.env.cloud`，填写域名、随机 token 和服务地址，执行：

```bash
cd server
docker compose --env-file .env.cloud up -d --build
```

网关镜像只安装网关和 FFmpeg，不会下载四个模型。Caddy 自动取得域名 TLS 证书，域名需已指向该服务器。`/v1/health`、参考上传和合成均须 Bearer token。

## 启动开源推理服务

**每个模型使用独立的官方 Python/CUDA 环境**，不要把四套冲突依赖装在同一环境。先按其官方仓库安装、下载正确模型，并运行官方示例验证 GPU 可用，再安装 `server/cloud_requirements.txt` 和启动本仓库的包装器。依赖/权重初始化失败会返回明确失败，不返回占位音频。

| 引擎 | 官方安装与权重 | 关键配置 |
| --- | --- | --- |
| Qwen3-TTS | [QwenLM/Qwen3-TTS](https://github.com/QwenLM/Qwen3-TTS)；`Qwen/Qwen3-TTS-12Hz-1.7B-Base` | `VOICE_WORKER_ENGINE=qwen3tts`；Base 才能克隆 |
| CosyVoice 3 | [FunAudioLLM/CosyVoice](https://github.com/FunAudioLLM/CosyVoice)；`Fun-CosyVoice3-0.5B` | `VOICE_WORKER_ENGINE=cosyvoice3`；`VOICE_MODEL_DIR` 为该权重路径，启动目录须可导入官方代码及 Matcha-TTS |
| IndexTTS 2.5 | [index-tts/index-tts](https://github.com/index-tts/index-tts)；`IndexTeam/IndexTTS-2.5` | `VOICE_WORKER_ENGINE=indextts25`；`VOICE_MODEL_DIR` 内含 config.yaml |
| Chatterbox V3 | [resemble-ai/chatterbox](https://github.com/resemble-ai/chatterbox)；`t3_mtl23ls_v3.safetensors` | `VOICE_WORKER_ENGINE=chatterboxv3`；权重下载需要时配置 HF_TOKEN |

以 Qwen 为例，在官方模型环境中、从本仓库根目录启动：

```bash
export VOICE_WORKER_ENGINE=qwen3tts
export VOICE_WORKER_TOKEN='与网关上游 TOKEN 相同的随机密钥'
export VOICE_DEVICE=cuda:0
uvicorn server.open_model_worker:app --host 127.0.0.1 --port 8770 --workers 1
```

为 worker 配置 HTTPS 反代，或同机网关设 `VOICE_QWEN3TTS_URL=http://127.0.0.1:8770`。容器间请使用 HTTPS 域名；网关只允许 HTTPS 或同机 loopback 上游。手机只填写网关地址与网关 token，模型平台密钥留在服务器。

如果不使用手机录音，worker 需 `VOICE_DEFAULT_REFERENCE` 指向服务器固定的 3—10 秒单声道 PCM16 WAV；可设置 `voice=auto/default`。不接受客户端传任意服务器文件路径。Qwen/Chatterbox 有自身语言范围，无法支持语言时明确失败；手机目录提供的语言不是所有引擎都支持。

## 生成与故障处理

- 在“音色”页选择云端引擎、填写网关、检测连接，勾选“用已录制的参考声音克隆”并选择人物录音，再保存。
- 参考文字只描述录音原文，新朗读文本独立传给模型。CosyVoice 收集并拼接全部返回段，避免只保存开头一段；Chatterbox 在 worker 内继续按最多 60 字分段，减小官方单次生成 token 上限导致截断的风险。
- 手机对 1—12000 字的单次文本无损分段，保留换行和末句；完成所有段后才发布作品。长文可分批提交。
- 自动选择在提交前并行 GET 检查开源 worker，按 Qwen、CosyVoice、IndexTTS、Chatterbox、Fish、MiniMax、ElevenLabs 的优先级选择适合的已配置服务；Fish 可使用已上传的录音与准确原文；其它商业服务需要该平台有效的 Voice ID。不同商业平台 Voice ID 不能混用，建议手动选平台。
- 一旦 POST 合成已提交，超时不自动换引擎/重发。已完成段留存，未知状态段重提交需要用户明确确认，以避免重复计费。自动选择可能在不同分段选到不同开源引擎；追求音色一致时手动固定引擎。
- 网关检查 PCM16、实际帧长度、静音、时长与短输出，拒绝空音频/假 WAV/截断录音。时长阈值不能保证每个字都被正确读出，真实音质仍需实测。
- 参考录音仅在主动生成时上传网关，默认 72 小时过期，后续上传会清理过期 WAV/文本。更强的保留/删除策略由服务器运维配置。
- 旧版离线转写、视频人物分解与翻译的历史结果可查看，本次云端版没有新的云视频分人/翻译服务，不会偷偷调用本地模型。这些旧任务不能继续离线推理。

## 验证

```bash
python -m pytest -q server/test_cloud_gateway.py server/test_open_model_worker.py
ECJ_JAR=/tmp/ecj.jar JSON_JAR=/tmp/json.jar bash tests/run_accelerated.sh
```

以上使用模拟模型/请求，验证数据完整性、无重复提交、参考录音身份、自动选择和错误处理；不证明云端音质、速度、逐字朗读或免费额度。APK 签名覆盖兼容与 Android 页面检查另见 `validation/current-checks.md`。
