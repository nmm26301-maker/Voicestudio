# VoiceStudio 1.5.0 · 深空界面与原生动效

安装 `VoiceStudio-1.5.0-Hybrid.apk`，可直接覆盖 1.4.1。包名 com.voice.studio、版本代码 16、Android 8.0 以上、ARM64。沿用上一版开发签名；作品、模型、音色、加密 Fish API Key 和任务保留。不要先卸载。

## 这次改动

- 创作、音色、作品、设置统一深空底色、青紫配色、渐变按钮、轨道图形和高对比度文字。离线工作台、模型下载/安装、录音入口、悬浮听写与应用弹窗同步改造。
- 创作页底部固定“生成/取消”按钮与状态；真实生成进度条位于固定栏，错误详情与播放入口保留在页面。点击生成仍留在当前页。
- 页面卡片依次淡入、轻微位移，按钮轻压回弹，弹窗缩放淡入，页间柔和过渡。头部轨道图形为装饰，不表示麦克风音量或音频内容。
- 设置页顶部新增“界面与动效”开关。跟随系统动画开关，活动暂停/隐藏时停止绘制循环；合成或省电模式下暂停头部装饰动画。生成进度仍可展示真实进度，关闭动效后显示静态状态。
- 原生 Canvas 绘制，不引入视频背景。头部装饰最多约每 80 ms 刷新，生成进度最多约每 50 ms 刷新；这些是代码调度间隔，不是实测帧率。
- 工作台新增模型、音色、转写、翻译、任务快速定位。保留 Fish Audio 配置、作品搜索、收藏、导出、分享、波形和任务继续/取消入口。
- 本版合成核心与 1.4.1 字节一致。保留 16/32 步、分阶段模型复用、分词缓存与直接写 WAV 的加速；这轮不宣称新的推理加速比例。

## 开源动效

实际采用 AndroidX `FastOutSlowInInterpolator` 和 `LookupTableInterpolator`，Apache-2.0。源代码固定在 AOSP support 提交 f2e05c341382db64d127118a13451dcaa554b702，只改 Java 包名。原代码、许可、来源和 SHA-256 位于 `third_party/androidx-interpolator`；保留版权头与 NOTICE。其余动效为本项目原生实现。

AndroidX 来源：https://android.googlesource.com/platform/frameworks/support/+/f2e05c341382db64d127118a13451dcaa554b702/interpolator/interpolator/src/main/java/androidx/interpolator/view/animation/FastOutSlowInInterpolator.java

参考 Material Motion 的缓动与时长设计，未打包 Material Components 库：https://github.com/material-components/material-components-android/blob/master/docs/theming/Motion.md

## 验证结果与范围

- Android 完整构建、v2/v3 签名、16 KiB ZIP 对齐、版本代码 16、ARM64 与原证书一致性检查通过。
- 实际缓动曲线 8,003 项宿主 Java 检查；137 项配色/开源代码完整性/界面接线静态检查。共享文字配色最低对比度 6.48:1，主按钮白字梯度各点大于 4.5:1。
- 合成流程 8,560 项，Fish 模拟接口 55 项，采样/作品/波形 1,589 项，分词/进度/缓存 17,548 项，分段流水线 16,646 项，模型/文件/字幕 285 项，音频套件与 ICU 74 的 211 组分词样例通过。原页生成/取消/继续源代码检查通过。
- 与 1.4.1 比对的 14 个核心文件字节完全一致。1.4.1 的原生 ONNX 诊断结果作为历史证据保留，本版没有重跑该诊断。
- 本版未运行 Android 真机或模拟器，未进行完整 OmniVoice 权重推理或真实 Fish API 调用。动效流畅度、手机速度、峰值内存和真实语音质量尚未测量。静态源代码检查不等于 Android 生命周期或交互验证。
- `VoiceStudio-1.5.0-UI-Preview.png` 是四页配色与布局示意，带示例作品和状态，不是真机截图。未更新云端 Figma。

## 复现与安装

构建需要 Java 17、Android SDK 35 / build-tools 35.0.0、ECJ 3.38+。配置 ANDROID_SDK_ROOT 与 ECJ_JAR 后运行 `bash tools/build.sh`。生成的 R.java 也参与手动编译。保留 `signing/voicestudio-development.p12` 才能覆盖安装；该密钥仅用于当前开发发行。

配置 JSON_JAR 后运行 `bash tests/run_synthesis.sh`、`bash tests/run_models.sh`。音频使用 `bash tests/run_audio.sh`。新增验证：`bash tests/run_motion.sh`、`python3 tests/check_cosmic_ui.py`；原页生成路由：`python3 tests/check_inline_generation.py`。检查结果存于 validation 目录。

安装后在设置顶部打开“界面与动效”。可先用短句检验已有模型，再用相同文本、音色和步数比较作品中的生成阶段用时。需要 Fish 时在设置保存 API Key、音色 ID 与模型，生成必须联网；OmniVoice 在本机运行。模型仍通过“下载与安装离线模型”入口按需下载并导入。

预训练模型权重仍遵循各自的上游许可，界面改造没有更改模型许可。源码 Apache-2.0。
