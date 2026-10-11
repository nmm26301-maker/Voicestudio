# 当前验证：1.5.4-hybrid（versionCode 20）

- Android Java、资源、D8、ARM64 APK 完整构建通过（23 项旧 API 等警告，无错误）；v2/v3 签名及 16 KiB 对齐通过。实际 1.5.3 APK 作为上版，47 项升级配置检查通过，包名/证书/Provider/资源/原生库连续，版本代码 19→20。见 `validation/build-1.5.4.log`、`validation/upgrade-checks-1.5.4.json`。
- 完整宿主回归通过。新增采样器对照 32,656 项：冻结 1.5.3 基线，检查 8/16/32 步、各种缓冲区、前缀/帧长、平分/随机/极端 logits、部分已填 token、零/超额 k、每步 token、随机数状态与缓冲区位置。16/32 步结果没有因计算优化改变。见 `validation/regression-1.5.4.log`。
- 原生 ONNX Runtime 1.30 测试图检查 1,460,223 项通过：16/32 步输出 PCM 与冻结旧引擎对照；8 步自动/参考音色多段输出；实际模型运行次数为 8/16/32；不支持的步数在加载前拒绝；取消后重试；实际模型与采样分项计时。缓冲区复用检查 26,489 项通过。见 `validation/staged-engine-1.5.4.log`、`validation/buffer-1.5.4.log`。这些是自编诊断图，不包含真实语音权重。
- 完整 Java 采样轨迹宿主基准（3 次预热、7 次交替顺序测量的中位数）已记录 `validation/sampler-benchmark-1.5.4.json`。相对 1.5.3 的 8 个 16/32 步用例耗时变化约 -3.14% 至 +1.94%（正值为减少），没有显示稳定明显的采样加速。**不把这一结果描述成手机或完整模型提速。** 8 步的直接改进是减少模型运算次数，总耗时仍包含图加载、分词、采样和解码。
- 实际 APK 与现有签名文件单独私下归档，核对签名及归档字节。公开仓库不提交 APK、密钥或完整发布备份；五项签名/模式保护的历史记录仍在 `validation/signing-guard-1.5.3.log`，本轮未重跑这些未变逻辑。

**未验收：手机/模拟器实际安装、数据保留、完整模型总耗时与内存、8 步真实音质与音色相似度、麦克风/视频人物模型、真实 Fish 调用。** 默认仍为 16 步，8 步明确标为实验选项，已排队参数保持原值。设备操作见 [安装与速度验收](docs/INSTALL_1.5.4.md)。

---

# 历史验证：1.5.3-hybrid（versionCode 19）

- Java、Android 资源、D8、ARM64 APK 完整构建通过（旧 API 等编译警告，无错误）；v2/v3 签名与 16 KiB ZIP 对齐通过。
- 已交付 1.5.2 正式 APK 及 1.5.1 测试 APK 分别作为上一版核对，47 项实际 APK 检查通过；包名、证书、组件类和分享身份不变，版本代码升为 19。新录音与人物提取类已包含在 DEX。见 `validation/upgrade-checks-1.5.3.json`、`validation/upgrade-from-preview-1.5.3.json`。
- 完整宿主回归通过；新增真实 PCM 人物提取 653 项检查，验证不连续对白拼接、65 秒完整导出、10 秒参考上限、重叠/越界拒绝与取消。队列文件归属及引用保护共 55 项通过。见 `validation/regression-1.5.3.log`。
- 完整发布备份包含实际 APK 与同一密钥，签名匹配与归档字节校验通过；拒绝覆盖旧备份。五项签名/模式构建保护重跑通过，见 `validation/signing-guard-1.5.3.log`。备份私下保存，不在仓库中提交二进制或密钥。

**未完成：手机麦克风与权限交互、旋转/锁屏等生命周期验收、视频实际解码、Whisper/CAM++/克隆完整模型准确性及性能、手机覆盖安装和数据保留。** 宿主 WAV 导出测试不代表人物分组准确率；不能按画面识别人名，重叠对白尚不能逐人分离。手机步骤见 [人物音色验收](docs/INSTALL_1.5.3.md)。

