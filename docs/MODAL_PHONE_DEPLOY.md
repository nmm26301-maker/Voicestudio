# VoiceStudio · 只用安卓手机部署 Qwen3-TTS 云端 GPU（试验版）

**本教程不要求电脑、本机 CUDA 或手机下载模型。** 模型和 Python 包由 Modal 在云端 GPU 上安装。部署可以通过 GitHub Actions 在服务器上执行。部署所需的账户授权必须由本人完成，且每次 GPU 使用可能收费。

## 部署到哪里 / 对应哪些模型

本仓库提供的首个可执行部署入口是 `server/modal_qwen_deploy.py`，目前**只部署 Qwen3-TTS**。部署后的 HTTPS 网关兼容 Android 客户端的三个接口：

- `GET /v1/health`：显示 qwen3tts 配置状态（不代表合成验证成功）。
- `POST /v1/voices/reference`：上传 3—10 秒 PCM16 单声道 WAV 和参考原文，短期存在云端持久 Volume 中。
- `POST /v1/audio/speech`：把文字和临时参考音色 ID 发给云端 GPU Qwen3-TTS；返回 WAV，而不是打开浏览器。

IndexTTS 2.5、CosyVoice 3、Chatterbox V3 和 GPT-SoVITS 仍需要**另外的** GPU worker/模型环境，不会因为这次部署自动可用。MiniMax/ElevenLabs/Fish/Cartesia 使用各自独立官方 API，不需要这个 GPU。

## 只用手机完成四步

### 第一步：注册 Modal，先设预算

1. 在安卓 Chrome 打开 https://modal.com/signup 并登录。
2. 在 Modal Dashboard 的 **Billing / Spending limits 或 Budgets** 查看可用的免费计算额度、是否需要绑定支付方式，并设置可承受的支出上限。
3. Modal Starter 的赠送计算额度及具体计费规则以 https://modal.com/pricing 为准；赠额不是无限免费。

### 第二步：在 Modal 创建两种不同的凭据

A. **部署凭据**：在 Modal 控制台的 workspace/settings 的 API Tokens 页面创建 Modal API Token。会得到 `MODAL_TOKEN_ID` 和 `MODAL_TOKEN_SECRET`。这两个只交给 GitHub Actions 用于部署，不填在 VoiceStudio 里。

B. **VoiceStudio 自建网关密钥**：在 Modal 的 Secrets 页面创建名为 `voicestudio-cloud` 的 Secret（注意名称完全一致），添加一个变量：

- Key：`VOICE_GATEWAY_TOKEN`
- Value：用户自行生成的**至少 24 位、无空格**的随机 ASCII 字符串（建议 32—64 位）。

**该值要保存好，因为稍后会作为 VoiceStudio 的“自建网关访问密钥”。** 不要放进仓库源代码、README、普通 GitHub Variables、截图或聊天消息。

Modal Web Function 由 VoiceStudio 自带的 Bearer 网关密钥鉴权。这个部署示例尚未提供多人账号隔离、外部配额管理或防滥用后台。

### 第三步：在 GitHub 手机网页手动部署

1. 打开 https://github.com/nmm26301-maker/Voicestudio 。手动部署工作流已经加入默认 `main` 分支，GitHub Actions 列表可以直接找到；它会在运行时自动检出 `feature/cloud-voice-20261014` 的部署代码。
2. 打开仓库 **Settings → Secrets and variables → Actions → New repository secret**，分别添加：
   - `MODAL_TOKEN_ID` = 上一步创建的 Modal Token ID。
   - `MODAL_TOKEN_SECRET` = 对应 Modal Token Secret。
3. 在仓库 **Actions** 标签选择 **Deploy VoiceStudio Qwen3-TTS to Modal**，点击 **Run workflow**，保持默认 `main` 分支后确认（实际部署代码会从开发分支读取）。
4. 等待工作流部署完成。执行时 GitHub Actions 只安装 Modal 部署客户端，真正的 qwen-tts 和模型下载、推理发生在 Modal 云端。
5. 部署成功后，在本次 GitHub 运行的 **Summary / 摘要** 查看“VoiceStudio · 实际部署地址”，直接复制 URL。它由 Modal SDK 查询已经部署的 `voice_api` 得到，不是根据账号拼接的假地址。Artifacts 提供不含密钥的 `VoiceStudio-Modal-connection` JSON，App 地址框也接受该 JSON。
6. 也可打开 Modal Dashboard → Apps → `voicestudio-qwen3tts` → `voice_api`，复制平台显示的 HTTPS **Web Endpoint URL**。

