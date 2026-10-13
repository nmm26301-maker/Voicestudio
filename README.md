# VoiceStudio 1.5.11

Android ARM64 声音工作室：文字合成、音色克隆、录音转写、字幕配音、翻译、人声分离与有声书。最低 Android 8.0（API 26），当前源码版本 `1.5.11-hybrid`，`versionCode 27`。

本次优化完整音频校验、下载恢复与生成进度。GPU WAV 改为固定 32 KiB 缓冲区完整读取，避免反复解码成浮点数组；仍检查 float32 无效值、截断和格式错误。HF 已生成音频的下载遇到超时、断线或 HTTP 502/503/504 时，最多自动重试两次，合成提交与队列连接不会自动重放。长文显示已校验保存的分段进度。沿用原签名与包名，版本代码 27。免费 GPU [使用与申请说明](docs/FREE_GPU.md)。宿主校验阶段提速不代表手机或 GPU 推理提速，真实读音与设备体验仍需实测。

## 功能在哪里

| 主入口 | 内容 |
| --- | --- |
| 创作 | 文本、草稿、语言/音色、语速、8/16/32 步、生成进度、取消、继续和最近作品 |
| 音色 | OmniVoice、GPU 加速接口、Fish Audio、手机离线音色切换；参考音色管理入口 |
| 工具 | 按用途切换八个分类，顶部分类栏固定；每个分类展示对应工具 |
| 作品 | 搜索、排序、收藏、播放、波形、重命名、保存、分享、回收站 |
| 设置 | Fish 密钥与模型、动效、模型安装管理、回收站和应用说明 |

| 工具分类 | 工作流程 | 需要的模型/配置 |
| --- | --- | --- |
| 转写 | 导入音视频/录音/悬浮听写 → 任务结果 → 字幕校对与导出 | Whisper；麦克风/悬浮窗权限按需授权 |
| 音色 | 页面内录音/视频人物提取 → 试听并命名 → 创建、选择人物音色 → 创作 | 创建需克隆模型；合成需 OmniVoice；自动文字需 Whisper；视频分组另需 CAM++ |
| 翻译 | 输入文字 → 翻译任务 → 复制、保存或配音 | NLLB；合成另需 OmniVoice |
| 音频 | 分离人声/伴奏；从已完成转写任务进入说话人分组与字幕配音 | 分离用 MDX-Net；分组用 CAM++；配音用 OmniVoice，翻译另需 NLLB |
| 有声书 | UTF-8 TXT/未加密 EPUB → 分章合成 → ZIP 导出 | OmniVoice |
| 批量 | 每行一个任务 → 独立合成结果 | OmniVoice，使用本机语言/语速/参考音色配置 |
| 任务 | 筛选、暂停、继续、失败详情、结果和删除 | 根据任务类型；新录音必须单独开始 |
| 模型 | 六类模型的下载、安装、完整性检查、移除和设备诊断 | 模型包需另行下载 |

批量、有声书、字幕配音当前使用 OmniVoice，不会因首页选择 Fish 而自动发送云端。Fish 只用于明确选择的 Fish 生成任务。

## 沿用的功能修复

- 录音任务恢复重新处理麦克风授权；新录音不能排在其他运行任务后面；已保存的录音可随暂停队列继续识别。
- 任务入队、恢复与模型维护共用互斥检查；恢复清除旧的起止时间和错误状态。
- 字幕配音分块读取完整生成片段，修复只取前 60 秒的问题；过长片段报错提示拆分，保留原有时长匹配约束。
- 批量、克隆、声音参数和翻译表单验证失败保留输入；批量已提交的行从输入框移除。
- 文件导出使用截断写入，补上空返回和缺少文件选择器处理；MP3 导出状态支持页面重建恢复。
- 模型安装返回后刷新状态；“使用模型”进入对应工具或选择 OmniVoice；下载链接保留原目标地址。
- 主播放器在耳机断开时暂停；页面重建不再重复用旧分享文本覆盖已修改草稿。
- 缺少原签名时停止发行构建，不再自动生成无法覆盖旧安装的新密钥。

本次变化见 [1.5.11 更新记录](CHANGELOG-1.5.11.md)，功能修复见 [1.5.1 更新记录](CHANGELOG-1.5.1.md)，设备步骤见 [正式版安装与验收](docs/INSTALL_1.5.11.md) 和 [功能核对](docs/FUNCTION_AUDIT_1.5.1.md)。

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

