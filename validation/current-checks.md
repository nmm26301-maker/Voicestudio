# VoiceStudio 1.5.26-cloud 当前验证范围

## 当前交付已通过

- Android ECJ 编译、DEX 打包、zipalign、v2/v3 原签名验证通过。包名 `com.voice.studio.preview`、versionCode 42、非 debug。
- 与前次实际交付的 1.5.25 对比，31 项覆盖兼容检查通过：证书、分享 authority、组件集合、版本递增、关于页版本等一致。
- 无本地推理库、权重、模型安装清单或 ONNX 图，只有语言目录元数据。
- Java 新增 34 项音色读取和部署配置检查：只读 GET、平台认证头、分页编码、ID 过滤、重复条目、拒绝跳转、错误状态、响应边界、Modal 实际地址及 JSON 配置校验。
- 既有 Java 84 项官方 API、16 项健康检查、1023 项持久化、42 项音频、123 项恢复、10 项 WAV 合并、67 项请求、10841 项远程管线、52 项旧 GPU 协议检查通过。均为 host/mock。
- 服务端 40 项测试通过，包括四平台模板、GPT 桥接、九引擎路由、部署结果生成和实际 FFmpeg 格式转换。部署结果测试使用模拟 URL，不是已部署的用户 URL。
- 默认分支的 Modal 手动 workflow 仅增加查询真实地址和上传无密钥连接 JSON；发布入口仍须用户主动 Run workflow。没有运行云部署或触发 GPU 推理。
- 发布恢复归档内的 APK 与交付 APK 字节一致，签名密钥沿用且未写入公开仓库。

APK SHA256：`b1f73c0879716e709a34acb930c03e9ce9970e550d4fcd53f7725b7866d1f2a6`。

## 真实验证边界

没有连接用户手机、实际平台账户、真实 Space 或远程 GPU；录音、播放、模型加载、克隆音质、逐字完整朗读、真实配额和计费没有验收。读取音色的测试使用 mock，尚未使用真实账号 API Key 查询。

HF ZeroGPU、Modal、Lightning 的直接 Qwen3-TTS 模式必须有实际运行服务和对应权限；Kaggle 使用 Notebook 生成 WAV 后导入，不能冒充常驻 API。其他四款开源模型仍由已提供的独立 worker / GPT 桥接服务承接，不声称本次四平台向导已经自动部署全部模型。

网关密钥在手机生成不代表已修改平台 Secret；用户需在平台与 App 填入相同值。健康检查的配置成功不代表 GPU 已加载或合成成功。商业 API 费用保护和未知提交状态不自动重试的规则保留。

接口核对来源：
- ElevenLabs 音色 GET：https://elevenlabs.io/docs/api-reference/voices/search
- Cartesia 音色 GET：https://docs.cartesia.ai/api-reference/voices/list
- Modal Function.get_web_url：https://modal.com/docs/sdk/py/latest/Function
