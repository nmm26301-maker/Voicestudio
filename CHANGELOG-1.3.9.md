# VoiceStudio 1.3.9 · 原页生成与梦幻进度

覆盖安装 `VoiceStudio-1.3.9-Hybrid.apk`，无需卸载。包名 `com.voice.studio`、ARM64、Android 8 及以上，versionCode 13，沿用上一版签名。已安装模型、作品、收藏音色与保存的密钥继续保留。

## 修复与交互

- 修复截图中 `IllegalArgumentException: Unsupported flags: 256`：OmniVoice 分词器不再传入 Android 不支持的 `UNICODE_CHARACTER_CLASS`。显式保留 Unicode White_Space 与字母、数字切分规则及局部 Unicode 大小写处理；没有改动模型权重。
- 创作页点击生成，以及 Fish 音色页点击生成试听，均留在当前页面。缺少模型、密钥、音色时也在原页提示；设置、音色、工作台仍可主动打开。
- 增加原生紫蓝粉渐变进度卡片：柔光、浮动星点、流动光带。本机推理显示当前段的实际步数与步数完成比例；加载、解码、检查保存、Fish 等待使用不定进度，不估算服务器百分比。
- 当前页面可以取消生成、继续暂停或失败的原任务、复制完整错误、播放刚完成的音频。失败保留草稿；继续原任务沿用提交时的文本与配置，修改草稿后可点击“生成声音”创建新任务。
- 任务状态在后台线程读取，退到后台后暂停页面轮询；返回应用恢复跟踪。关闭应用后，Omni/Fish 任务由既有后台服务管理，进程中断的恢复沿用原有暂停流程。
- 动画只在视图可见、任务运行时持续绘制，遵循系统关闭动画设置。完成以音频验证和保存后的任务状态为准。
- Fish Audio、离线模型下载/安装、声音克隆、转写、翻译、分离、说话人分组以及之前的长文本优化继续保留。

## 验证

- Android APK 完整编译，v2/v3 签名与 16 KiB 对齐通过；证书 SHA-256 与上一版一致。
- 分词器/真实步数进度宿主机回归 17,545 项检查通过，包含 Unicode 空白、中文、英文、阿拉伯语、组合字符、emoji、补充平面字符、随机混合文本、BPE 合并与模型特殊 token。
- 原始 JVM 分词规则与修正规则边界一致。生产正则另在原生 ICU 74 以 flags=0 编译和执行，211 组 Unicode 样例通过；它是兼容性检查，不是 Android 真机测试。
- 合成流程 8,560 项断言、Fish 模拟接口 55 项断言、模型/文件/字幕 285 项断言及已有音频套件通过。
- 源码路由检查确认生成入口没有页面导航，并接入按任务取消与继续；实际动画外观及 Activity 生命周期仍需手机验收。
- 未运行 Android 真机、真实 Fish API 或完整 OmniVoice 大模型推理。修复了已定位的参数 256 崩溃，其他设备内存、模型运行及网络错误仍可能出现，并会显示可复制详情。

Android 正则参数说明：https://developer.android.com/reference/java/util/regex/Pattern#UNICODE_CHARACTER_CLASS

宿主机：设置 `ECJ_JAR`、`JSON_JAR` 后执行 `bash tests/run_synthesis.sh`，再运行 `python3 tests/check_icu_tokenizer.py`（需要原生 ICU）与 `python3 tests/check_inline_generation.py`。模型和音频回归入口分别为 `tests/run_models.sh`、`tests/run_audio.sh`。Android 构建使用 `tools/build.sh`，依赖与上一版相同。
