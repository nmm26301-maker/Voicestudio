# VoiceStudio 1.5.25-cloud 当前验证范围

本文件报告这次交付 APK，不以历史版本日志代替当前检查。

## 已通过

- Android 编译、DEX 打包、zipalign、v2/v3 原签名验证通过。包名 `com.voice.studio.preview`，versionCode 41，非 debug。
- 与上次实际交付的 1.5.24 对比，覆盖兼容检查 31 项通过，包括相同证书、分享 authority、组件集合、版本递增和关于页版本。
- 安装包没有推理原生库、权重、模型安装清单或 ONNX 图；仅保留语言目录元数据。
- Java 官方 API 84 项、健康检查 16 项、持久化 1023 项、音频验证 42 项、下载恢复 123 项、WAV 合并 10 项、API 请求 67 项、远程管线 10841 项、旧 GPU 协议 52 项 host/mock 检查通过。
- 网关 / 开源 worker / 九引擎及 GPT 桥接的 29 项服务端测试通过，使用 mock 模型/HTTP 与实际 FFmpeg 转换；没有提交真实付费请求。
- Cartesia 请求使用官方当前文档的 `2026-08-14` 版本、Bearer 认证、字符串 Voice ID、标准 WAV 输出。来源：https://docs.cartesia.ai/api-reference/tts/bytes 。GPT 原生请求依据 https://github.com/RVC-Boss/GPT-SoVITS/blob/main/api_v2.py ，桥接只访问本机服务且关闭环境代理。
- 原签名恢复归档已校验 APK 字节与密钥证书一致。公开仓库不含私钥或密码归档。

交付 APK SHA256：`87d67a1b1bcadf671b214f505fc41be5a34fcd45dccf13b0ced05fce02ff1f6f`。

## 验收边界

没有连接用户手机，也没有提供平台真实密钥或已运行的远程 GPU 网关。当前不宣称九引擎音质、克隆相似度、逐字朗读完整性或真实计费已验收。时长/格式/静音检测只识别明显失败。

手机录音、浏览器跳转、文件导出/多选、导航与音频播放仍需实际设备确认。模拟器检查单独记录，不等同真机。

免费浏览器模式需要从网页生成、下载、导回 App；自动长文合成和自动选择需可用的 API 或网关。官方账号、私有密钥及远程网关不能由固定网页链接代替。默认费用保护保留，未知提交状态不自动重复 POST。
