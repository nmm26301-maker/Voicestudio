# VoiceStudio 1.5.27.2-cloud 当前验证

- 1.5.27 系列界面基线，未采用 1.5.28 样式。
- 本次重新编译的全部 DEX、manifest、资源和素材与交付签名 APK 逐字节一致。Android 编译、DEX、zipalign、原 v2/v3 签名通过，versionCode 46；从已交付 1.5.27.1 升级的 31 项兼容检查通过。
- 独立存储类测试 118 项：实际 CloudConnectionStore、任务构造与密钥选择，偏好存储和凭据仓库注入测试。覆盖多个平台配置、创建任务后切换其他平台、区域隔离、参考录音隔离、错误标识拒绝、仅同平台地址变更影响其旧任务、明确归属的旧配置迁移。没有使用真实 API Key。
- SecretStore 真实构造范围检查 26 项通过，不访问 Android Keystore；前次 API 专项 76 项及既有官方 API、恢复、音频、合并、持久化、全文管线回归通过。
- 137 项 UI 基础 / 对比度检查通过。没有执行真机布局、录音、播放、Keystore 加密读写或安装验收。
- 原签名恢复归档与 APK 字节一致，私钥未放入公开源码。

APK SHA256：`f0c4b0a0dff0307d167a5f2f057c4e6a7aa4ceee8bcb53b8531f04b4ba27af53`。

用户确认原失败出现在点击合成后，类型为 HTTP 401/403。当前没有具体平台的完整错误消息，也没有真实账号 TTS 验收。此次代码与测试证明连接 / 密钥 / 任务的隔离规则，不证明服务端认证一定已成功。无效 Key、权限、账号地区或限制仍需具体返回信息判断。

服务端与 GPU 部署代码未改动，未触发真实云部署或收费推理。五款自建模型仍需已运行的服务。

公开验证证据：isolation-java-1.5.27.2.log、upgrade-checks-1.5.27.2.json、source-match-1.5.27.2.json。

本次补充核验：source-match-1.5.27.2.json；隔离覆盖同一模型的 REST / Modal / Lightning 与匿名 Qwen Space。
