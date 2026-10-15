# VoiceStudio 1.5.27.1 云端版

原生 Android 声音工作室，最低 Android 8.0（API 26），版本 `1.5.27.1-cloud / versionCode 45`。沿用已交付版包名 `com.voice.studio.preview` 和原签名覆盖升级，不自动删除旧作品、参考 WAV 与录音。

本次采用科幻、可爱、梦幻风的原生界面：柔和星空、耳机云朵伙伴、圆角紫蓝卡片。四个固定入口为音色、合成、任务、作品；设置在右上角。手机端移除本地模型合成、模型安装入口和 Android TTS。APK 不包含本地推理原生库或模型安装资源。

| 页面 | 功能 |
| --- | --- |
| 合成 | 文本草稿、录音快捷入口、语言/语速、分段合成、进度、继续与最近作品 |
| 音色 | 九引擎/自动选择、只读连接检测、参考录音克隆配置、Fish 原有账户 |
| 任务 | 录音实验室、独立文字合成工作台、批量云端朗读、任务与旧模型清理 |
| 作品 | 搜索、收藏、排序、播放、波形、重命名、导出和回收站 |
| 设置 | 云端连接、动效、平台申请入口与作品管理 |

[免电脑免费试用](docs/FREE_MOBILE_CLOUD.md) · [手机 Modal 部署与实际地址](docs/MODAL_PHONE_DEPLOY.md) · [五引擎独立部署](docs/OPEN_MODEL_DEPLOY.md) · [平台申请与自动地址](docs/PLATFORM_ACCESS.md) · [九引擎部署和使用](docs/CLOUD_ENGINES.md) · [八引擎接口与合成步骤](docs/EIGHT_ENGINES_SYNTHESIS.md) · [本次变更](CHANGELOG-1.5.27.1.md) · [验证范围](validation/current-checks.md) · [历史版本说明](docs/HISTORY-1.5.19.md)

包含你指定的 IndexTTS 2.5、CosyVoice 3、Qwen3-TTS、ElevenLabs、MiniMax Speech、Chatterbox V3、GPT-SoVITS、Cartesia Sonic，以及保留的 Fish Audio（总计九个）。前四款开源模型通过 Python worker，GPT-SoVITS 通过独立安全桥接，Fish、ElevenLabs、MiniMax、Cartesia 提供官方 API 直连。开源模型必须有自己的可用 GPU/权重和部署网关，商业直连需 API Key/Voice ID 才能真实合成，此安装包不会自动提供免费服务器。模拟回归不代表已通过九模型真实音质验收。

使用已有参考录音，输入新文本后请求云端；手机无损分段并校验全部音频后发布作品。免费试用模式支持手机导出参考 WAV、复制录音原文、网页生成后导入结果，以及多段 WAV 的本机拼接（没有本地 TTS 推理）。自动选择在提交前探测开源服务，已提交未知状态请求不自动重复扣费。旧离线工具任务不能继续本地推理，可查看已有结果。

构建：准备 Android SDK 35、build-tools 35.0.0 和 ECJ，恢复已有签名后执行 `VOICE_STANDALONE=1 bash tools/build.sh`。公开仓库不存签名私钥。私有备份规则见 [BACKUP.md](BACKUP.md)。

## 1.5.27.1 API 修复

恢复 1.5.27 的界面和动效，取消 1.5.28 的界面改动。修复商用平台切换仍使用网关模式、原始录音串入平台注册音色请求、平台错误被吞没的问题。MiniMax 支持明确选择中国站 / 国际站、分别保存密钥并读取实际音色。连接检查会查询认证与音色，不再仅检查密钥格式。覆盖安装后请在音色页重新选择平台并保存连接；真实账号合成尚未验收。详见 [修复说明](CHANGELOG-1.5.27.1.md)。
