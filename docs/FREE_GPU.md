# 免费 GPU（1.5.9）

手机路径：**音色 → GPU 加速接口 → 免费 GPU**，设置页也有同一入口。页面内提供三个平台的注册、额度、控制台与部署链接，不内置账号或共享密钥，不轮换账号绕过额度，不自动切换收费服务。

## Hugging Face ZeroGPU

应用内“配置并使用 ZeroGPU”默认使用官方 `https://k2-fsa-omnivoice.hf.space`。保存后回到创作输入全文，使用真正的 Gradio 6 `/gradio_api/call/_design_fn` → SSE → 音频下载协议；不是把介绍页当成 REST 地址。匿名可留空 Token，个人额度/排队身份由 HF 识别。填写自己的 HF Token 可供授权访问；浏览器登录状态不会自动复制到 Android 请求。

目前官方介绍匿名每天 2 分钟、免费账号每天 5 分钟 GPU 计算时间；不是生成音频时长。额度与政策可能变化，查看 [官方说明](https://huggingface.co/docs/hub/spaces-zerogpu)。账号验证邮箱且注册满 30 天等条件满足后可按官方政策创建自己的 ZeroGPU Space。注册：[huggingface.co/join](https://huggingface.co/join)；Token：[设置](https://huggingface.co/settings/tokens)；官方演示：[OmniVoice](https://huggingface.co/spaces/k2-fsa/OmniVoice)。

自建 Space：把 `server/zerogpu` 内的 app.py、requirements.txt、README.md、LICENSE 放入 Gradio Space，在硬件设置选择有资格使用的 ZeroGPU。模板固定 OmniVoice 源码提交与 Gradio 版本；其他传递依赖与模型 revision 尚未完全锁定。应用填写实际 `https://作者-空间名.hf.space` 运行地址。ZeroGPU 不支持普通 FastAPI 替代 Gradio。

人物音色：在配置框主动勾选“上传所选参考录音”，选择工具 → 音色已保存的人物。合成时只截取该人物记录的 3—10 秒参考片段并上传，不发送整段视频、不自动发送其他录音。未选择时生成自动音色。保存的参考文字同时发送，避免每次重新进行服务器 ASR。原录音缺失、片段无效或已更改上传选择，任务会报错；不会静默切回其他人物。官方 Space 不支持本机采样种子。公共服务管理员的日志、文件留存由该服务管理，按需要使用自己的 Space。

## Modal

[注册](https://modal.com/signup)、[免费额度与价格](https://modal.com/pricing)、[控制台](https://modal.com/apps)。Starter 当前每月有 30 美元计算额度，超出可能收费，不能承诺无限免费。应用使用自己的部署；不把注册网站当作可调用的 GPU 服务。

在已登录自己 Modal 账号的电脑执行：

```bash
pip install modal
modal setup
# 在 Modal 控制台 Secrets 创建 voicestudio-api，填入自己生成的 VOICE_API_TOKEN。
modal deploy server/modal_app.py
```

把部署输出的 HTTPS `.modal.run` 根地址和自己设置的 **VOICE_API_TOKEN** 填入应用“配置并使用 Modal”。不是填 Modal 账号 token_id/token_secret。API 兼容 `/v1/audio/speech`，全文、语言、语速、8/16/32 步与种子由官方 OmniVoice GPU 模型处理，当前使用自动音色。

模板采用 L4，最多一台 GPU 容器，无请求后 15 秒缩到零；CPU 在申请 GPU 前验证服务密钥和请求。默认每天最多接受 **10 次**、每次最多 **2000 字**；超额 HTTP 429，失败调用也计数。每日按 UTC 重置，可在 Secret 配置 `VOICE_DAILY_MAX_CALLS`。这只是应用调用上限，**不是美元预算保证**；模型加载、容器运行、存储等也可能计费，必须查看账号实际剩余额度。首次调用会下载模型并冷启动，可能超时；查看控制台状态后重试。模板未在用户 Modal 账号部署，不能把静态检查说成已上线。

## Kaggle

[注册](https://www.kaggle.com/account/login?phase=startRegisterTab)、[账号验证与额度](https://www.kaggle.com/settings)、[Notebook](https://www.kaggle.com/code)、[官方高效 GPU 使用说明](https://www.kaggle.com/docs/efficient-gpu-usage)。免费额度随账号和平台政策变化，不硬编码为每周一定 30 小时。

1. 在创作页填写全文和语言/语速；到免费 GPU 页点“导出当前全文 GPU 任务”，保存 `.ipynb`。
2. 在 Kaggle 创建 Notebook 并导入该文件，开启 GPU 和 Internet。完成账号要求的验证，确认剩余额度，再 Run All。没有 GPU 会报错，不会偷偷回落到慢 CPU。
3. 下载运行输出 `/kaggle/working/VoiceStudio-Kaggle.wav`，在应用点“导入 Kaggle 生成的 WAV”。应用验证完整 WAV 后加入作品，可播放、导出和分享。

导出的 Notebook 含全文和生成参数，不含任何账号密钥或参考录音；用户自己选择上传。该入口提供真实的 GPU 批处理；Kaggle 没有提供常驻手机合成 API，本版本不会伪造直连地址或开启公开隧道。当前为自动音色。

## 验证与限制

官方 HF 公共接口已实际提交中文全文并获得 24 kHz、4.56 秒 WAV（合成文件与协议检查），同一 Java 客户端通过本测试环境代理完成实际联网验收：全文任务返回 5.64 秒 WAV，总耗时 12.194 秒（含排队和网络，不是手机或 GPU 纯计算基准）。此项不证明每字读音正确，也不代表所有网络都能访问或随时有额度。取消断开客户端并删除未完成文件；远端已提交的 GPU 计算是否停止由平台决定。参考上传公网测试被自动审批拦截，尚未通过实际公网验收；模拟协议已验证。Modal 与 Kaggle 尚未在用户账号运行；本机 OmniVoice 原有音质问题没有因增加入口而宣称修好。批量、有声书和字幕配音仍走本机原流程。
