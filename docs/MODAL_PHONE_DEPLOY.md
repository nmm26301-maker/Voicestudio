# VoiceStudio · 只用安卓手机部署云端 GPU · 1.5.27

无需电脑或手机本地模型。通过手机浏览器手动部署，模型在 Modal GPU 容器运行。部署配方尚未完成真实 GPU 验收；GPU 与模型缓存可能收费，先在 Modal 查看账户权限、预算和额度。

1. 登录 [Modal](https://modal.com/apps)，创建 Secret，名称严格为 `voicestudio-cloud`。其中新增变量 `VOICE_GATEWAY_TOKEN`，值使用 App → 音色 → Modal 生成的随机网关密钥（至少 24 位）。复制后在两边保存同一值，不发进公开聊天。
2. 在 Modal 账户创建部署 Token，取得 `MODAL_TOKEN_ID`、`MODAL_TOKEN_SECRET`。在 [GitHub Actions Secrets](https://github.com/nmm26301-maker/Voicestudio/settings/secrets/actions) 分别保存这两个仓库 Secret。它们用于部署，不填入 App。
3. 打开 [五引擎手动工作流](https://github.com/nmm26301-maker/Voicestudio/actions/workflows/deploy-modal-engine.yml)，选择 main → Run workflow → engine，选择 qwen3tts / indextts25 / cosyvoice3 / chatterboxv3 / gptsovits 中的一项。每款模型独立部署，不会在一次操作中自动启动五个服务。
4. 等待该次运行成功。远程依赖构建失败时先查看日志；成功发布不代表模型已经加载或音质验收完成。
5. 在运行的 Summary 复制真实 Web Endpoint，也可下载 Artifacts 中的连接 JSON。地址由 Modal SDK 查询，不自行拼接。也可在 Modal Apps → `voicestudio-<engine>` → `voice_api` 查看 HTTPS Web Endpoint。
6. App → 音色 → Modal，选择**相同引擎**，粘贴 URL 或 JSON、`VOICE_GATEWAY_TOKEN`，选择录好的 3—10 秒参考音色，检测连接后保存。GPT-SoVITS 还需要准确的录音原文。检测只核对服务、密钥与配置，不证明音质。
7. 在合成页输入短句，确认费用保护后手动开始。检查任务状态、完整 WAV 和实际听感后，再进行长文本测试。手机超时或提交状态未知时不会自动重试，先查看云端日志。

实际运行地址应为 `https://...modal.run`，不是 modal.com 控制台。不要把 HF Token、Modal 部署 Token 或 GitHub Token 当作网关密钥。

健康请求不提交合成或下载模型，但可能唤醒 GPU 容器并产生运行成本。参考 WAV 持久保存在该引擎的 Volume，按网关三天 TTL 管理，缓存与录音存储也可能收费。旧 Qwen 专用工作流继续存在，原来的地址仍能使用。

更多模型依赖、Lightning 启动器、四个平台覆盖范围与验证限制，见 [五引擎云端部署](OPEN_MODEL_DEPLOY.md)。HF ZeroGPU 与 Kaggle 目前仍是 Qwen 模板。
