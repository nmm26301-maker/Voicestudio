# VoiceStudio 1.5.1

Android ARM64 声音工作室：文字合成、音色克隆、录音转写、字幕配音、翻译、人声分离与有声书。最低 Android 8.0（API 26），当前源码版本 `1.5.1-hybrid`，`versionCode 17`。

本次整理功能入口并修复任务、文件和配音流程。完整编译与宿主回归已通过；**尚未完成 Android 真机/模拟器、完整模型和真实 Fish 账户验收**。本仓库不包含可直接覆盖安装的 1.5.1 签名 APK，见下方构建说明。

## 功能在哪里

| 主入口 | 内容 |
| --- | --- |
| 创作 | 文本、草稿、语言/音色、语速、16/32 步、生成进度、取消、继续和最近作品 |
| 音色 | OmniVoice、Fish Audio、手机离线音色切换；参考音色管理入口 |
| 工具 | 按用途切换八个分类，顶部分类栏固定；每个分类展示对应工具 |
| 作品 | 搜索、排序、收藏、播放、波形、重命名、保存、分享、回收站 |
| 设置 | Fish 密钥与模型、动效、模型安装管理、回收站和应用说明 |

| 工具分类 | 工作流程 | 需要的模型/配置 |
| --- | --- | --- |
| 转写 | 导入音视频/录音/悬浮听写 → 任务结果 → 字幕校对与导出 | Whisper；麦克风/悬浮窗权限按需授权 |
| 音色 | 参考音频 → 创建音色 → 选择音色 → 创作 | 克隆模型；合成另需 OmniVoice；参考文字留空另需 Whisper |
| 翻译 | 输入文字 → 翻译任务 → 复制、保存或配音 | NLLB；合成另需 OmniVoice |
| 音频 | 分离人声/伴奏；从已完成转写任务进入说话人分组与字幕配音 | 分离用 MDX-Net；分组用 CAM++；配音用 OmniVoice，翻译另需 NLLB |
| 有声书 | UTF-8 TXT/未加密 EPUB → 分章合成 → ZIP 导出 | OmniVoice |
| 批量 | 每行一个任务 → 独立合成结果 | OmniVoice，使用本机语言/语速/参考音色配置 |
| 任务 | 筛选、暂停、继续、失败详情、结果和删除 | 根据任务类型；新录音必须单独开始 |
| 模型 | 六类模型的下载、安装、完整性检查、移除和设备诊断 | 模型包需另行下载 |

批量、有声书、字幕配音当前使用 OmniVoice，不会因首页选择 Fish 而自动发送云端。Fish 只用于明确选择的 Fish 生成任务。

## 本次修复

- 录音任务恢复重新处理麦克风授权；新录音不能排在其他运行任务后面；已保存的录音可随暂停队列继续识别。
- 任务入队、恢复与模型维护共用互斥检查；恢复清除旧的起止时间和错误状态。
- 字幕配音分块读取完整生成片段，修复只取前 60 秒的问题；过长片段报错提示拆分，保留原有时长匹配约束。
- 批量、克隆、声音参数和翻译表单验证失败保留输入；批量已提交的行从输入框移除。
- 文件导出使用截断写入，补上空返回和缺少文件选择器处理；MP3 导出状态支持页面重建恢复。
- 模型安装返回后刷新状态；“使用模型”进入对应工具或选择 OmniVoice；下载链接保留原目标地址。
- 主播放器在耳机断开时暂停；页面重建不再重复用旧分享文本覆盖已修改草稿。
- 缺少原签名时停止发行构建，不再自动生成无法覆盖旧安装的新密钥。

详见 [更新记录](CHANGELOG-1.5.1.md)、[功能核对与真机验收](docs/FUNCTION_AUDIT_1.5.1.md)。

## 构建与检查

需要 Java 17、Android SDK platform 35 / build-tools 35.0.0、ECJ 3.38+；宿主回归另外需要 `org.json` JVM JAR。本次使用 ECJ 3.38.0、org.json 20240303。

```bash
export ANDROID_SDK_ROOT=/path/to/android-sdk
export ECJ_JAR=/path/to/ecj.jar
export JSON_JAR=/path/to/json.jar
python3 tools/fetch_runtime.py
bash tests/run_regression.sh
VOICE_UNSIGNED=1 bash tools/build.sh
```

`VOICE_UNSIGNED=1` 仅生成编译验证用的未签名 APK，不能直接安装或覆盖升级。

要构建原安装的更新包，必须恢复原 `signing/voicestudio-development.p12` 后运行 `bash tools/build.sh`。**不要用新密钥替代，也不要卸载旧应用来绕过签名不一致**；卸载可能删除内部作品、音色和模型。原私有签名不在公开仓库中。

结果与范围见 [VALIDATION.md](VALIDATION.md)，机器可读记录为 [validation/current-checks.json](validation/current-checks.json)。历史构建记录与本次结果分开保存。

## 来源、设计与历史

- 应用许可见 [LICENSE](LICENSE)，第三方代码和模型分别遵循 [NOTICE.txt](NOTICE.txt)、[MODEL_PROVENANCE.json](MODEL_PROVENANCE.json)；本次不修改许可证或模型权重。部分权重含非商业使用限制。
- [PROVENANCE.json](PROVENANCE.json) 记录原 1.5.0 备份构建，不是 1.5.1 安装包证书/验证记录。
- `design/` 是历史设计文件；本次修改 Android 原生界面，未同步历史 Figma 文件或生成真机截图。
- [SETUP.md](SETUP.md) 保留安装与各版本历史说明。历史验证不能代替当前手机验收。
