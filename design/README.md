# VoiceStudio 1.3.2 · 21 屏可编辑设计

画板为 390×960：创作、音色、作品、设置、作品菜单、生成中、系统离线音色、语言搜索、工作台、克隆、队列、转写、字幕、翻译、配音、六模块管理、人声分离、说话人分组、悬浮听写、作品回收站、录音启动。示例任务与识别文本不代表真机结果。

## 导入

1. 解压本包，在 Figma 桌面端打开设计文件。
2. Plugins → Development → Import plugin from manifest，选择 `manifest.json`。
3. 运行 VoiceStudio UI Import。生成“VoiceStudio 1.3.2 · 离线工作室 21屏”；同名存在时只打开，不覆盖。旧版本页面不会被修改。
4. 需要 Noto Sans SC / Noto Sans CJK SC / Source Han Sans SC 的 Regular，建议同时安装 Bold。若 Figma 要求插件 ID，用 Development → New plugin 创建空插件，保留其分配的 ID 并复制本包代码与配置。

## 内容与检查

26 变量、24 文字样式、4 按钮变体、8 导航主组件、1 作品卡组件及 140 个原型跳转。21 个画板共 842 个描述图层、383 个独立文字描述；本地插件模拟生成 494 个文字节点（包含组件与实例）。几何和文字宽高检查通过，PNG 已渲染检查。

插件使用文字、矩形、矢量图标、组件实例和语义变量。PNG 仅为预览，不作为插件画板的填充或背景。SVG 可以拖入，但不同导入路径可能把文字转成路径；可编辑导入优先使用插件。

真实 Figma 编辑器导入未执行；现有目标 https://www.figma.com/design/3OshVJkopPEm5kkAX803XD 此前在线写入受 Starter MCP 配额限制。本次未重试在线写入，也未更新云端文件。设计稿展示功能组织，并不保证与原生 Android 界面逐像素一致。

`make_design.py` 可重新生成 scenes.json/SVG；再用 plugin-template.js 的 __DESIGN__ 与 __ICONS__ 占位符分别填入 scenes.json 与 icon-paths.json，得到 code.js。源代码中的 tests/design_check.py 与 tests/plugin_mock.cjs 提供本地检查。
