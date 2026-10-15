# VoiceStudio 1.5.27-cloud 当前验证范围

## 已通过

- Android ECJ、资源、DEX、zipalign 与原签名 v2/v3 校验通过；包名 com.voice.studio.preview，versionCode 43，非 debug。
- 与已交付 1.5.26 比较，31 项升级兼容检查通过；云端 APK 不含本地推理库、权重、安装清单或 ONNX 图。
- Java 音色读取和部署配置 44 项 host/mock 检查通过，覆盖五引擎 URL/JSON 匹配、错误引擎拒绝、官方音色只读读取和鉴权边界。
- 既有 Java 84 项官方 API、16 项健康检查、1023 项持久化、42 项音频、123 项恢复、10 项 WAV 合并、67 项请求、38 项 prompt、10841 项远程管线、52 项旧 GPU 协议检查通过，均为 host/mock。
- 服务端 51 项回归通过：五种单引擎完整文字 / 参考 WAV / 原文传递、鉴权、只读健康检查、错误引擎与无效文字拒绝、并发忙碌不重复提交、错误释放锁、GPT 原文前置校验、Index BF16 能力选择、Chatter V3 参数、连接 JSON、既有九引擎路由和实际 FFmpeg 格式转换。
- Python 3.10 语法兼容和生成的远程 shell 命令语法通过。
- 私有恢复归档中的 APK 与交付 APK 字节一致，签名密钥未进入公开源码。

APK SHA256：`c3275b3c09ab5071e0619e52cbd61ccb428eda560badca96556e948185039c02`。

## 未完成的真实验收

没有用实际平台账户构建新引擎镜像、启动 GPU 或验证模型加载、音质和逐字完整朗读。51 项服务端检查使用 host/mock，不代表实际 GPU 运行通过。未连接用户手机进行安装、数据保留、录音或播放验收；原签名兼容检查不能代替这些验收。

Modal / Lightning 新增五种引擎的独立部署配方；HF ZeroGPU 与 Kaggle 当前仍使用 Qwen 模板。Kaggle 生成 WAV 后导入，不是常驻 API。GPT 当前选官方 V2 基线，没有声明已支持最新 Pro/V5 权重。

健康检查不下载权重或提交合成，但 Modal 请求可能唤醒 GPU 容器并产生运行费用。部署成功、引擎已配置均不等于真实合成成功。网关密钥需在平台 Secret 与手机一致。实际部署入口仍由用户手动 Run workflow，本次没有执行收费云部署。

## 证据

- build-1.5.27.log / upgrade-checks-1.5.27.json
- cloud-java-1.5.27.log / cloud-server-1.5.27.log
- release-backup-1.5.27.json
- 官方源码入口和精确提交：server/engine_recipes.py，部署与覆盖范围：docs/OPEN_MODEL_DEPLOY.md
