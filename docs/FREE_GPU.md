# 免费 GPU（1.5.10）

手机路径：**音色 → GPU 加速接口 → 免费 GPU**，设置页也有同一入口。页面按平台切换，保留三个平台的注册、额度、控制台与部署链接，不内置账号或共享密钥，不轮换账号绕过额度，不自动切换收费服务。

## Hugging Face ZeroGPU

应用内“配置并使用 ZeroGPU”默认使用官方 `https://k2-fsa-omnivoice.hf.space`。保存后回到创作输入全文，使用真正的 Gradio 6 `/gradio_api/call/_design_fn` → SSE → 音频下载协议；不是把介绍页当成 REST 地址。匿名可留空 Token，个人额度/排队身份由 HF 识别。填写自己的 HF Token 可供授权访问；浏览器登录状态不会自动复制到 Android 请求。

目前官方介绍匿名每天 2 分钟、免费账号每天 5 分钟 GPU 计算时间；不是生成音频时长。额度与政策可能变化，查看 [官方说明](https://huggingface.co/docs/hub/spaces-zerogpu)。账号验证邮箱且注册满 30 天等条件满足后可按官方政策创建自己的 ZeroGPU Space。注册：[huggingface.co/join](https://huggingface.co/join)；Token：[设置](https://huggingface.co/settings/tokens)；官方演示：[OmniVoice](https://huggingface.co/spaces/k2-fsa/OmniVoice)。

自建 Space：把 `server/zerogpu` 内的 app.py、requirements.txt、README.md、LICENSE 放入 Gradio Space，在硬件设置选择有资格使用的 ZeroGPU。模板固定 OmniVoice 源码提交与 Gradio 版本；其他传递依赖与模型 revision 尚未完全锁定。应用填写实际 `https://作者-空间名.hf.space` 运行地址。ZeroGPU 不支持普通 FastAPI 替代 Gradio。

人物音色：在配置框主动勾选“上传所选参考录音”，选择工具 → 音色已保存的人物。合成时只截取该人物记录的 3—10 秒参考片段并上传，同一连续任务各段复用这份上传与文字，不发送整段视频、不自动发送其他录音。未选择时生成自动音色。保存的参考文字同时发送，避免每次重新进行服务器 ASR。原录音缺失、片段无效或已更改上传选择，任务会报错；不会静默切回其他人物。官方 Space 不支持本机采样种子。公共服务管理员的日志、文件留存由该服务管理，按需要使用自己的 Space。

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

## 长文与恢复（1.5.10）

HF 按句子分段（通常每段最多 180 个 UTF-16 单元）；REST 每段最多 2000。文本中空格/换行/标点保留，表情和非 BMP 汉字不会在代理对中间拆开。连续空白可能合并到前段，实际非空朗读内容保持上限。每段均消耗平台额度；自动音色不同段可能变化，选同一人物参考录音更适合长文。

任务目录保存每段完整 WAV、SHA-256 与阶段状态，不保存密钥。恢复复用已完成段；拿到结果地址但下载失败时只 GET 下载。Gradio 6.10 源码会清理断开的队列连接，因此已提交但没拿到结果时不假设可以重放旧 SSE；应用确认后仅重提该未完成段，可能再次耗额度。批量恢复跳过需要确认的此类任务。改变文本/参数/参考文件会拒绝混用旧片段。服务端清理旧音频/参考缓存后，可能需要新任务。删除任务会清理其分段缓存。

HF / Modal 标准单声道 PCM16 WAV 使用原始 PCM 字节合并，保留所有接缝样本。只有一个片段时保留原格式；多段自定义服务须返回一致采样率、单声道 PCM16。最终音频上限 64 MiB，超过时不截断保存。长文输出还会做宽松的极短时长检测；不能用该检测代替试听和转写。


## 1.5.11 下载与进度优化

- 已生成音频遇到超时、连接重置、响应长度不足或 HTTP 502/503/504，自动重试下载最多两次（250/500 ms 退避）。不会自动再次提交合成，也不会重连结果不确定的队列事件流。
- 401/403/404/429、非音频、超出 64 MiB 等不会自动重试；仍保留已有分段和下载地址，便于排查后继续任务。
- “分段已保存”百分比只计已校验并保存的音频；100% 后仍需合并与最终校验，成功保存作品才显示完成。它不是 GPU 推理百分比。
- PCM16/24/32 用固定 32 KiB 缓冲区检查完整数据，float32 仍逐值拒绝 NaN/Infinity。明显过短结果拦截与取消逻辑保留。此项减少手机接收音频后的处理开销，没有测量设备推理速度。