---

# 历史验证：1.5.2-hybrid（versionCode 18）

2026-10-11 完成正式名称 APK 构建：`com.voice.studio.preview`，名称“VoiceStudio”，签名沿用已交付的 1.5.1 测试版。

- 完整 Java、Android 资源、D8、ARM64 APK 构建、v2/v3 签名与 16 KiB 对齐通过。
- 44 项实际 APK 检查通过：此前测试包完整哈希核对、签名/包名/Provider 一致、版本代码 17→18、组件类存在、模型资源与原生库字节一致、不含签名私钥。见 `validation/upgrade-checks-1.5.2.json`。
- 原有合成、Fish 模拟传输、队列/模型/字幕、音频、动效与界面源码宿主回归重新运行通过，见 `validation/regression-1.5.2.log`。
- 原包名及独立正式版的签名构建保护通过；缺失/错误签名或冲突模式均拒绝构建。见 `validation/signing-guard-1.5.2.log`。

**未完成：手机实际覆盖安装与数据保留、完整模型推理、麦克风/蓝牙、真实 Fish 账户和实际视频导出。** 正式名称不等于已完成这些功能验收。设备步骤见 [安装与验收](docs/INSTALL_1.5.2.md)。

---

# 历史验证：1.5.1-hybrid（versionCode 17）

2026-10-11 对本次源码执行了完整 Android 未签名构建及宿主回归。详情见 `validation/current-checks-1.5.1-historical.json`、`validation/regression-1.5.1.log` 和 `CHANGELOG-1.5.1.md`。

- ECJ 编译、Android 资源、D8、ARM64 APK 打包、版本元数据及 16 KiB ZIP 对齐通过。存在旧 API 弃用等编译警告，无编译错误。
- 合成、Fish 模拟传输、采样/作品、分词、分段流水线、模型/文件/字幕、音频、动效宿主回归通过；本次没有真实 Fish 调用。
- 新增队列测试 49 项，包括维护/入队竞争、重复恢复、录音任务准入、检查点保留、旧时间清理和临时文件删除。
- 新增配音测试 3 项，覆盖 61 秒生成音频完整读取、尾部保留、超过 150 秒的有界拒绝。
- 配色和源码集成检查 137 项通过；动效曲线 8,003 项通过。它们不能代替 Android UI 截图、布局、触控或生命周期测试。
- 缺失原签名的发行构建明确失败、且未创建替代密钥的检查通过，见 `validation/signing-guard-1.5.1.log`。
- 用户上传的 1.5.0 APK 完整哈希及签名与 `PROVENANCE.json` 一致。另构建 `com.voice.studio.preview` 独立测试 APK，v2/v3 签名、16 KiB 对齐、分享接口隔离、组件类解析、原生库字节一致等 40 项安装包检查通过，见 `validation/preview-checks-1.5.1.json`。这是新测试签名，不是原签名恢复；尚未进行手机并存安装。
- 分享 URI 根据当前应用包名生成，适配独立测试版。默认原版构建拒绝与已上传 APK 证书不一致的密钥。

**未完成：真机/模拟器交互、完整语音/翻译/分离模型推理、麦克风/蓝牙、实际 MP4 输出、真实 Fish 账户、原签名覆盖升级。** 没有把历史原生 ONNX/ICU 检查或旧版签名结果算成本轮验证。逐功能设备步骤见 [功能核对与验收](docs/FUNCTION_AUDIT_1.5.1.md)。

---

以下记录只描述对应历史版本。

# 历史版本：1.4.1-hybrid（versionCode 15）

本轮验证与边界见 `CHANGELOG-1.4.1.md` 与 `validation/current-checks-1.4.1-historical.json`。原生分阶段引擎诊断使用自编图，对照冻结的 1.4.0 引擎，未使用完整 OmniVoice 权重；不代表手机性能或音质验收。当前日志为 `validation/*-1.4.1.log`。

以下为历史验证记录。

## 验证范围