找不到部署工作流时，请在浏览器切到完整桌面版网站并检查仓库 Actions 是否启用。首次部署也可能失败，因为 Modal Secret 尚未创建、账户权限或付费设置限制、GPU 不可用、模型依赖安装失败。

### 第四步：回到 VoiceStudio 填入云端地址

1. 打开 **音色 → 九引擎**，选 **Qwen3-TTS**。
2. 连接方式选 **我的云端网关 · 开源 / 自动**（不是“官方 API 直连”）。
3. **HTTPS 网关地址**：粘贴 Modal Dashboard 提供的 Web Endpoint URL，**不要**自行加上 `/v1/audio/speech`；App 会自动拼接。
4. **自建网关访问密钥**：粘贴上面 Modal Secret 的 `VOICE_GATEWAY_TOKEN` 原始值，不是 Modal API Token，也不是 Hugging Face Token。
5. 录制 3—10 秒自己的声音并保存参考音色；勾选“使用已录制的参考声音克隆”，选择它。文字合成需要在“合成”标签输入。
6. 点 **检查地址 / 检测网关连接**，看到 `qwen3tts` 已配置再点 **保存并使用**。
7. 如费用保护开启，先阅读并主动确认 API 可能计费，再到 **合成 → 输入短句 → 开始合成**。成功后应能在“任务”和“作品”中查看 WAV。

**关键区别**：Modal 的网站 URL（例如 Dashboard 页）不等于你部署出来的 HTTPS Web Endpoint URL；Hugging Face Token 也不等于 VoiceStudio 网关密钥。

## 验证边界、费用、已知约束

- GitHub 静态语法检查和 Modal `Deploy succeeded` 只能说明软件已发布，不代表实际 GPU 加载或录音克隆成功。**首次实际生成**会下载模型，可能有较长的冷启动；长时间模型下载可能触发 Android 超时，未知提交状态不会自动重试；仍需真实 GPU 端到端验证。
- 为了降低静置成本，后台允许自动缩容至 0；下一次请求会有冷启动。部署时 L4 GPU 可否申请成功由 Modal 区域/账户限制决定。
- `/v1/health` 只验证服务和密钥、并显示模型路由，**不**触发可能计费的语音生成，`model_loaded` 可帮助诊断是否已经加载过模型。
- 云端参考录音按网关 3 天过期设置管理，持久 Volume 可能单独产生存储费用。未经授权不要上传他人声音。停止 App 不一定自动清除 Volume；可以在 Modal Dashboard 管理云端资源。
- 这个入口仅公开 **Qwen3-TTS**，因此选择 Chatterbox、CosyVoice、IndexTTS 或 GPT-SoVITS 时仍会提示未部署。需要分别安装并启用它们，不能把单个 Qwen 地址当成五模型地址。
- 不会在 GitHub 保存任何 Modal Token 或 VoiceStudio 私有网关密钥。提交到 GitHub 的仅是部署代码、工作流和说明文档。

参考： [Modal Pricing](https://modal.com/pricing) / [Modal GitHub Actions deployment](https://modal.com/docs/guide/useful-snippets) / [Modal Secret](https://modal.com/docs/sdk/py/latest/Secret) / [GitHub Actions repository secrets](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets)。

## 1.5.26 手机连接向导

音色页的 Modal 卡片现在直接提供部署入口、Actions Secrets 设置、密钥名称复制、生成 / 复制 256 位随机网关密钥、检测连接及参考音色选择。生成密钥只发生在本机，不会自动修改 Modal Secret；平台和 App 必须使用同一值。Lightning 有独立连接表单和密钥存储。

HF ZeroGPU 卡片提供默认分支可见的一键发布入口及自己的 Space 连接表单。Kaggle 保持 Notebook 导出 / WAV 导入路径，不宣称常驻 API。
