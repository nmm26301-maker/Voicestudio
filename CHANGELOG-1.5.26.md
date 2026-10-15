# VoiceStudio 1.5.26-cloud / versionCode 42

- 打包此前新增的 HF ZeroGPU、Modal、Kaggle、Lightning 四平台代码和手机入口；保留九引擎、直接合成、Cartesia 密钥作用域修复、MiniMax 预置音色 ID。
- Modal / Lightning 从只打开教程改为原生部署连接向导：真实 HTTPS 运行地址、网关密钥、参考录音、只读健康检测、保存后进入合成工作台。
- Modal 向导加入 Run workflow、运行结果、Actions Secrets 和 Secret 创建教程入口。可生成 / 复制新的 256 位随机网关密钥，明确区分账号部署凭据与 VOICE_GATEWAY_TOKEN；各平台密钥分开加密保存。
- HF ZeroGPU 的一键发布入口同步进开发分支；已有免费配额直接合成规则与用户选择的 Space 地址保留。
- 新增 ElevenLabs / Cartesia 音色 ID 的只读 GET 查询、分页和选中自动填写；不播放样例、不发起语音生成、不接受重定向。MiniMax 保留原生预置 ID。
- 选中使用平台 Voice ID 的商业引擎时关闭上次遗留的原始录音克隆选项，避免配置保存因前一引擎状态而失败。
- Modal 手动部署成功后，通过 SDK 查询实际 Web Endpoint 并写入 GitHub Summary，同时生成不含密钥的连接 JSON。不会启动真实 GPU 合成来冒充验收。
- 无本地模型。此轮没有操作用户平台账户、创建云 GPU 或发起付费合成；真实云端与真机仍需验收。
