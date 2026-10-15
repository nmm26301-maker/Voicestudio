# VoiceStudio 1.5.27-cloud

- Modal / Lightning 连接表单支持 Qwen、IndexTTS 2.5、CosyVoice 3、Chatterbox V3、GPT-SoVITS，所选引擎与部署 JSON、健康检查及实际请求保持一致。
- 四款待补引擎提供固定官方源码提交、独立 Python/依赖环境的云端部署配方。新增 Modal 手动 engine 选择工作流和 Lightning 远程启动器，不自动触发付费部署。
- 统一鉴权、录音上传、单模型路由、并发忙碌拒绝与完整 WAV 返回；健康检查不下载模型、不提交推理。GPT 使用同容器 loopback 子进程桥接，要求准确参考原文。
- IndexTTS 按 GPU 能力选择 BF16；Chatterbox 使用官方 V3 模型选择；修正 Lightning 帮助按钮重复打开自身的问题。
- 版本号 1.5.27-cloud / 43，继续沿用原包名和签名。

验证：Android 构建与签名、31 项升级兼容、Java host/mock 管线、服务端协议回归通过。没有执行实际云端镜像构建或 GPU 推理；手机安装、录音、播放、音质和真实全文朗读尚未验收。HF ZeroGPU / Kaggle 当前仍是 Qwen 模板，不宣称五模型全平台均可用。
