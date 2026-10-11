# GitHub backup: VoiceStudio 1.5.0

先执行 `python3 tools/fetch_runtime.py` 恢复经 SHA-256 核对的 ONNX Runtime 1.30.0 依赖。构建与验证见 SETUP.md；原开发签名保留在私有备份，未上传公开仓库。

---

# VoiceStudio 1.5.0

深空原生 UI 与动效。当前安装与验证范围见 SETUP.md，版本记录见 CHANGELOG-1.5.0.md。

---

# 当前版本：VoiceStudio 1.4.1 Hybrid

长文本分阶段复用合成/解码会话，省去每段 WAV 临时文件与回读；保留首段自动音色锚点和 16/32 步。新作品显示加载、推理、解码用时。构建与宿主机回归通过，手机总提速与峰值内存尚未实测。使用说明见 `SETUP.md`，本轮修改与边界见 `CHANGELOG-1.4.1.md`。

以下为历史版本说明。

# 当前版本：VoiceStudio 1.4.0 Hybrid

快速 16 步默认，复用本机推理缓冲与分词器，作品页改为深色霓虹声音星库。质量优先 32 步可切换。原页生成、梦幻进度、Fish 和六类离线功能保留。验证与实际手机测速限制见 `CHANGELOG-1.4.0.md`、`SETUP.md`。

# 当前版本：VoiceStudio 1.3.9 Hybrid

修复 Android 参数 256 的分词器崩溃；生成留在原页，带梦幻渐变进度、取消、继续、错误复制和完成播放。完整使用与验证范围见 `SETUP.md`、`CHANGELOG-1.3.9.md`。

# VoiceStudio 1.3.8 · Android 云端与离线工作室

原生 Java/ONNX 的 Android ARM64 应用，最低 API 26、目标 API 34。APK 版本 1.3.8-hybrid、versionCode 12。用户导入模型后，推理在手机上进行；运行时遥测在初始化前禁用，APK 恢复网络权限供用户主动选择的 Fish Audio 使用；不会自动将离线任务切到云端。安装步骤与模型下载见 [SETUP.md](SETUP.md)。

| 功能 | 当前实现 | 验证边界 |
| --- | --- | --- |
| Fish Audio | 恢复 API Key 加密配置、模型选择、原有收藏音色、后台合成与 MP3 导出 | 本轮模拟接口测试通过；真实账户及 Android 联机运行未测试。 |
| 646 项语音合成 | 原生 OmniVoice INT8、语言搜索、播放与 WAV 导出 | 全部语言未逐项验收。 |
| 克隆与声音指令 | 三图参考编码、保存音色、指令、种子、16/32 步 | 中文克隆回读偏差明显，相似度需人工试听。 |
| 长文与批量 | 分段合成、自动参考音色延续、批量队列 | 长文英语测试通过；设备性能未验证。 |
| 有声书 | UTF-8 TXT/EPUB 按顺序分章、续做、共享参考音色、ZIP | Android Html 解析及大书籍真机运行待验收。 |
| 转写与录音 | Whisper tiny、音视频解码、麦克风/蓝牙入口、TXT/SRT/JSON | 识别模型运行过；录音与路由待真机验收。 |
| 字幕与翻译 | 字幕校对/拆分、版本冲突保护、导出修复、NLLB、202 项目录、146 项合成映射 | 翻译需校对，不表示 646 项翻译能力。 |
| 多音色配音 | 分组音色、时间轴、WSOLA、WAV/SRT/MP4 | 替换原音轨；Android MP4 封装未真机验收。 |
| 人声分离 | MDX-Net、保留双声道、人声与伴奏两个 WAV | 模型运行过；混合音乐质量需试听。 |
| 说话人分组 | CAM++ 片段嵌入、聚类、手动分组与音色映射 | 同段多人需先拆分，不保证自动分组全对。 |
| 悬浮听写 | 可拖动小窗、主动录音、离线识别、复制结果 | 缓存录音可重试识别；悬浮窗待真机验收。 |
| 队列与作品 | 任务分类、后台刷新、应用内试听、单任务暂停、手动恢复、作品回收站、保存/分享 | 普通任务重试重新执行；有声书复用章节。 |

本轮优化长文分段与分词器加载，复用已生成音频并防止重复提交；Fish 增加配置引导和短句试听，失败可带原文换引擎。新检查见 `tests/run_synthesis.sh`。

本次更新说明见 [CHANGELOG-1.3.8.md](CHANGELOG-1.3.8.md)。设置与工作台提供模型下载页；浏览器管理下载，返回后选择文件导入。下载地址可能要求登录保存文件的 ChatGPT 账号。安装页提供分包预检、可取消进度和完成后的使用入口。模型分包、修复、任务恢复与作品并发读写检查见 `tests/run_models.sh`；设置页可直接进入六类模型管理；工作台可运行设备基础自检并导出结果。

## 设计

`design/` 提供 1.3.2 的 21 屏 Figma 离线导入包，包含作品回收站和录音启动页，主要由可编辑文本、组件实例和矢量构成，不使用整屏截图作为图层。本次未更新云端文件；此前在线写入受 Starter MCP 配额限制。导入说明见 `design/README.md`；真实编辑器运行尚未验证；本轮未更新设计页面。

## 构建

`tools/build.sh` 需要 Android SDK 35 / build-tools 35.0.0、ECJ 3.38+ 和 Java 17。配置 `ANDROID_SDK_ROOT`、`ECJ_JAR` 后执行。包含 ONNX Runtime Android 1.30.0 AAR。开发签名 `signing/voicestudio-development.p12` 必须保留，手工构建脚本使用它以保持覆盖升级；此密钥不适用于商店正式发行。Android Studio/Gradle 也可打开项目，但默认 Gradle debug 签名不同于已交付安装包。

## 回归检查

`ECJ_JAR=/path/ecj.jar bash tests/run_audio.sh` 编译并运行音频核心测试。`node tests/plugin_mock.cjs design/code.js` 检查插件生成的本地 API 模拟树。`VOICE_STUDIO_FONT=/path/NotoSansCJKsc-Regular.otf python3 tests/design_check.py` 需要 Pillow。`tests/offline_run.py` 用 Linux seccomp 拒绝网络。模型测试记录及当前未验收项目见 `VALIDATION.md`。`tests/legacy_*` 只保留历史测试，不是当前真机验收。`tests/run_native_selftest.sh` 需要 `ECJ_JAR`、`ORT_HOST_JAR`（ONNX Runtime 1.30.0 宿主机 Java 包）和 `ANDROID_SDK_ROOT`，在 Linux 拒绝网络的进程中运行小型 Identity 图；不执行 Android 音频测试。`tools/create_selftest_graph.py` 使用 onnx Python 包重建自检图。

## 来源与许可

OmniVoice、Whisper、NLLB、MDX-Net 与 CAM++ 的来源和各文件散列见 `MODEL_PROVENANCE.json` 及 `app/src/main/assets/*-model-manifest.json`。上游权重许可独立于应用代码；OmniVoice 与 NLLB 含非商业使用限制，不重新许可第三方权重。见 `NOTICE.txt`。
