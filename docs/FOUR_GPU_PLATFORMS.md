# VoiceStudio · 四种云端 GPU 全路线（安卓手机、无本地模型）

> 状态：仓库已准备四个平台的代码和操作入口；**尚未替用户注册平台、取得凭据、购买/申请 GPU 或通过真实 GPU 合成验证**。其中 Modal/Lightning 和 HF ZeroGPU 支持计划中的 App 内合成；Kaggle Notebook 只能生成 WAV 再导入，不能冒充持久在线 API。

## ① Hugging Face ZeroGPU（每天少量免费 GPU）

**用途：** 发布自己的 Qwen3-TTS 声音克隆 Gradio Space，按免费额度排队；使用 VoiceStudio 的 Gradio 客户端直接生成。

1. 打开 https://huggingface.co/join 注册/登录并验证邮箱。官方当前规则：个人免费账号至少 30 天且状态良好，可免费托管最多 2 个 ZeroGPU Spaces；普通免费账号每日 GPU 额度约 5 分钟，额度与排队会变。
2. 打开 https://huggingface.co/new-space 新建一个 Gradio Space（如 `VoiceStudio-Qwen3`），在 Settings 的 Hardware 中选择 **ZeroGPU**。如果账号还没资格，不要强制使用收费 GPU。
3. GitHub 仓库 Settings → Secrets and variables → Actions → Secrets 新建 `HF_TOKEN`，值为可写该 Space 的 HF Token；Actions **Variables** 新建 `HF_SPACE_ID`，值为 `你的HF用户名/VoiceStudio-Qwen3`。Token 不要存进文件。
4. 手机打开 https://github.com/nmm26301-maker/Voicestudio/actions/workflows/deploy-hf-zerogpu.yml → Run workflow。它仅把 `deploy/hf_zerogpu` 中的 `README.md`、`app.py` 和 `requirements.txt` 上传到你已创建的 Space；不会替你启用付费 GPU。
5. Space Building/Running 后复制实际运行地址 `https://<owner>-<space>.hf.space`（不要填 `https://huggingface.co/spaces/...`）。
6. 在 VoiceStudio 音色页选择「**ZeroGPU · 配置手机直接合成**」，填运行地址及可选 HF Token，勾选上传参考录音、选择自己的 3—10 秒 WAV，保存。
7. 进入合成页输入短句、点击开始合成。Space 的兼容 API 名称为 `/_clone_fn`，若返回超时/配额/排队报错，查看 Space Logs，不能保证每次免费生成。

部署源码：[ZeroGPU Space](../deploy/hf_zerogpu/)。**ZeroGPU Space 代码是兼容性实现，不代表已在实际 ZeroGPU 机器上测试。** 此版本需参考录音且语速 1.0×。公共 Space 的上传音频可能在服务缓存中停留，不要上传敏感/未经授权的人声音频。

## ② Modal（每月计算赠额、按需 GPU API）

**用途：** 直接运行带 Bearer 鉴权的 `/v1/health`、`/v1/voices/reference`、`/v1/audio/speech`，Android 生成 WAV。

1. 到 https://modal.com/signup 注册。Starter 官方页面显示每月 **$30 计算赠额**，额度外为按量收费，不是“永久无限免费”。先在 Billing 设置 Workspace Budget 或支出提醒，避免误用。
2. 创建 Modal 账号部署 API token，在 GitHub Actions Secrets 中添加 `MODAL_TOKEN_ID` / `MODAL_TOKEN_SECRET`。
3. Modal Secrets 中创建名字完全一致的 `voicestudio-cloud`，只放变量 `VOICE_GATEWAY_TOKEN`（随机至少 24 位），这就是手机自建网关密钥。
4. 手机打开 https://github.com/nmm26301-maker/Voicestudio/actions/workflows/deploy-modal-qwen.yml → Run workflow。它读取开发分支的 `server/modal_qwen_deploy.py`，部署按需 L4 GPU。
5. 在 Modal Apps 找到实际生成的 HTTPS web endpoint，复制到 VoiceStudio → Qwen3-TTS → 我的云端网关，粘贴 `VOICE_GATEWAY_TOKEN`，录制参考音频，检测和合成。

更多细节：[Modal 手机部署](MODAL_PHONE_DEPLOY.md)。第一次可能需要下载模型，云端冷启动、费用、GPU 可用性均未实测。

