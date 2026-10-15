# 五引擎独立云端部署 · 1.5.27

手机只负责录音、任务和作品。下列脚本只在 Modal 或 Lightning 的远程 Linux GPU 环境安装依赖和模型，不要在手机本地运行。本文提供可检查的部署配方；尚未用实际账户构建这些镜像、启动 GPU 或验收音质。

## 引擎对应关系

| App 引擎 | 部署 engine 值 | 独立 Python | 预训练模型 / 调用 |
|---|---|---|---|
| Qwen3-TTS | `qwen3tts` | 3.12 | Qwen/Qwen3-TTS-12Hz-0.6B-Base |
| IndexTTS 2.5 | `indextts25` | 3.11 | IndexTeam/IndexTTS-2.5，infer_v2_5.IndexTTS2 |
| CosyVoice 3 | `cosyvoice3` | 3.10 | FunAudioLLM/Fun-CosyVoice3-0.5B-2512，AutoModel |
| Chatterbox V3 | `chatterboxv3` | 3.11 | ResembleAI/chatterbox，t3_model="v3" |
| GPT-SoVITS | `gptsovits` | 3.10 | 官方 GPT-SoVITS V2 预训练基线，api_v2.py + 私有桥接 |

各引擎独立环境，不能把 IndexTTS 的 torch 2.8、CosyVoice 的 torch 2.3.1、Chatterbox 的 torch 2.6 放到同一个环境。`server/engine_recipes.py` 固定四款引擎的官方源码提交；模型仓库和间接依赖仍遵循上游更新，并非完全锁定的镜像。不要把“提供配方”视为云端构建已通过。

## Modal：手机网页手动选择引擎

1. 按 [Modal 手机部署](MODAL_PHONE_DEPLOY.md) 创建 `voicestudio-cloud` Secret，内容为 `VOICE_GATEWAY_TOKEN`（24 位以上），并在 GitHub Actions 设置 `MODAL_TOKEN_ID`、`MODAL_TOKEN_SECRET`。
2. 在 [Deploy VoiceStudio selected engine to Modal](https://github.com/nmm26301-maker/Voicestudio/actions/workflows/deploy-modal-engine.yml) 选择 `main`，点击 Run workflow，在 `engine` 选择上表值。一次只部署一个模型环境。
3. 工作流在远程构建依赖并发布 `voicestudio-<engine>` / `voice_api`。构建失败时查看该次日志；不得当成部署成功。
4. 成功后 Summary 显示实际 `*.modal.run` URL，Artifacts 中的连接 JSON 包含正确的 `engine`，不包含任何密钥。查询地址不提交语音生成。
5. 回到 App 的音色 → Modal，选择相同引擎，粘贴实际 URL 或 JSON、网关密钥，选择参考录音并检测连接，再保存。

每个引擎单独的模型缓存与录音 Volume，L4、最多 1 个容器、空闲缩到 0。GPU 运行及持久存储可能收费；健康请求虽不加载模型或合成，也可能唤醒带 GPU 的容器。旧的 Qwen 专用工作流仍保留兼容。

## Lightning：云端 Studio 终端

在已经开启 GPU 的远程 Linux Studio 中操作。需要 git、ffmpeg、libsndfile、libsox、编译工具、cmake、pkg-config 与 uv；缺少时在 Studio 安装系统依赖。不要在安卓终端安装这些软件。

```bash
# 在 Lightning 云端终端，克隆开发分支
 git clone --branch feature/cloud-voice-20261014 https://github.com/nmm26301-maker/Voicestudio.git
 cd Voicestudio
 python -m pip install uv
# 在 Studio 的 Secrets / 环境变量中设置 VOICE_GATEWAY_TOKEN；不要提交到源码。
 python tools/run_cloud_engine.py indextts25
```

把最后的 engine 换成表中任意一个。启动器为每个引擎创建 `.cloud-envs/<engine>` 下独立 Python 环境，核对 GPU 后启动 8000 端口，单进程运行。通过 Lightning Ports 暴露该端口的 HTTPS 地址；Studio 必须保持运行。App → 音色 → Lightning 中选择同一引擎并填写真实运行地址。

依赖安装中断时，启动器不会自动覆盖残留目录：先查看错误，再在云端重命名对应引擎目录并重试。不要删除其他引擎、录音或作品。云端参考录音按网关 TTL 过期，不应作为作品备份。

## 参考录音、长文本和 GPT 桥接

所有模式先上传 3—10 秒 PCM16 单声道 WAV，得到服务器临时 reference ID，客户端不能传服务器文件路径。GPT-SoVITS 必须填写准确的参考原文；新文字和录音原文分别传输。

GPT 仅在首次有效合成时下载官方 V2 需要的权重、G2PW 与语言数据，然后在同一容器里启动 `api_v2.py -a 127.0.0.1 -p 9880`。本机 `/tts` 不对外暴露，外部统一使用鉴权后的 `/v1/audio/speech`。本版本没有宣称 GPT 的最新 Pro/V5 权重已经适配。

长文本仍由 App 完整分段（每段最多 180 字符）并合并 WAV；服务端超长请求会明确拒绝，不会静默截断。CosyVoice 会保留全部输出块，Chatterbox 内部再次分段。首次权重下载、模型加载可能超过手机超时；不要在提交状态未知时反复生成或切换引擎，先查任务和云端日志。

## 四个平台的实际覆盖

| 平台 | 本次提供的入口 | 仍需验证 |
|---|---|---|
| Modal | 五种引擎各自独立的部署配方和连接 JSON | 远程依赖构建、GPU 加载、合成和计费 |
| Lightning | 五种引擎各自独立的环境启动器 | Studio 系统依赖、端口权限、GPU 合成 |
| HF ZeroGPU | 既有 Qwen Gradio Space 模板 | 自己的 Space、额度与 GPU 权限 |
| Kaggle | 既有 Qwen Notebook 导出 / WAV 导入 | Notebook GPU、实际生成；不是常驻 API |

HF/Kaggle 当前模板不声称支持五模型任选，也不声称全部平台永久免费。Fish、ElevenLabs、MiniMax、Cartesia 继续使用官方 API 直连。

## 官方核对入口

- [IndexTTS](https://github.com/index-tts/index-tts)，源码提交见 engine_recipes.py。
- [CosyVoice](https://github.com/QwenAudio/CosyVoice)，含 Matcha-TTS 子模块。
- [Chatterbox](https://github.com/resemble-ai/chatterbox)，使用多语言模型 V3。
- [GPT-SoVITS](https://github.com/RVC-Boss/GPT-SoVITS)，V2 预训练模型及 api_v2.py。