- Android 完整构建、v2/v3 签名、16 KiB 对齐、版本代码和上一版证书一致性检查通过。
- 加速采样/作品筛选排序/时长/真实波形回归 1,589 项检查通过；包含 16/32 步、平分数、随机分数、极端分数和完整采样流程的原算法对照。
- 原生 ONNX Runtime 1.30 缓冲复用检查 26,489 项通过，使用自编四输入诊断图、三种动态序列长度、反复改变输入、取消与关闭检查，执行进程禁用网络。它验证缓冲读写和生命周期，不是完整语音模型或手机测速。
- 分词器/进度/缓存检查 17,548 项、合成流程 8,560 项、Fish 模拟接口 55 项、模型/文件/字幕 285 项和现有音频套件通过。
- 原生 ICU 74 的 211 组 Unicode 分词样例与原页生成路由检查通过，保留参数 256 的 Android 兼容修复。
- Android 真机界面、真实 Fish API、完整 OmniVoice 大模型推理和本机整体提速比例未实测。不能把减少步数、减少复制或宿主机诊断结果等同于手机总耗时减半；首次加载模型仍需要时间，音质也需要试听比较。
- 本次修改原生 Android UI；没有同步更新云端 Figma 文件。源码中的历史 Figma 素材仍保留。

## 官方参考与复现

- OmniVoice 生成参数（官方建议 16 步用于更快推理，更多步数更慢但改善质量）：https://github.com/k2-fsa/OmniVoice/blob/master/docs/generation-parameters.md
- ONNX Runtime Java 固定输出： https://onnxruntime.ai/docs/api/java/ai/onnxruntime/OrtSession.html
- ONNX Runtime Java 直接张量缓冲及输出复制说明： https://onnxruntime.ai/docs/api/java/ai/onnxruntime/OnnxTensor.html

设置 `ECJ_JAR`、`JSON_JAR` 后运行 `tests/run_synthesis.sh`、`tests/run_models.sh`、`tests/run_audio.sh`。原生缓冲验证需要 Linux ONNX Runtime 1.30 Java 包，设置 `ORT_HOST_JAR` 后运行 `tests/run_buffers.sh`。诊断图可用安装了 `onnx` 的 Python 运行 `tests/create_buffer_graph.py` 重新生成。Android 构建依赖见 `tools/build.sh`。


以下为历史验证记录，不代表本版手机验收。

## 验证

- Android APK 完整编译，v2/v3 签名与 16 KiB 对齐通过；证书 SHA-256 与上一版一致。
- 分词器/真实步数进度宿主机回归 17,545 项检查通过，包含 Unicode 空白、中文、英文、阿拉伯语、组合字符、emoji、补充平面字符、随机混合文本、BPE 合并与模型特殊 token。
- 原始 JVM 分词规则与修正规则边界一致。生产正则另在原生 ICU 74 以 flags=0 编译和执行，211 组 Unicode 样例通过；它是兼容性检查，不是 Android 真机测试。
- 合成流程 8,560 项断言、Fish 模拟接口 55 项断言、模型/文件/字幕 285 项断言及已有音频套件通过。
- 源码路由检查确认生成入口没有页面导航，并接入按任务取消与继续；实际动画外观及 Activity 生命周期仍需手机验收。
- 未运行 Android 真机、真实 Fish API 或完整 OmniVoice 大模型推理。修复了已定位的参数 256 崩溃，其他设备内存、模型运行及网络错误仍可能出现，并会显示可复制详情。

Android 正则参数说明：https://developer.android.com/reference/java/util/regex/Pattern#UNICODE_CHARACTER_CLASS

宿主机：设置 `ECJ_JAR`、`JSON_JAR` 后执行 `bash tests/run_synthesis.sh`，再运行 `python3 tests/check_icu_tokenizer.py`（需要原生 ICU）与 `python3 tests/check_inline_generation.py`。模型和音频回归入口分别为 `tests/run_models.sh`、`tests/run_audio.sh`。Android 构建使用 `tools/build.sh`，依赖与上一版相同。


以下为历史验证记录，不代表本版真机验收。