## ③ Kaggle Notebook（免费 GPU、不能常驻后台）

**用途：** 一次运行生成 WAV，再通过 Android 文件导入 VoiceStudio；**不提供可长期访问的 HTTPS API**。

1. 登录 https://www.kaggle.com/code，新建或导入 [VoiceStudio-Qwen3-TTS.ipynb](../deploy/kaggle/VoiceStudio-Qwen3-TTS.ipynb)。
2. Settings → Accelerator → GPU，并启用 Internet；上传自己声音 WAV 到 Notebook 的 Input/Dataset。
3. 填参考录音的实际路径、准确原文与合成文字。Run All；完成后在 Output 下载 `VoiceStudio-Qwen3-TTS.wav`。
4. 在 VoiceStudio → 作品中导入下载的 WAV。

详见：[Kaggle 说明](../deploy/kaggle/README.md)。手机端云端运行，临时 Notebook 会休眠，配额与网络限制受 Kaggle 控制。

## ④ Lightning AI（注册送积分、可开启 Studio HTTPS）

**用途：** 使用 Lightning GPU Studio 装载 Qwen 模型，并通过平台 Ports 插件开放已鉴权的接口，手机直接合成。**免费 CPU Studio 不等于 GPU**。

1. 登录 https://lightning.ai/，核对免费 GPU credits、可用机型和试用限制：官方公布初始 5 credits，绑定支付方式可能追加 25 credits。账户验证或地域限制以控制台为准。
2. 在 Lightning GPU Studio 的网页终端运行：
   `git clone -b feature/cloud-voice-20261014 https://github.com/nmm26301-maker/Voicestudio.git`
3. Studio Secrets 添加 `VOICE_GATEWAY_TOKEN`（至少 24 位随机 ASCII），并让运行环境读取它。
4. `cd Voicestudio && bash deploy/lightning/run_qwen.sh`，验证实际 CUDA GPU 存在，再下载 qwen-tts。
5. 通过 Lightning **Ports** 插件公开 8000 端口，拿到 HTTPS 地址，在 VoiceStudio 选 Qwen3-TTS 自建网关，填地址和独立网关密钥，参考录音后测试。

说明：[Lightning Studio](../deploy/lightning/README.md)。Studio 停止、积分耗尽、端口失效后，VoiceStudio 不能继续调用。

## 平台与模型对应

| 模型 / API | ZeroGPU | Modal | Kaggle | Lightning |
| --- | --- | --- | --- | --- |
| Qwen3-TTS（先验证 0.6B Base；Modal 为 1.7B Base） | Gradio 声音克隆 | 已编写 REST 运行入口 | 已编写 Notebook | 已编写 REST 运行入口 |
| IndexTTS 2.5 | 需另适配 Space | 需单独 GPU worker | 可后续建立 Notebook | 需单独安装环境 |
| CosyVoice 3 | 需另适配 Space | 需单独 GPU worker | 可后续建立 Notebook | 需单独安装环境 |
| Chatterbox V3 | 需另适配 Space | 需单独 GPU worker | 可后续建立 Notebook | 需单独安装环境 |
| GPT-SoVITS | 需复杂部署，暂不支持一键 | 需原生 /tts + bridge | 可后续建立 Notebook | 需原生 API + 安全桥接 |
| Fish / ElevenLabs / MiniMax / Cartesia | **不需要自建 GPU** | **不需要自建 GPU** | **不需要自建 GPU** | **不需要自建 GPU** |

**切记：** “在 App 中有引擎选项”不代表云端已经有模型。一个 Qwen 实例不会自动提供另外四个模型。不要把 HF Token、Modal Deploy Token、商业 TTS API Key 错填为 VoiceStudio 网关 Token。密钥只进入各平台 Secret、GitHub Actions Secret 或安卓加密存储，绝不提交仓库明文。

## 安全与检查

- 先只用自己授权的参考音色和 10—30 字短句测试；先检查费用预算，避免重复提交可能扣费的请求。
- “地址检测成功”只是健康接口正常，不代表该云端 GPU 真能完成模型推理。
- 生成失败到 VoiceStudio 任务页看 HTTP 状态与错误详情，平台端查看模型安装与 GPU 日志。
- 只有成功获得真实音频、完整发音和无截断后，才算端到端验证成功。
- 保留手机上现有录音、作品与 App 签名；尚未确认正确签名的 APK 时不要卸载旧版。
