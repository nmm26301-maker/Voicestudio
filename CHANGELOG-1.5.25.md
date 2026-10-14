# VoiceStudio 1.5.25-cloud / versionCode 41

- 承接九引擎开发代码：Fish、IndexTTS 2.5、CosyVoice 3、Qwen3-TTS、ElevenLabs、MiniMax Speech、Chatterbox V3、GPT-SoVITS、Cartesia Sonic。
- 按整体示意图整理为音色、合成、任务、作品四个固定入口；设置移动到页头；合成按钮始终可见，默认费用保护保留。
- 新增耳机云朵伙伴、五步流程提示，九引擎显示为四款官方 API / 五款云端 GPU 两组可点选卡片。
- 统一 MainActivity 与声音实验室的文字合成工作台；消除独立合成文本框的静默 12000 字截断与活动重建丢失草稿风险。草稿超过上限时完整保留，提交会提示拆分。
- 修复配置页跳转到创作页的问题；选择引擎现在进入音色配置，外部导航可明确打开音色/合成/作品/设置。
- Cartesia 按当前官方 `2026-08-14` API 版本使用 Bearer 鉴权、字符串 voice ID，标准 WAV 输出；GPT-SoVITS 沿用云端桥接与准确参考原文检查。
- 不增加任何本地模型/权重。原签名发布，versionCode 41 高于已交付的 40。