- APK 完整构建、v2/v3 签名和 16 KiB 对齐通过；证书与 1.3.7 一致。
- 分段对照包含中文、英文、阿拉伯语、数字、组合字符、emoji 与补充平面汉字，4 种语速、自动/参考音色、12,000 字及随机混合文本。对照保留原算法；验证段落一致、时长预算和 Unicode 边界。
- 验证完成音频复用、不覆盖损坏文件、缺失/空/截断文件、解析失败，以及不同引擎/音色/模型/种子/语速的请求区分和批量重复项保留。合成流程宿主机测试共 8,560 项断言（含各段重复边界检查）。
- Fish 模拟接口 55 项断言通过。没有使用真实密钥或调用线上生成服务。
- 模型/文件/字幕回归 285 项断言通过，现有音频测试套件通过。
- 分段准备阶段的宿主机基准见 `validation/synthesis-tests-1.3.8.log`；它不代表手机完整合成的速度或音质。
- 未运行 Android 真机、真实 Fish API 或完整 OmniVoice 推理。本轮检查不证明此前手机的合成失败已完全解决，仍需失败详情确认。

官方 Fish 接口说明：https://docs.fish.audio/api-reference/endpoint/openapi-v1/text-to-speech

源码运行新检查：设置 `ECJ_JAR` 与 `JSON_JAR` 后执行 `bash tests/run_synthesis.sh`；Android 构建依赖与原版相同。


以下为历史记录，不代表当前真机验收。



- APK 构建、v2/v3 签名、16 KiB 对齐、versionCode 11；签名证书与 1.3.6 一致。
- Fish 接口模拟测试 55 项断言：UTF-8 请求、模型/音色/参数校验、HTTP 状态、非音频/空数据拒绝、取消清理、MP3 头检查。不连接真实 API、不使用账号密钥。
- 模型与文件流程 285 项断言通过；现有音频测试套件通过。
- 未运行 Android 真机、真实 Fish API、完整 OmniVoice 大模型。本次尚未拿到用户手机的具体错误，不能认定原始合成失败的根因已定位或完全修复。

官方接口文档：https://docs.fish.audio/api-reference/endpoint/openapi-v1/text-to-speech


以下为历史验证，不代表当前真机验收。

# 1.3.6 新增验证范围

模型与文件工作流回归共 285 项断言通过，其中 43 项分包预检与目录一致性、35 项取消和进度检查。取消在解压前、解压中、启用前均验证临时目录清理、已有文件保留与重试。现有模型导入、分卷、恢复、并发作品与字幕流程同时通过。

最终 APK 编译、v2/v3 签名、16 KiB 对齐与上一版证书一致通过。模型清单和下载包目录沿用上一版，未更改模型权重。最终源码 ZIP 在完成后验证成员 CRC。

宿主机测试不模拟 Android 文件提供方或生命周期。页面重建、浏览器下载登录、模型安装的实际耗时及 Android 后台进程处理没有真机运行结果。未声称模型质量或 646 项语言已完成验收。

历史记录：

# 1.3.5 新增验证范围

最终构建与签名通过，沿用 1.3.4 证书。六类模型的 10 个下载分包已逐个校对，拼接 ZIP 内容与现有模型安装清单核对大小和 SHA-256。模型导入、分卷、恢复、作品并发与字幕流程共 207 项断言通过。

下载页使用浏览器处理下载和登录，保留手动文件导入。未运行手机或模拟器；浏览器解析、登录、下载和文件选择仍需真机检查。没有增加应用内下载器，没有公开托管模型 API。

历史验证记录：

# VoiceStudio 1.3.4 本轮验证

2026-10-09（北京时间）。最终版本与日志见 validation/current-checks-1.4.1-historical.json。

