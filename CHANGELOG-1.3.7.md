# VoiceStudio 1.3.7：恢复 Fish Audio，补充合成失败诊断

- 移除启动时把 Fish 引擎强制改为 OmniVoice 的逻辑。音色页可独立选择 Fish Audio、OmniVoice、手机语音。
- 恢复 Fish Audio API Key、官方音色 ID、收藏音色、模型配置与联网权限。复用原版 AndroidKeyStore 别名及 secure 偏好文件，可读取仍保留的旧密钥；读取失败可重新填写。
- Fish 按钮主动发起请求，使用后台队列、可暂停、保存 MP3、检查音频并导出。API Key 不写入任务与作品 JSON。未知模型配置在手机上拒绝，避免服务端自动切到其他模型；不自动转用收费模型或云端重试。
- Android 10—13 的普通音频任务显式使用非麦克风前台服务类型；Android 14+ 保留 specialUse，录音仍使用 microphone。任务启动失败保存错误类型。
- 已删除的参考音色在生成前切回自动音色，并显示提示。
- 工作台失败任务增加“错误详情”：显示处理阶段、异常类型/原因，可复制，或手动选择其他合成引擎。保留草稿、作品、模型和六类模型安装流程。

## 使用

覆盖安装 VoiceStudio-1.3.7-Hybrid.apk，避免卸载导致密钥、作品和模型丢失。

Fish Audio：设置 → 保存 Fish 配置 → 使用 Fish Audio → 选择/添加音色 → 创作 → 生成。原有密钥如果仍存在会自动读取；使用自己的账号额度，具体模型可用性由服务决定。没有 API Key 时会在发起请求前提示。

OmniVoice：仍需导入基础模型。若失败，工作台点击该任务 → 错误详情 → 复制详情，将内容或截图发来。短句建议先用“你好，欢迎来到声音工作室”测试。

## 已验证与边界

- APK 构建、v2/v3 签名、16 KiB 对齐、versionCode 11；签名证书与 1.3.6 一致。
- Fish 接口模拟测试 55 项断言：UTF-8 请求、模型/音色/参数校验、HTTP 状态、非音频/空数据拒绝、取消清理、MP3 头检查。不连接真实 API、不使用账号密钥。
- 模型与文件流程 285 项断言通过；现有音频测试套件通过。
- 未运行 Android 真机、真实 Fish API、完整 OmniVoice 大模型。本次尚未拿到用户手机的具体错误，不能认定原始合成失败的根因已定位或完全修复。

官方接口文档：https://docs.fish.audio/api-reference/endpoint/openapi-v1/text-to-speech
