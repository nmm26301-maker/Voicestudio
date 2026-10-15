# VoiceStudio 1.5.27.1-cloud · API 修复版

以 1.5.27 为界面基线，取消 1.5.28 界面改动。11 个配色、卡片、背景、进度、录音及动画文件与 1.5.27 完全一致。版本码提高到 45，以便覆盖已经安装过 1.5.28 的设备；不用卸载。

## 已确认的客户端问题

1. 官方平台选择：原下拉框从开源引擎切到 ElevenLabs / MiniMax / Cartesia 时，可能沿用自建网关模式，取错连接和密钥范围。现在商用平台默认官方直连，切换平台时同步路由；仍可明确手动使用自建网关。
2. 参考录音串入：保存录音会更新全局 cloud-profile；创建官方平台任务时原来会继承它，后台随后拒绝非 Fish 的原始录音。现在单条、批量和旧任务处理均按平台能力区分。ElevenLabs / MiniMax / Cartesia 使用平台 Voice ID，Fish 和自建 GPU 的参考录音规则保留。
3. 错误被遮蔽：原 HTTP 错误体被丢弃，MiniMax 的业务码也被统一提示替换。现在显示平台、HTTP 状态和经过密钥脱敏的错误码 / 消息；不显示完整响应体，不自动重发提交状态未知的合成。

## 补充修复

- MiniMax 增加明确的中国站 / 国际站切换，使用各自固定 HTTPS 地址与独立密钥范围，不自动换站重试。可直接查询平台实际可用音色。
- 官方连接检查通过 ElevenLabs / Cartesia 音色 GET 或 MiniMax get_voice 查询核对认证和所选音色，不再把密钥格式检查当作连接成功。查询不提交语音生成，不证明 TTS 权限或余额可用。
- Cartesia 支持官方 generation_config.speed 0.6—1.5；保存配置时按当前语速校验，避免保存成功后才被客户端拦下。
- 从平台账号或密钥页面返回时保留未保存的 API 表单。网关检查会明确说明所选开源模型是否实际配置。

## 使用修复版

覆盖安装后，在音色页重新选择出错的平台，确认官方直连，核对该平台自己的密钥和 Voice ID，再保存并使用。MiniMax 需明确选择与账号对应的站点；中国站不要填国际站密钥。可先点击检查连接，查询认证与音色；再输入短句手动合成。

原有 Fish 合成入口、包名和签名保持。原录音、已存密钥和作品不会由此次升级主动删除。

## 验证范围

76 项 API 专项 host/mock、10 项真实 SecretStore 范围构造检查、既有官方 API 与全文管线回归、Android 编译及原签名升级兼容通过。未访问用户平台账号，没有真实付费合成、余额或手机音质验收。五款开源引擎仍需实际部署的 GPU 服务，不是安装 APK 后自动获得可用接口。

核对来源：
- https://platform.minimax.cn/docs/api-reference/speech-t2a-http
- https://platform.minimax.cn/docs/api-reference/voice-management-get
- https://platform.minimax.io/docs/api-reference/speech-t2a-http
- https://docs.cartesia.ai/api-reference/tts/bytes
- https://docs.cartesia.ai/build-with-cartesia/capability-guides/volume-speed-emotion
- https://elevenlabs.io/docs/api-reference/text-to-speech/convert