- Android 构建成功，versionCode 8、1.3.4-offline；v2/v3 签名与既有证书一致，16 KiB 对齐，无 INTERNET 权限，resources.arsc 未压缩，六个模型清单及自检图与源码一致。
- 文件/数据：ModelParts 24、OmniModels 27、StudioRecovery 24、HistoryConcurrency 100、SubtitleWorkflow 32，共 207 项。
- 音频：AudioChecks 20、RecordingHealth 17、RecordedInput 11、StreamingResampler 146，共 194 项。合计 401 项断言。
- 连续重采样验证 7 组采样率、单样本/任意大小分块、短输入与尾部排空；与完整音轨 sinc 结果比较，误差小于 1e-6；1024×1000 输入块输出总长度精确。
- 字幕验证 Unicode、多行 SRT、时间排序、过期编辑拒绝、时间重叠/范围拒绝、两线程同版本只有一次提交成功、导出失败保留主记录及启动修复。
- 保留已有 PCM/WSOLA/重采样回归。自检 Identity 图经 ONNX checker 验证；Linux ONNX Runtime 1.30.0 JNI 在 seccomp 拒绝网络下运行通过。
- Android AAC、自检页面、麦克风、蓝牙、真实视频封装以及完整大模型本轮没有运行；不能将桌面测试视为设备验收。设计 21 屏沿用 1.3.2，云端未更新。

以下为历史记录，不代表当前设备验收。

---

# VoiceStudio 1.3.3 本轮验证

2026-10-09（北京时间）。日志与当前范围见 validation/current-checks-1.4.1-historical.json。

- 最终 Android 构建成功，versionCode 7、1.3.3-offline；v2/v3 签名与原证书一致、16 KiB 对齐，无 INTERNET 权限，resources.arsc 未压缩，六个模型清单与源码一致。
- ModelPartsTest 24、OmniModelsTest 27、StudioRecoveryTest 24、HistoryConcurrencyTest 100，共 175 项宿主机断言通过。
- AudioChecks 20；RecordingHealthTest 17（无数据超时、静音、负幅值峰值、录音时限）；RecordedInputTest 11（有效 WAV 保留、短录音/损坏输入拒绝、已有录音不被覆盖）。共 223 项断言。
- PCM16/24/32、float32、双声道、重采样、WAV 异常和 WSOLA 回归通过。WSOLA 四个测试输出约 439.51/439.88/440.24/440.09 Hz。
- 播放器、焦点、耳机断开、录音硬件及服务生命周期仅完成编译检查，未在实际 Android 环境运行。宿主机文件适配器不模拟这些行为。
- 大模型本轮未重跑；646 项语言及中文克隆质量未验收。设计沿用 1.3.2 的 21 屏；本轮未重跑设计检查或写入在线 Figma。

以下为历史开发记录，不代表当前设备验收。

---

# VoiceStudio 1.3.2 本轮验证

2026-10-08。详情见 CHANGELOG-1.3.2.md 与 validation/current-checks-1.4.1-historical.json。

- Android SDK35/ECJ/AAPT2/D8 构建成功，versionCode 6、1.3.2-offline；v2/v3 签名与既有证书一致，16 KiB zipalign 通过，resources.arsc 未压缩，无 INTERNET 权限。
- ModelPartsTest 24、OmniModelsTest 27、StudioRecoveryTest 24、HistoryConcurrencyTest 100，共 175 项断言通过。
- 新增作品记录检查覆盖并发新增与逐条编辑、迟到回写保留用户信息、备份恢复、禁止旧编辑重建已删记录、软删除保留音频与活动队列删除保护。
- 使用宿主机 Context/AtomicFile 等适配器，只验证文件与数据逻辑，不验证 Android 生命周期、真实 fsync、麦克风权限或前台服务。
- AudioChecks 20 项及 PCM 格式、WSOLA 时长与重采样回归通过。
- 21 屏设计：842 个描述图层、383 个文字描述，几何和文字边界通过；本地插件 API 模拟通过，26 变量、140 原型链接、494 文字节点。PNG 已渲染并目视检查新增页面。
- 本轮没有重新运行大型模型、Android 真机/模拟器运行、在线 Figma 写入或真实 Figma 编辑器运行；录音启动改动仅完成构建检查。

以下保留历史记录，不作为当前设备验收结果。

---

# VoiceStudio 1.3.1 本轮验证

2026-10-08。详情和未验收范围见 CHANGELOG-1.3.1.md 与 validation/current-checks-1.4.1-historical.json。