### 正式版：覆盖已交付的 1.5.1—1.5.10

恢复已备份的 `signing/voicestudio-preview.p12` 后执行：

```bash
VOICE_STANDALONE=1 bash tools/build.sh
python3 tools/verify_upgrade.py build-manual/VoiceStudio-1.5.11-Hybrid-Standalone.apk /path/to/VoiceStudio-1.5.1-Hybrid-Preview.apk
```

正式版名称为“VoiceStudio”、版本为 `1.5.11-hybrid`，包名继续使用 `com.voice.studio.preview` 以保留测试版的安装身份和数据。签名文件名及别名为兼容旧备份而保留，不要重新生成。`VOICE_STANDALONE=1` 会核对已交付测试版的证书指纹；密钥缺失或不匹配时拒绝构建。

安装时直接更新已交付的 1.5.1 测试版或 1.5.2—1.5.7 正式版，无需卸载。已在测试版中导入的模型、音色、作品和配置使用同一数据目录；实际保留情况需要手机验收。本分支与原 `com.voice.studio` 的 1.5.0 并存，不自动迁移该旧应用的数据。

### 每次交付备份 APK 与原签名

每次生成并交付版本都要单独保留实际 APK 与现有签名备份，不覆盖旧版本、不重新生成密钥、不把私钥上传到公开仓库。构建后运行：

```bash
python3 tools/backup_release.py build-manual/VoiceStudio-1.5.11-Hybrid-Standalone.apk /private/release-backups
```

脚本核对 APK 与原有密钥的签名证书，并校验归档的 APK 和密钥字节。完整备份 ZIP 私下保存；安装时使用单独 APK。

### 可选测试构建

原签名尚未恢复时，可以用 `VOICE_PREVIEW=1` 构建 `com.voice.studio.preview`，桌面名称为“VoiceStudio 测试版”。它与原 1.5.0 版并存，使用独立数据目录、音频分享接口和专用签名；旧版模型、音色、作品及 Fish 配置不会自动复制到测试版，需要重新导入/配置。

首次创建测试版签名（已有测试版时必须恢复其备份签名，避免影响后续升级）：

```bash
mkdir -p signing
keytool -genkeypair -keystore signing/voicestudio-preview.p12 -storetype PKCS12 \
  -storepass android -keypass android -alias voicestudio-preview \
  -keyalg RSA -keysize 2048 -validity 3650 \
  -dname 'CN=VoiceStudio Preview, O=VoiceStudio, C=CN'
VOICE_PREVIEW=1 bash tools/build.sh
python3 tools/verify_preview.py build-manual/VoiceStudio-1.5.11-Hybrid-Preview.apk /path/to/original-1.5.0.apk
```

此前测试版的签名现在继续用于独立正式版，不能覆盖原 1.5.0。备份 `.p12` 到私人存储；签名文件被 Git 忽略，不应提交到公开仓库。默认原包名发行构建仍核对原版证书指纹，拒绝不匹配的密钥。

2026-10-11 用户上传的原版 APK 的 SHA-256 与 `PROVENANCE.json` 完全一致；测试版已通过签名、实际包名/分享接口隔离、组件 DEX 类存在、运行库一致与对齐检查，见 `validation/preview-checks-1.5.1.json`。这不代表已完成手机安装和功能验收。

结果与范围见 [VALIDATION.md](VALIDATION.md)，机器可读记录为 [validation/current-checks.json](validation/current-checks.json)。历史构建记录与本次结果分开保存。

## 来源、设计与历史

- 应用许可见 [LICENSE](LICENSE)，第三方代码和模型分别遵循 [NOTICE.txt](NOTICE.txt)、[MODEL_PROVENANCE.json](MODEL_PROVENANCE.json)；本次不修改许可证或模型权重。部分权重含非商业使用限制。
- [PROVENANCE.json](PROVENANCE.json) 记录原 1.5.0 备份构建，不是当前安装包证书/验证记录。
- `design/` 是历史设计文件；本次修改 Android 原生界面，未同步历史 Figma 文件或生成真机截图。
- [SETUP.md](SETUP.md) 保留安装与各版本历史说明。历史验证不能代替当前手机验收。
