# VoiceStudio 1.5.27.1-cloud · API 修复验证

## 当前通过

- 原生 Android 编译、DEX、zipalign、v2/v3 原签名通过；安装身份 com.voice.studio.preview，versionCode 45。
- 从 1.5.27、1.5.28 升级各 31 项兼容检查通过，无需因为版本回退而降低 versionCode。没有执行用户手机安装。
- 11 个界面样式、录音面板和动效文件与 1.5.27 字节一致，已取消 1.5.28 的界面改动。主界面中的变动仅涉及 API 路由、地区、查询、表单保留及错误显示。
- 76 项 API 专项 host/mock 回归通过：平台切换、官方 ID 请求不继承全局录音、Fish 参考录音保留、MiniMax 两地区固定地址和密钥范围、真实查询请求结构、HTTP / 业务码保留、密钥脱敏、Cartesia 语速参数、未知提交状态不重复请求。
- 10 项 SecretStore 真实构造函数检查通过，覆盖全部允许的密钥范围和未知范围拒绝。测试没有调用 Android Keystore，不能替代设备上的加密读写验收。
- 既有 host/mock 回归通过：84 项官方 API、44 项音色与部署配置、16 项健康、1023 项持久化、42 项音频、123 项恢复、10 项 WAV 合并、67 项请求、38 项 prompt、10841 项远程管线、52 项旧 GPU 协议。
- UI 基础 / 对比度检查 137 项通过。原签名恢复归档中的 APK 与交付 APK 相同。

APK SHA256：`22563de8ee4fe229cfb8f4aa0df539fadff77ed1aa1bf60ec3d02b14d154c108`。

## 原故障的代码证据

保存参考录音会更新共享 cloud-profile。旧单条和批量任务把它直接写入官方 API 任务；旧后台对非 Fish 的原始参考录音主动拒绝。现三处都使用平台能力策略，ElevenLabs / MiniMax / Cartesia 走平台 Voice ID，不再继承该文件。

旧引擎下拉框与连接方式未同步，HTTP 错误体被丢弃，官方检查只验证密钥字符格式。现切换平台同步方式、实际查询音色并显示结构化错误，MiniMax 明确选择中国站或国际站。保存配置不会被查询成功冒充为 TTS 实测通过。

## 未验证

没有用户实际平台账户或当前错误日志，因此不能确认其每一次失败都只有上述原因。尚未用真实账号完成 ElevenLabs、MiniMax、Cartesia 合成、余额和权限验收；未连接手机验证安装、密钥读写、录音、播放和保留数据。五款开源模型仍需真实 GPU 部署和可运行服务，不因为 APK 有入口而已可用。

证据：api-fix-java-1.5.27.1.log、build-1.5.27.1.log、ui-baseline-1.5.27.1.json、upgrade-checks-1.5.27.1.json、upgrade-from-1.5.28-to-1.5.27.1.json、release-backup-1.5.27.1.json。