- Android SDK35/ECJ/AAPT2/D8 构建成功，versionCode 5、1.3.1-offline。
- APK v2/v3 签名通过，证书 SHA-256 与 1.3.0 相同；16 KiB zipalign 通过，resources.arsc 未压缩，无 INTERNET 权限。
- ModelPartsTest 24 项与 OmniModelsTest 27 项断言通过：编号排序、下载重名、缺包/重复/混选、逐个打开与关闭、大小/散列、损坏模型原地修复和中断导入恢复。
- StudioRecoveryTest 24 项断言通过：任务与章节状态保留、运行/等待状态恢复、模型维护互斥、备份记录读取。使用宿主机 Android 适配器，不模拟真实 Android 生命周期、原子文件 fsync 或前台服务权限。
- AudioChecks 20 项，以及 PCM 格式、WSOLA 时长、重采样和 WAV 异常回归通过。
- 本次没有重新运行大型模型；没有 Android 真机/成功启动模拟器结果，没有在线 Figma 写入。

下面保留 1.3 的历史开发记录，与本轮检查分开。

---

# VoiceStudio 1.3 验证记录

日期：2026-10-08。区分此前原生模型运行与本次恢复后的复核，未把桌面 JVM 检查当作 Android 真机验收。

## 本次恢复后的检查

- 保存的 APK SHA-256 为 a8b6c6a71a3ec49bc7000b121e199db84c3117f72823451288230c887aa73566；33,543,561 字节。包内全部六个模型清单与当前源码一致，646 项合成目录、202 项翻译目录、146 项翻译到合成映射一致。resources.arsc 未压缩。
- AudioChecks 20 项断言通过。PCM16/24/32、float32、带符号双声道、单声道混合、双声道静音通过。
- WSOLA 0.5/0.8/1.25/2.0 时长适配保持测试音高约 439.51/439.88/440.24/440.09 Hz；输出长度精确。重采样、WAV 地址、合成元数据、异常 WAV 检查通过。
- 19 个设计画板、777 个描述图层、354 个文字描述的边界检查通过。本地插件 API 模拟通过：26 变量、24 文字样式、13 主组件、124 原型链接、458 文字节点。预览使用 Noto Sans CJK SC 渲染并检查。

## 此前 1.3 开发过程中的检查

- SDK35/ECJ/AAPT2/D8 构建，版本代码 4，min26/target34；签名 v2/v3，ARM64 JNI 及 16 KiB zipalign。原签名保留，无 INTERNET 权限。
- Linux seccomp 拒绝 IPv4/IPv6 socket；在无网络进程中实际执行 OmniVoice、三个克隆编码图、Whisper、NLLB、MDX-Net 与 CAM++，运行库遥测初始化前禁用。
- 英语长文 33 词测试的 Whisper 回读与目标文本吻合。英语克隆样例存在词语偏差；中文克隆样例回读存在明显偏差，不能视作合格中文克隆验收。回读工具本身也可能识别错误。
- NLLB 分词八个多语言用例与上游 tokenizer 一致；英译中、汉译英运行。MDX-Net 双声道输出时长一致。CAM++ 同声样例余弦约 0.696，另一声音约 0.212；不代表任意素材聚类准确率。
- 原生 fbank 对 Kaldi 比较最大绝对差约 0.000852；6144 点 FFT 与参考比较约 1.64e-11。EPUB 元数据、规范化路径、XXE 拒绝检查运行过；Android Html 解析未测试。

这些历史模型运行的原始工作区日志因维护清理未包含在本包中。本次没有重新下载和重跑所有大型模型；上述历史结果是开发记录，不是独立质量报告。

## 尚未验证

- 真实 Android 手机安装、JNI 推理、模型导入交互、内存峰值、耗电、耗时、播放、分享、文件选择器及后台进程恢复。
- 麦克风、蓝牙路由、悬浮窗、MediaCodec 解码、MediaMuxer MP4 以及实际 EPUB Html 处理。
- 小米 10 Pro 实机。此前模拟器启动超时，没有用模拟器结果替代真机结果。
- 646 种语言逐项质量、中文克隆相似度、混合音乐分离效果及任意素材说话人分组准确率。
- 云端 Figma 写入及真实编辑器插件运行；Starter MCP 配额仍耗尽。
