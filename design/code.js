/* Offline import helper. Creates real editable Figma nodes from the Android design. */
const DESIGN = {
  "colors": {
    "bg": "#F6F7FB",
    "surface": "#FFFFFF",
    "ink": "#202239",
    "muted": "#74788E",
    "accent": "#6154DB",
    "tint": "#EEEBFF",
    "border": "#E6E7EF",
    "success": "#2D8A6E"
  },
  "screens": [
    {
      "name": "01 创作",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "今天，想说些什么？",
          "text": "今天，想说些什么？",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "写下文字，制作属于你的第一段声音。",
          "text": "写下文字，制作属于你的第一段声音。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "配音文本",
          "x": 20,
          "y": 210,
          "w": 350,
          "h": 280,
          "children": [
            {
              "type": "group",
              "name": "文本标题行",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 24,
              "children": [
                {
                  "type": "text",
                  "name": "配音文本",
                  "text": "配音文本",
                  "x": 0,
                  "y": 0,
                  "w": 266,
                  "h": 22.5,
                  "size": 15,
                  "color": "ink",
                  "bold": true
                },
                {
                  "type": "text",
                  "name": "粘贴",
                  "text": "粘贴",
                  "x": 274,
                  "y": 0,
                  "w": 40,
                  "h": 19.5,
                  "size": 13,
                  "color": "accent",
                  "bold": true
                }
              ],
              "color": "surface",
              "radius": 0,
              "layout": "HORIZONTAL",
              "padding": 0,
              "gap": 8
            },
            {
              "type": "text",
              "name": "愿你今天遇见的，\n都是温柔的小事。",
              "text": "愿你今天遇见的，\n都是温柔的小事。",
              "x": 18,
              "y": 71,
              "w": 314,
              "h": 51.0,
              "size": 17,
              "color": "ink",
              "bold": false
            },
            {
              "type": "group",
              "name": "字数与保存",
              "x": 18,
              "y": 182,
              "w": 314,
              "h": 17,
              "children": [
                {
                  "type": "text",
                  "name": "草稿自动保存",
                  "text": "草稿自动保存",
                  "x": 0,
                  "y": 0,
                  "w": 220,
                  "h": 16.5,
                  "size": 11,
                  "color": "muted",
                  "bold": false
                },
                {
                  "type": "text",
                  "name": "20 / 12000",
                  "text": "20 / 12000",
                  "x": 228,
                  "y": 0,
                  "w": 86,
                  "h": 16.5,
                  "size": 11,
                  "color": "muted",
                  "bold": false
                }
              ],
              "color": "surface",
              "radius": 0,
              "layout": "HORIZONTAL",
              "padding": 0,
              "gap": 8
            },
            {
              "type": "group",
              "name": "操作行",
              "x": 18,
              "y": 214,
              "w": 314,
              "h": 48,
              "children": [
                {
                  "type": "button",
                  "name": "示例文本",
                  "text": "示例文本",
                  "x": 0,
                  "y": 0,
                  "w": 153,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                },
                {
                  "type": "button",
                  "name": "清空 / 撤销",
                  "text": "清空 / 撤销",
                  "x": 161,
                  "y": 0,
                  "w": 153,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                }
              ],
              "color": "surface",
              "radius": 0,
              "layout": "HORIZONTAL",
              "padding": 0,
              "gap": 8
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "已选音色",
          "x": 20,
          "y": 504,
          "w": 350,
          "h": 84,
          "children": [
            {
              "type": "rect",
              "name": "图标背景",
              "x": 18,
              "y": 18,
              "w": 46,
              "h": 46,
              "color": "tint",
              "radius": 12,
              "stroke": null
            },
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 25,
              "y": 25,
              "w": 32,
              "h": 32,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "OmniVoice · 中文普通话",
              "text": "OmniVoice · 中文普通话",
              "x": 78,
              "y": 20,
              "w": 225,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "646 种语言 · 本机离线合成",
              "text": "646 种语言 · 本机离线合成",
              "x": 78,
              "y": 47,
              "w": 246,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "text",
              "name": "›",
              "text": "›",
              "x": 317,
              "y": 21,
              "w": 15,
              "h": 37.5,
              "size": 25,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1
        },
        {
          "type": "text",
          "name": "语速",
          "text": "语速",
          "x": 20,
          "y": 608,
          "w": 200,
          "h": 21.0,
          "size": 14,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "1.00×",
          "text": "1.00×",
          "x": 315,
          "y": 608,
          "w": 55,
          "h": 21.0,
          "size": 14,
          "color": "accent",
          "bold": true
        },
        {
          "type": "rect",
          "name": "滑轨",
          "x": 20,
          "y": 642,
          "w": 350,
          "h": 3,
          "color": "border",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "速度",
          "x": 20,
          "y": 642,
          "w": 175,
          "h": 3,
          "color": "accent",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "滑块",
          "x": 187,
          "y": 635,
          "w": 16,
          "h": 16,
          "color": "accent",
          "radius": 8,
          "stroke": null
        },
        {
          "type": "button",
          "name": "生成声音",
          "text": "生成声音",
          "x": 20,
          "y": 680,
          "w": 350,
          "h": 48,
          "primary": true,
          "link": 5,
          "state": "Default"
        },
        {
          "type": "text",
          "name": "模型已导入 · 本机离线合成",
          "text": "模型已导入 · 本机离线合成",
          "x": 20,
          "y": 743,
          "w": 350,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false,
          "align": "center"
        },
        {
          "type": "text",
          "name": "长文自动分段，任务在工作台查看。",
          "text": "长文自动分段，任务在工作台查看。",
          "x": 20,
          "y": 771,
          "w": 350,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        },
        {
          "type": "button",
          "name": "打开离线工作台",
          "text": "打开离线工作台",
          "x": 20,
          "y": 796,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": 8,
          "state": "Default"
        }
      ]
    },
    {
      "name": "02 音色",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "找到你的声音",
          "text": "找到你的声音",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "原模型的语言能力，在手机上离线运行。",
          "text": "原模型的语言能力，在手机上离线运行。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "button",
          "name": "OmniVoice",
          "text": "OmniVoice",
          "x": 20,
          "y": 209,
          "w": 170,
          "h": 48,
          "primary": true,
          "link": null,
          "state": "Default"
        },
        {
          "type": "button",
          "name": "手机语音",
          "text": "手机语音",
          "x": 200,
          "y": 209,
          "w": 170,
          "h": 48,
          "primary": false,
          "link": 6,
          "state": "Default"
        },
        {
          "type": "group",
          "name": "原模型语言",
          "x": 20,
          "y": 282,
          "w": 350,
          "h": 193,
          "children": [
            {
              "type": "text",
              "name": "646 种语言",
              "text": "646 种语言",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 33.0,
              "size": 22,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "完整保留原模型语言目录。\n选择文字的语言，不会自动翻译。",
              "text": "完整保留原模型语言目录。\n选择文字的语言，不会自动翻译。",
              "x": 18,
              "y": 59,
              "w": 314,
              "h": 39.0,
              "size": 13,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "中文普通话 · 选择语言",
              "text": "中文普通话 · 选择语言",
              "x": 18,
              "y": 119,
              "w": 314,
              "h": 48,
              "primary": false,
              "link": 7,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "原生自动音色",
          "x": 20,
          "y": 492,
          "w": 350,
          "h": 124,
          "children": [
            {
              "type": "text",
              "name": "原生自动音色",
              "text": "原生自动音色",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "由 OmniVoice 生成自动音色。\n工作台可创建参考音色与设置声音描述。",
              "text": "由 OmniVoice 生成自动音色。\n工作台可创建参考音色与设置声音描述。",
              "x": 18,
              "y": 57,
              "w": 314,
              "h": 39.0,
              "size": 13,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "离线状态",
          "x": 20,
          "y": 632,
          "w": 350,
          "h": 171,
          "children": [
            {
              "type": "text",
              "name": "模型已导入",
              "text": "模型已导入",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "success",
              "bold": true
            },
            {
              "type": "text",
              "name": "音频由本机计算，不发送文本。",
              "text": "音频由本机计算，不发送文本。",
              "x": 18,
              "y": 54,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "管理离线模型",
              "text": "管理离线模型",
              "x": 18,
              "y": 101,
              "w": 314,
              "h": 48,
              "primary": false,
              "link": 3,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "text",
          "name": "646 种语言尚未逐一实测。",
          "text": "646 种语言尚未逐一实测。",
          "x": 20,
          "y": 819,
          "w": 350,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        }
      ]
    },
    {
      "name": "03 作品",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "我的作品",
          "text": "我的作品",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "已保存 2 段声音 · 本机存储",
          "text": "已保存 2 段声音 · 本机存储",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "作品搜索",
          "x": 20,
          "y": 210,
          "w": 350,
          "h": 48,
          "children": [
            {
              "type": "text",
              "name": "搜索标题、文字或音色",
              "text": "搜索标题、文字或音色",
              "x": 14,
              "y": 13,
              "w": 322,
              "h": 22.5,
              "size": 15,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 13,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "button",
          "name": "全部",
          "text": "全部",
          "x": 20,
          "y": 274,
          "w": 170,
          "h": 48,
          "primary": true,
          "link": null,
          "state": "Default"
        },
        {
          "type": "button",
          "name": "已收藏",
          "text": "已收藏",
          "x": 200,
          "y": 274,
          "w": 170,
          "h": 48,
          "primary": false,
          "link": null,
          "state": "Default"
        },
        {
          "type": "group",
          "name": "作品卡片",
          "x": 20,
          "y": 336,
          "w": 350,
          "h": 250,
          "children": [
            {
              "type": "group",
              "name": "标题行",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 48,
              "children": [
                {
                  "type": "text",
                  "name": "作品标题",
                  "text": "★ 温柔的一天",
                  "x": 0,
                  "y": 10,
                  "w": 254,
                  "h": 24.0,
                  "size": 16,
                  "color": "ink",
                  "bold": true
                },
                {
                  "type": "button",
                  "name": "···",
                  "text": "···",
                  "x": 262,
                  "y": 0,
                  "w": 52,
                  "h": 48,
                  "primary": false,
                  "link": 4,
                  "state": "Default"
                }
              ],
              "color": "surface",
              "radius": 0,
              "layout": "HORIZONTAL",
              "padding": 0,
              "gap": 8
            },
            {
              "type": "text",
              "name": "作品信息",
              "text": "默认音色 · 10-08 08:10",
              "x": 18,
              "y": 77,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "text",
              "name": "音频属性",
              "text": "MP3 · 246.4 KB · 00:15",
              "x": 18,
              "y": 101,
              "w": 314,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false
            },
            {
              "type": "group",
              "name": "操作行",
              "x": 18,
              "y": 133,
              "w": 314,
              "h": 48,
              "children": [
                {
                  "type": "button",
                  "name": "播放",
                  "text": "播放",
                  "x": 0,
                  "y": 0,
                  "w": 99,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                },
                {
                  "type": "button",
                  "name": "导出",
                  "text": "导出",
                  "x": 107,
                  "y": 0,
                  "w": 99,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                },
                {
                  "type": "button",
                  "name": "分享",
                  "text": "分享",
                  "x": 214,
                  "y": 0,
                  "w": 100,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                }
              ],
              "color": "surface",
              "radius": 0,
              "layout": "HORIZONTAL",
              "padding": 0,
              "gap": 8
            },
            {
              "type": "rect",
              "name": "进度轨道",
              "x": 18,
              "y": 207,
              "w": 314,
              "h": 3,
              "color": "border",
              "radius": 2,
              "stroke": null
            },
            {
              "type": "text",
              "name": "点击播放，可拖动定位",
              "text": "点击播放，可拖动定位",
              "x": 18,
              "y": 225,
              "w": 314,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "作品卡片",
          "x": 20,
          "y": 600,
          "w": 350,
          "h": 250,
          "children": [
            {
              "type": "group",
              "name": "标题行",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 48,
              "children": [
                {
                  "type": "text",
                  "name": "作品标题",
                  "text": "视频开场旁白",
                  "x": 0,
                  "y": 10,
                  "w": 254,
                  "h": 24.0,
                  "size": 16,
                  "color": "ink",
                  "bold": true
                },
                {
                  "type": "button",
                  "name": "···",
                  "text": "···",
                  "x": 262,
                  "y": 0,
                  "w": 52,
                  "h": 48,
                  "primary": false,
                  "link": 4,
                  "state": "Default"
                }
              ],
              "color": "surface",
              "radius": 0,
              "layout": "HORIZONTAL",
              "padding": 0,
              "gap": 8
            },
            {
              "type": "text",
              "name": "作品信息",
              "text": "默认音色 · 10-08 08:06",
              "x": 18,
              "y": 77,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "text",
              "name": "音频属性",
              "text": "MP3 · 246.4 KB · 00:15",
              "x": 18,
              "y": 101,
              "w": 314,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false
            },
            {
              "type": "group",
              "name": "操作行",
              "x": 18,
              "y": 133,
              "w": 314,
              "h": 48,
              "children": [
                {
                  "type": "button",
                  "name": "播放",
                  "text": "播放",
                  "x": 0,
                  "y": 0,
                  "w": 99,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                },
                {
                  "type": "button",
                  "name": "导出",
                  "text": "导出",
                  "x": 107,
                  "y": 0,
                  "w": 99,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                },
                {
                  "type": "button",
                  "name": "分享",
                  "text": "分享",
                  "x": 214,
                  "y": 0,
                  "w": 100,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                }
              ],
              "color": "surface",
              "radius": 0,
              "layout": "HORIZONTAL",
              "padding": 0,
              "gap": 8
            },
            {
              "type": "rect",
              "name": "进度轨道",
              "x": 18,
              "y": 207,
              "w": 314,
              "h": 3,
              "color": "border",
              "radius": 2,
              "stroke": null
            },
            {
              "type": "text",
              "name": "点击播放，可拖动定位",
              "text": "点击播放，可拖动定位",
              "x": 18,
              "y": 225,
              "w": 314,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        }
      ]
    },
    {
      "name": "04 设置",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "设置",
          "text": "设置",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "一次导入，离线创作。",
          "text": "一次导入，离线创作。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "离线模型",
          "x": 20,
          "y": 211,
          "w": 350,
          "h": 311,
          "children": [
            {
              "type": "text",
              "name": "OmniVoice 离线模型",
              "text": "OmniVoice 离线模型",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 27.0,
              "size": 18,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "INT8 原生引擎 · 646 种语言",
              "text": "INT8 原生引擎 · 646 种语言",
              "x": 18,
              "y": 54,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            },
            {
              "type": "text",
              "name": "模型约 715 MB，预留 1.5 GB 空间。\n首次导入：同时选择两份分包。",
              "text": "模型约 715 MB，预留 1.5 GB 空间。\n首次导入：同时选择两份分包。",
              "x": 18,
              "y": 90,
              "w": 314,
              "h": 39.0,
              "size": 13,
              "color": "muted",
              "bold": false
            },
            {
              "type": "text",
              "name": "模型已导入 · 校验通过",
              "text": "模型已导入 · 校验通过",
              "x": 18,
              "y": 146,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "success",
              "bold": true
            },
            {
              "type": "button",
              "name": "已导入完整模型",
              "text": "已导入完整模型",
              "x": 18,
              "y": 182,
              "w": 314,
              "h": 48,
              "primary": true,
              "link": null,
              "state": "Disabled"
            },
            {
              "type": "button",
              "name": "移除模型，释放空间",
              "text": "移除模型，释放空间",
              "x": 18,
              "y": 245,
              "w": 314,
              "h": 48,
              "primary": false,
              "link": null,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "本机处理",
          "x": 20,
          "y": 539,
          "w": 350,
          "h": 137,
          "children": [
            {
              "type": "text",
              "name": "本机处理",
              "text": "本机处理",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "无需 API Key、电脑或服务器。\n应用不申请网络权限。\n运行库遥测在初始化前关闭。",
              "text": "无需 API Key、电脑或服务器。\n应用不申请网络权限。\n运行库遥测在初始化前关闭。",
              "x": 18,
              "y": 54,
              "w": 314,
              "h": 58.5,
              "size": 13,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "离线预览说明",
          "x": 20,
          "y": 692,
          "w": 350,
          "h": 141,
          "children": [
            {
              "type": "text",
              "name": "作品与回收站",
              "text": "作品与回收站",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "button",
              "name": "打开作品回收站",
              "text": "打开作品回收站",
              "x": 18,
              "y": 57,
              "w": 314,
              "h": 48,
              "primary": false,
              "link": 19,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": true
        }
      ]
    },
    {
      "name": "05 作品管理",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "我的作品",
          "text": "我的作品",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "已保存 2 段声音 · 本机存储",
          "text": "已保存 2 段声音 · 本机存储",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "作品搜索",
          "x": 20,
          "y": 210,
          "w": 350,
          "h": 48,
          "children": [
            {
              "type": "text",
              "name": "搜索标题、文字或音色",
              "text": "搜索标题、文字或音色",
              "x": 14,
              "y": 13,
              "w": 322,
              "h": 22.5,
              "size": 15,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 13,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "button",
          "name": "全部",
          "text": "全部",
          "x": 20,
          "y": 274,
          "w": 170,
          "h": 48,
          "primary": true,
          "link": null,
          "state": "Default"
        },
        {
          "type": "button",
          "name": "已收藏",
          "text": "已收藏",
          "x": 200,
          "y": 274,
          "w": 170,
          "h": 48,
          "primary": false,
          "link": null,
          "state": "Default"
        },
        {
          "type": "group",
          "name": "作品卡片",
          "x": 20,
          "y": 336,
          "w": 350,
          "h": 250,
          "children": [
            {
              "type": "group",
              "name": "标题行",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 48,
              "children": [
                {
                  "type": "text",
                  "name": "作品标题",
                  "text": "★ 温柔的一天",
                  "x": 0,
                  "y": 10,
                  "w": 254,
                  "h": 24.0,
                  "size": 16,
                  "color": "ink",
                  "bold": true
                },
                {
                  "type": "button",
                  "name": "···",
                  "text": "···",
                  "x": 262,
                  "y": 0,
                  "w": 52,
                  "h": 48,
                  "primary": false,
                  "link": 4,
                  "state": "Default"
                }
              ],
              "color": "surface",
              "radius": 0,
              "layout": "HORIZONTAL",
              "padding": 0,
              "gap": 8
            },
            {
              "type": "text",
              "name": "作品信息",
              "text": "默认音色 · 10-08 08:10",
              "x": 18,
              "y": 77,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "text",
              "name": "音频属性",
              "text": "MP3 · 246.4 KB · 00:15",
              "x": 18,
              "y": 101,
              "w": 314,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false
            },
            {
              "type": "group",
              "name": "操作行",
              "x": 18,
              "y": 133,
              "w": 314,
              "h": 48,
              "children": [
                {
                  "type": "button",
                  "name": "播放",
                  "text": "播放",
                  "x": 0,
                  "y": 0,
                  "w": 99,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                },
                {
                  "type": "button",
                  "name": "导出",
                  "text": "导出",
                  "x": 107,
                  "y": 0,
                  "w": 99,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                },
                {
                  "type": "button",
                  "name": "分享",
                  "text": "分享",
                  "x": 214,
                  "y": 0,
                  "w": 100,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                }
              ],
              "color": "surface",
              "radius": 0,
              "layout": "HORIZONTAL",
              "padding": 0,
              "gap": 8
            },
            {
              "type": "rect",
              "name": "进度轨道",
              "x": 18,
              "y": 207,
              "w": 314,
              "h": 3,
              "color": "border",
              "radius": 2,
              "stroke": null
            },
            {
              "type": "text",
              "name": "点击播放，可拖动定位",
              "text": "点击播放，可拖动定位",
              "x": 18,
              "y": 225,
              "w": 314,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "作品卡片",
          "x": 20,
          "y": 600,
          "w": 350,
          "h": 250,
          "children": [
            {
              "type": "group",
              "name": "标题行",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 48,
              "children": [
                {
                  "type": "text",
                  "name": "作品标题",
                  "text": "视频开场旁白",
                  "x": 0,
                  "y": 10,
                  "w": 254,
                  "h": 24.0,
                  "size": 16,
                  "color": "ink",
                  "bold": true
                },
                {
                  "type": "button",
                  "name": "···",
                  "text": "···",
                  "x": 262,
                  "y": 0,
                  "w": 52,
                  "h": 48,
                  "primary": false,
                  "link": 4,
                  "state": "Default"
                }
              ],
              "color": "surface",
              "radius": 0,
              "layout": "HORIZONTAL",
              "padding": 0,
              "gap": 8
            },
            {
              "type": "text",
              "name": "作品信息",
              "text": "默认音色 · 10-08 08:06",
              "x": 18,
              "y": 77,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "text",
              "name": "音频属性",
              "text": "MP3 · 246.4 KB · 00:15",
              "x": 18,
              "y": 101,
              "w": 314,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false
            },
            {
              "type": "group",
              "name": "操作行",
              "x": 18,
              "y": 133,
              "w": 314,
              "h": 48,
              "children": [
                {
                  "type": "button",
                  "name": "播放",
                  "text": "播放",
                  "x": 0,
                  "y": 0,
                  "w": 99,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                },
                {
                  "type": "button",
                  "name": "导出",
                  "text": "导出",
                  "x": 107,
                  "y": 0,
                  "w": 99,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                },
                {
                  "type": "button",
                  "name": "分享",
                  "text": "分享",
                  "x": 214,
                  "y": 0,
                  "w": 100,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                }
              ],
              "color": "surface",
              "radius": 0,
              "layout": "HORIZONTAL",
              "padding": 0,
              "gap": 8
            },
            {
              "type": "rect",
              "name": "进度轨道",
              "x": 18,
              "y": 207,
              "w": 314,
              "h": 3,
              "color": "border",
              "radius": 2,
              "stroke": null
            },
            {
              "type": "text",
              "name": "点击播放，可拖动定位",
              "text": "点击播放，可拖动定位",
              "x": 18,
              "y": 225,
              "w": 314,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        },
        {
          "type": "rect",
          "name": "对话框遮罩",
          "x": 0,
          "y": 0,
          "w": 390,
          "h": 960,
          "color": "ink",
          "radius": 0,
          "stroke": null,
          "opacity": 0.32
        },
        {
          "type": "group",
          "name": "作品操作菜单",
          "x": 30,
          "y": 290,
          "w": 330,
          "h": 372,
          "children": [
            {
              "type": "text",
              "name": "温柔的一天",
              "text": "温柔的一天",
              "x": 22,
              "y": 20,
              "w": 286,
              "h": 27.0,
              "size": 18,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "重命名",
              "text": "重命名",
              "x": 22,
              "y": 79,
              "w": 286,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": false
            },
            {
              "type": "text",
              "name": "取消收藏",
              "text": "取消收藏",
              "x": 22,
              "y": 130,
              "w": 286,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": false
            },
            {
              "type": "text",
              "name": "查看 / 复制文本",
              "text": "查看 / 复制文本",
              "x": 22,
              "y": 181,
              "w": 286,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": false
            },
            {
              "type": "button",
              "name": "用这段文字再创作",
              "text": "用这段文字再创作",
              "x": 22,
              "y": 228,
              "w": 286,
              "h": 48,
              "primary": false,
              "link": 0,
              "state": "Default"
            },
            {
              "type": "text",
              "name": "移到回收站",
              "text": "移到回收站",
              "x": 22,
              "y": 300,
              "w": 286,
              "h": 24.0,
              "size": 16,
              "color": "accent",
              "bold": false,
              "link": 19
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        }
      ]
    },
    {
      "name": "06 生成中",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "今天，想说些什么？",
          "text": "今天，想说些什么？",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "写下文字，制作属于你的第一段声音。",
          "text": "写下文字，制作属于你的第一段声音。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "配音文本",
          "x": 20,
          "y": 210,
          "w": 350,
          "h": 280,
          "children": [
            {
              "type": "group",
              "name": "文本标题行",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 24,
              "children": [
                {
                  "type": "text",
                  "name": "配音文本",
                  "text": "配音文本",
                  "x": 0,
                  "y": 0,
                  "w": 266,
                  "h": 22.5,
                  "size": 15,
                  "color": "ink",
                  "bold": true
                },
                {
                  "type": "text",
                  "name": "粘贴",
                  "text": "粘贴",
                  "x": 274,
                  "y": 0,
                  "w": 40,
                  "h": 19.5,
                  "size": 13,
                  "color": "accent",
                  "bold": true
                }
              ],
              "color": "surface",
              "radius": 0,
              "layout": "HORIZONTAL",
              "padding": 0,
              "gap": 8
            },
            {
              "type": "text",
              "name": "愿你今天遇见的，\n都是温柔的小事。",
              "text": "愿你今天遇见的，\n都是温柔的小事。",
              "x": 18,
              "y": 71,
              "w": 314,
              "h": 51.0,
              "size": 17,
              "color": "ink",
              "bold": false
            },
            {
              "type": "group",
              "name": "字数与保存",
              "x": 18,
              "y": 182,
              "w": 314,
              "h": 17,
              "children": [
                {
                  "type": "text",
                  "name": "草稿自动保存",
                  "text": "草稿自动保存",
                  "x": 0,
                  "y": 0,
                  "w": 220,
                  "h": 16.5,
                  "size": 11,
                  "color": "muted",
                  "bold": false
                },
                {
                  "type": "text",
                  "name": "20 / 12000",
                  "text": "20 / 12000",
                  "x": 228,
                  "y": 0,
                  "w": 86,
                  "h": 16.5,
                  "size": 11,
                  "color": "muted",
                  "bold": false
                }
              ],
              "color": "surface",
              "radius": 0,
              "layout": "HORIZONTAL",
              "padding": 0,
              "gap": 8
            },
            {
              "type": "group",
              "name": "操作行",
              "x": 18,
              "y": 214,
              "w": 314,
              "h": 48,
              "children": [
                {
                  "type": "button",
                  "name": "示例文本",
                  "text": "示例文本",
                  "x": 0,
                  "y": 0,
                  "w": 153,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                },
                {
                  "type": "button",
                  "name": "清空 / 撤销",
                  "text": "清空 / 撤销",
                  "x": 161,
                  "y": 0,
                  "w": 153,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                }
              ],
              "color": "surface",
              "radius": 0,
              "layout": "HORIZONTAL",
              "padding": 0,
              "gap": 8
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "已选音色",
          "x": 20,
          "y": 504,
          "w": 350,
          "h": 84,
          "children": [
            {
              "type": "rect",
              "name": "图标背景",
              "x": 18,
              "y": 18,
              "w": 46,
              "h": 46,
              "color": "tint",
              "radius": 12,
              "stroke": null
            },
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 25,
              "y": 25,
              "w": 32,
              "h": 32,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "OmniVoice · 中文普通话",
              "text": "OmniVoice · 中文普通话",
              "x": 78,
              "y": 20,
              "w": 225,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "646 种语言 · 本机离线合成",
              "text": "646 种语言 · 本机离线合成",
              "x": 78,
              "y": 47,
              "w": 246,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "text",
              "name": "›",
              "text": "›",
              "x": 317,
              "y": 21,
              "w": 15,
              "h": 37.5,
              "size": 25,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1
        },
        {
          "type": "text",
          "name": "语速",
          "text": "语速",
          "x": 20,
          "y": 608,
          "w": 200,
          "h": 21.0,
          "size": 14,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "1.00×",
          "text": "1.00×",
          "x": 315,
          "y": 608,
          "w": 55,
          "h": 21.0,
          "size": 14,
          "color": "accent",
          "bold": true
        },
        {
          "type": "rect",
          "name": "滑轨",
          "x": 20,
          "y": 642,
          "w": 350,
          "h": 3,
          "color": "border",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "速度",
          "x": 20,
          "y": 642,
          "w": 175,
          "h": 3,
          "color": "accent",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "滑块",
          "x": 187,
          "y": 635,
          "w": 16,
          "h": 16,
          "color": "accent",
          "radius": 8,
          "stroke": null
        },
        {
          "type": "button",
          "name": "取消生成",
          "text": "取消生成",
          "x": 20,
          "y": 680,
          "w": 350,
          "h": 48,
          "primary": true,
          "link": 0,
          "state": "Loading"
        },
        {
          "type": "text",
          "name": "模型已导入 · 本机离线合成",
          "text": "本机合成 · 18 / 32 步 · 已等待 8 秒",
          "x": 20,
          "y": 743,
          "w": 350,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false,
          "align": "center"
        },
        {
          "type": "text",
          "name": "长文自动分段，任务在工作台查看。",
          "text": "长文自动分段，任务在工作台查看。",
          "x": 20,
          "y": 771,
          "w": 350,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        }
      ]
    },
    {
      "name": "07 手机语音",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "找到你的声音",
          "text": "找到你的声音",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "选择配音方式，也可以收藏自己的音色。",
          "text": "选择配音方式，也可以收藏自己的音色。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "button",
          "name": "OmniVoice",
          "text": "OmniVoice",
          "x": 20,
          "y": 209,
          "w": 170,
          "h": 48,
          "primary": false,
          "link": 1,
          "state": "Default"
        },
        {
          "type": "button",
          "name": "手机语音",
          "text": "手机语音",
          "x": 200,
          "y": 209,
          "w": 170,
          "h": 48,
          "primary": true,
          "link": null,
          "state": "Default"
        },
        {
          "type": "group",
          "name": "语言选择",
          "x": 20,
          "y": 282,
          "w": 350,
          "h": 127,
          "children": [
            {
              "type": "text",
              "name": "朗读语言",
              "text": "朗读语言",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "普通话",
              "text": "普通话",
              "x": 18,
              "y": 55,
              "w": 280,
              "h": 22.5,
              "size": 15,
              "color": "ink",
              "bold": false
            },
            {
              "type": "icon",
              "name": "chevron",
              "icon": "chevron",
              "x": 299,
              "y": 54,
              "w": 18,
              "h": 18,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "语言用于选择语音包，不会翻译文字。",
              "text": "语言用于选择语音包，不会翻译文字。",
              "x": 18,
              "y": 91,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "手机音色 / 已选择",
          "x": 20,
          "y": 426,
          "w": 350,
          "h": 174,
          "children": [
            {
              "type": "text",
              "name": "普通话 · 音色 1",
              "text": "普通话 · 音色 1",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "可离线使用 · zh-cn-x-local",
              "text": "可离线使用 · zh-cn-x-local",
              "x": 18,
              "y": 54,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "group",
              "name": "操作行",
              "x": 18,
              "y": 108,
              "w": 314,
              "h": 48,
              "children": [
                {
                  "type": "button",
                  "name": "已选择",
                  "text": "已选择",
                  "x": 0,
                  "y": 0,
                  "w": 153,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                },
                {
                  "type": "button",
                  "name": "试听",
                  "text": "试听",
                  "x": 161,
                  "y": 0,
                  "w": 153,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                }
              ],
              "color": "surface",
              "radius": 0,
              "layout": "HORIZONTAL",
              "padding": 0,
              "gap": 8
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "手机音色 / 可选择",
          "x": 20,
          "y": 616,
          "w": 350,
          "h": 174,
          "children": [
            {
              "type": "text",
              "name": "普通话 · 音色 2",
              "text": "普通话 · 音色 2",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "可离线使用 · zh-cn-y-local",
              "text": "可离线使用 · zh-cn-y-local",
              "x": 18,
              "y": 54,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "group",
              "name": "操作行",
              "x": 18,
              "y": 108,
              "w": 314,
              "h": 48,
              "children": [
                {
                  "type": "button",
                  "name": "选择",
                  "text": "选择",
                  "x": 0,
                  "y": 0,
                  "w": 153,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                },
                {
                  "type": "button",
                  "name": "试听",
                  "text": "试听",
                  "x": 161,
                  "y": 0,
                  "w": 153,
                  "h": 48,
                  "primary": false,
                  "link": null,
                  "state": "Default"
                }
              ],
              "color": "surface",
              "radius": 0,
              "layout": "HORIZONTAL",
              "padding": 0,
              "gap": 8
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        }
      ]
    },
    {
      "name": "08 语言选择",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "选择语言",
          "text": "选择语言",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "646 种原模型语言，支持名称与代码搜索。",
          "text": "646 种原模型语言，支持名称与代码搜索。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "搜索语言",
          "x": 20,
          "y": 211,
          "w": 350,
          "h": 48,
          "children": [
            {
              "type": "text",
              "name": "搜索中文名、英文名或代码",
              "text": "搜索中文名、英文名或代码",
              "x": 16,
              "y": 13,
              "w": 318,
              "h": 21.0,
              "size": 14,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 13,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "text",
          "name": "常用语言",
          "text": "常用语言",
          "x": 20,
          "y": 282,
          "w": 350,
          "h": 21.0,
          "size": 14,
          "color": "ink",
          "bold": true
        },
        {
          "type": "group",
          "name": "语言 / zh",
          "x": 20,
          "y": 319,
          "w": 350,
          "h": 57,
          "children": [
            {
              "type": "text",
              "name": "中文普通话",
              "text": "中文普通话",
              "x": 18,
              "y": 17,
              "w": 230,
              "h": 22.5,
              "size": 15,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "zh",
              "text": "zh",
              "x": 272,
              "y": 17,
              "w": 50,
              "h": 19.5,
              "size": 13,
              "color": "accent",
              "bold": false
            }
          ],
          "color": "tint",
          "radius": 13,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1
        },
        {
          "type": "group",
          "name": "语言 / yue",
          "x": 20,
          "y": 388,
          "w": 350,
          "h": 57,
          "children": [
            {
              "type": "text",
              "name": "粤语",
              "text": "粤语",
              "x": 18,
              "y": 17,
              "w": 230,
              "h": 22.5,
              "size": 15,
              "color": "ink",
              "bold": false
            },
            {
              "type": "text",
              "name": "yue",
              "text": "yue",
              "x": 272,
              "y": 17,
              "w": 50,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 13,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1
        },
        {
          "type": "group",
          "name": "语言 / en",
          "x": 20,
          "y": 457,
          "w": 350,
          "h": 57,
          "children": [
            {
              "type": "text",
              "name": "英语",
              "text": "英语",
              "x": 18,
              "y": 17,
              "w": 230,
              "h": 22.5,
              "size": 15,
              "color": "ink",
              "bold": false
            },
            {
              "type": "text",
              "name": "en",
              "text": "en",
              "x": 272,
              "y": 17,
              "w": 50,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 13,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1
        },
        {
          "type": "group",
          "name": "语言 / ja",
          "x": 20,
          "y": 526,
          "w": 350,
          "h": 57,
          "children": [
            {
              "type": "text",
              "name": "日语",
              "text": "日语",
              "x": 18,
              "y": 17,
              "w": 230,
              "h": 22.5,
              "size": 15,
              "color": "ink",
              "bold": false
            },
            {
              "type": "text",
              "name": "ja",
              "text": "ja",
              "x": 272,
              "y": 17,
              "w": 50,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 13,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1
        },
        {
          "type": "group",
          "name": "语言 / ko",
          "x": 20,
          "y": 595,
          "w": 350,
          "h": 57,
          "children": [
            {
              "type": "text",
              "name": "韩语",
              "text": "韩语",
              "x": 18,
              "y": 17,
              "w": 230,
              "h": 22.5,
              "size": 15,
              "color": "ink",
              "bold": false
            },
            {
              "type": "text",
              "name": "ko",
              "text": "ko",
              "x": 272,
              "y": 17,
              "w": 50,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 13,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1
        },
        {
          "type": "group",
          "name": "语言 / fr",
          "x": 20,
          "y": 664,
          "w": 350,
          "h": 57,
          "children": [
            {
              "type": "text",
              "name": "法语",
              "text": "法语",
              "x": 18,
              "y": 17,
              "w": 230,
              "h": 22.5,
              "size": 15,
              "color": "ink",
              "bold": false
            },
            {
              "type": "text",
              "name": "fr",
              "text": "fr",
              "x": 272,
              "y": 17,
              "w": 50,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 13,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1
        },
        {
          "type": "text",
          "name": "可搜索全部 646 种语言；此处展示常用项。",
          "text": "可搜索全部 646 种语言；此处展示常用项。",
          "x": 20,
          "y": 761,
          "w": 350,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "button",
          "name": "返回音色",
          "text": "返回音色",
          "x": 20,
          "y": 798,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": 1,
          "state": "Default"
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        }
      ]
    },
    {
      "name": "09 离线工作台",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "你的离线工作室",
          "text": "你的离线工作室",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "克隆、转写、翻译与配音，在手机里完成。",
          "text": "克隆、转写、翻译与配音，在手机里完成。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "声音创作",
          "x": 20,
          "y": 210,
          "w": 350,
          "h": 132,
          "children": [
            {
              "type": "text",
              "name": "声音创作",
              "text": "声音创作",
              "x": 18,
              "y": 15,
              "w": 314,
              "h": 25.5,
              "size": 17,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "参考音色、长文与批量任务",
              "text": "参考音色、长文与批量任务",
              "x": 18,
              "y": 49,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "创建音色",
              "text": "创建音色",
              "x": 18,
              "y": 75,
              "w": 153,
              "h": 48,
              "primary": false,
              "link": 9,
              "state": "Default"
            },
            {
              "type": "button",
              "name": "任务与有声书",
              "text": "任务与有声书",
              "x": 179,
              "y": 75,
              "w": 153,
              "h": 48,
              "primary": false,
              "link": 10,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "文字与配音",
          "x": 20,
          "y": 358,
          "w": 350,
          "h": 132,
          "children": [
            {
              "type": "text",
              "name": "文字与配音",
              "text": "文字与配音",
              "x": 18,
              "y": 15,
              "w": 314,
              "h": 25.5,
              "size": 17,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "先转写、校对，再生成目标语言配音",
              "text": "先转写、校对，再生成目标语言配音",
              "x": 18,
              "y": 49,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "转写与字幕",
              "text": "转写与字幕",
              "x": 18,
              "y": 75,
              "w": 153,
              "h": 48,
              "primary": false,
              "link": 11,
              "state": "Default"
            },
            {
              "type": "button",
              "name": "离线翻译",
              "text": "离线翻译",
              "x": 179,
              "y": 75,
              "w": 153,
              "h": 48,
              "primary": false,
              "link": 13,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "音轨与说话人",
          "x": 20,
          "y": 506,
          "w": 350,
          "h": 132,
          "children": [
            {
              "type": "text",
              "name": "音轨与说话人",
              "text": "音轨与说话人",
              "x": 18,
              "y": 15,
              "w": 314,
              "h": 25.5,
              "size": 17,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "双声道分离，按字幕片段识别说话人",
              "text": "双声道分离，按字幕片段识别说话人",
              "x": 18,
              "y": 49,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "人声分离",
              "text": "人声分离",
              "x": 18,
              "y": 75,
              "w": 153,
              "h": 48,
              "primary": false,
              "link": 16,
              "state": "Default"
            },
            {
              "type": "button",
              "name": "说话人分组",
              "text": "说话人分组",
              "x": 179,
              "y": 75,
              "w": 153,
              "h": 48,
              "primary": false,
              "link": 17,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "听写与模型",
          "x": 20,
          "y": 654,
          "w": 350,
          "h": 132,
          "children": [
            {
              "type": "text",
              "name": "听写与模型",
              "text": "听写与模型",
              "x": 18,
              "y": 15,
              "w": 314,
              "h": 25.5,
              "size": 17,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "录音结果可复制，模型按模块导入",
              "text": "录音结果可复制，模型按模块导入",
              "x": 18,
              "y": 49,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "悬浮听写",
              "text": "悬浮听写",
              "x": 18,
              "y": 75,
              "w": 153,
              "h": 48,
              "primary": false,
              "link": 18,
              "state": "Default"
            },
            {
              "type": "button",
              "name": "模型管理",
              "text": "模型管理",
              "x": 179,
              "y": 75,
              "w": 153,
              "h": 48,
              "primary": false,
              "link": 15,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        }
      ]
    },
    {
      "name": "10 创建参考音色",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "留住你的声音",
          "text": "留住你的声音",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "参考音频对应文字，帮助模型还原说话方式。",
          "text": "参考音频对应文字，帮助模型还原说话方式。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "参考文件",
          "x": 20,
          "y": 210,
          "w": 350,
          "h": 139,
          "children": [
            {
              "type": "text",
              "name": "my-reference.wav",
              "text": "my-reference.wav",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 27.0,
              "size": 18,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "单人清晰音频 · 参考 3—10 秒",
              "text": "单人清晰音频 · 参考 3—10 秒",
              "x": 18,
              "y": 57,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "重新选择音频",
              "text": "重新选择音频",
              "x": 18,
              "y": 82,
              "w": 314,
              "h": 48,
              "primary": false,
              "link": null,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "克隆参数",
          "x": 20,
          "y": 365,
          "w": 350,
          "h": 329,
          "children": [
            {
              "type": "text",
              "name": "音色名称",
              "text": "音色名称",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            },
            {
              "type": "text",
              "name": "我的声音",
              "text": "我的声音",
              "x": 18,
              "y": 46,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "起点 0 秒    参考 6 秒",
              "text": "起点 0 秒    参考 6 秒",
              "x": 18,
              "y": 92,
              "w": 314,
              "h": 21.0,
              "size": 14,
              "color": "ink",
              "bold": false
            },
            {
              "type": "text",
              "name": "对应文字",
              "text": "对应文字",
              "x": 18,
              "y": 140,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            },
            {
              "type": "text",
              "name": "欢迎使用 VoiceStudio。\n让每一句话，都拥有自己的声音。",
              "text": "欢迎使用 VoiceStudio。\n让每一句话，都拥有自己的声音。",
              "x": 18,
              "y": 172,
              "w": 314,
              "h": 45.0,
              "size": 15,
              "color": "ink",
              "bold": false
            },
            {
              "type": "text",
              "name": "留空时使用 Whisper 自动转写。",
              "text": "留空时使用 Whisper 自动转写。",
              "x": 18,
              "y": 247,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "text",
              "name": "创建后可选择音色，再生成新文字。",
              "text": "创建后可选择音色，再生成新文字。",
              "x": 18,
              "y": 282,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "button",
          "name": "创建音色并加入队列",
          "text": "创建音色并加入队列",
          "x": 20,
          "y": 715,
          "w": 350,
          "h": 48,
          "primary": true,
          "link": 10,
          "state": "Default"
        },
        {
          "type": "button",
          "name": "返回工作台",
          "text": "返回工作台",
          "x": 20,
          "y": 785,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": 8,
          "state": "Default"
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        }
      ]
    },
    {
      "name": "11 任务队列",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "让创作继续",
          "text": "让创作继续",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "离开页面后继续；进程结束后可手动续做。",
          "text": "离开页面后继续；进程结束后可手动续做。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "button",
          "name": "暂停整个队列",
          "text": "暂停整个队列",
          "x": 20,
          "y": 210,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": null,
          "state": "Default"
        },
        {
          "type": "group",
          "name": "运行中的任务",
          "x": 20,
          "y": 278,
          "w": 350,
          "h": 173,
          "children": [
            {
              "type": "text",
              "name": "长文合成 · 产品介绍",
              "text": "长文合成 · 产品介绍",
              "x": 18,
              "y": 18,
              "w": 270,
              "h": 27.0,
              "size": 18,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "本机合成 · 第 2 段 · 18 / 32 步",
              "text": "本机合成 · 第 2 段 · 18 / 32 步",
              "x": 18,
              "y": 63,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "accent",
              "bold": false
            },
            {
              "type": "rect",
              "name": "进度轨道",
              "x": 18,
              "y": 103,
              "w": 314,
              "h": 6,
              "color": "border",
              "radius": 3,
              "stroke": null
            },
            {
              "type": "rect",
              "name": "进度值",
              "x": 18,
              "y": 103,
              "w": 174,
              "h": 6,
              "color": "accent",
              "radius": 3,
              "stroke": null
            },
            {
              "type": "text",
              "name": "展示任务状态的设计示例",
              "text": "展示任务状态的设计示例",
              "x": 18,
              "y": 130,
              "w": 314,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false
            },
            {
              "type": "text",
              "name": "···",
              "text": "···",
              "x": 290,
              "y": 16,
              "w": 42,
              "h": 21.0,
              "size": 14,
              "color": "accent",
              "bold": true
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "完成的任务",
          "x": 20,
          "y": 467,
          "w": 350,
          "h": 175,
          "children": [
            {
              "type": "text",
              "name": "转写 · interview.mp4",
              "text": "转写 · interview.mp4",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 27.0,
              "size": 18,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "已完成 · 可校对与导出",
              "text": "已完成 · 可校对与导出",
              "x": 18,
              "y": 62,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "success",
              "bold": false
            },
            {
              "type": "button",
              "name": "查看文字与字幕",
              "text": "查看文字与字幕",
              "x": 18,
              "y": 103,
              "w": 314,
              "h": 48,
              "primary": false,
              "link": 12,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "暂停的任务",
          "x": 20,
          "y": 658,
          "w": 350,
          "h": 152,
          "children": [
            {
              "type": "text",
              "name": "有声书 · 第一章",
              "text": "有声书 · 第一章",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 25.5,
              "size": 17,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "已完成章节保留，继续当前章节。",
              "text": "已完成章节保留，继续当前章节。",
              "x": 18,
              "y": 58,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "继续生成",
              "text": "继续生成",
              "x": 18,
              "y": 91,
              "w": 314,
              "h": 48,
              "primary": false,
              "link": null,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        },
        {
          "type": "button",
          "name": "继续所有已暂停任务",
          "text": "继续所有已暂停任务",
          "x": 20,
          "y": 821,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": 10,
          "state": "Default"
        }
      ]
    },
    {
      "name": "12 语音转写",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "听见，再写下来",
          "text": "听见，再写下来",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "Whisper tiny · 语种与合成目录分别管理。",
          "text": "Whisper tiny · 语种与合成目录分别管理。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "选择转写输入",
          "x": 20,
          "y": 210,
          "w": 350,
          "h": 238,
          "children": [
            {
              "type": "text",
              "name": "音频、视频与麦克风",
              "text": "音频、视频与麦克风",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 27.0,
              "size": 18,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "音轨离线解码，识别结果可以校对。",
              "text": "音轨离线解码，识别结果可以校对。",
              "x": 18,
              "y": 56,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "导入音频 / 视频",
              "text": "导入音频 / 视频",
              "x": 18,
              "y": 93,
              "w": 314,
              "h": 48,
              "primary": true,
              "link": 10,
              "state": "Default"
            },
            {
              "type": "button",
              "name": "麦克风录音并转写",
              "text": "麦克风录音并转写",
              "x": 18,
              "y": 155,
              "w": 314,
              "h": 48,
              "primary": false,
              "link": 10,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "识别语言",
          "x": 20,
          "y": 464,
          "w": 350,
          "h": 152,
          "children": [
            {
              "type": "text",
              "name": "自动检测",
              "text": "自动检测",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 27.0,
              "size": 18,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "也可按模型支持范围指定语言。\n识别有误时，请先校对再配音。",
              "text": "也可按模型支持范围指定语言。\n识别有误时，请先校对再配音。",
              "x": 18,
              "y": 63,
              "w": 314,
              "h": 39.0,
              "size": 13,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "button",
          "name": "查看已完成的转写",
          "text": "查看已完成的转写",
          "x": 20,
          "y": 645,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": 12,
          "state": "Default"
        },
        {
          "type": "button",
          "name": "返回工作台",
          "text": "返回工作台",
          "x": 20,
          "y": 715,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": 8,
          "state": "Default"
        },
        {
          "type": "text",
          "name": "646 项合成语言不等于识别语言范围。",
          "text": "646 项合成语言不等于识别语言范围。",
          "x": 20,
          "y": 799,
          "w": 350,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        }
      ]
    },
    {
      "name": "13 字幕校对",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "校对每一句话",
          "text": "校对每一句话",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "编辑时间与文字，支持添加、拆分和删除。",
          "text": "编辑时间与文字，支持添加、拆分和删除。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "字幕条目 1",
          "x": 20,
          "y": 210,
          "w": 350,
          "h": 175,
          "children": [
            {
              "type": "text",
              "name": "00:00:00,000 → 00:00:04,200",
              "text": "00:00:00,000 → 00:00:04,200",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "accent",
              "bold": true
            },
            {
              "type": "text",
              "name": "欢迎使用 VoiceStudio。",
              "text": "欢迎使用 VoiceStudio。",
              "x": 18,
              "y": 58,
              "w": 314,
              "h": 25.5,
              "size": 17,
              "color": "ink",
              "bold": true
            },
            {
              "type": "button",
              "name": "编辑时间与文字",
              "text": "编辑时间与文字",
              "x": 18,
              "y": 103,
              "w": 314,
              "h": 48,
              "primary": false,
              "link": null,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "字幕条目 2",
          "x": 20,
          "y": 401,
          "w": 350,
          "h": 175,
          "children": [
            {
              "type": "text",
              "name": "00:00:04,200 → 00:00:08,600",
              "text": "00:00:04,200 → 00:00:08,600",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "accent",
              "bold": true
            },
            {
              "type": "text",
              "name": "让文字，拥有声音。",
              "text": "让文字，拥有声音。",
              "x": 18,
              "y": 58,
              "w": 314,
              "h": 25.5,
              "size": 17,
              "color": "ink",
              "bold": true
            },
            {
              "type": "button",
              "name": "拆分 / 删除此条",
              "text": "拆分 / 删除此条",
              "x": 18,
              "y": 103,
              "w": 314,
              "h": 48,
              "primary": false,
              "link": null,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "button",
          "name": "导出 TXT / SRT / JSON",
          "text": "导出 TXT / SRT / JSON",
          "x": 20,
          "y": 605,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": null,
          "state": "Default"
        },
        {
          "type": "button",
          "name": "用校对字幕生成配音",
          "text": "用校对字幕生成配音",
          "x": 20,
          "y": 674,
          "w": 350,
          "h": 48,
          "primary": true,
          "link": 14,
          "state": "Default"
        },
        {
          "type": "text",
          "name": "字幕内容为设计示例；导出使用真实识别结果。",
          "text": "字幕内容为设计示例；导出使用真实识别结果。",
          "x": 20,
          "y": 760,
          "w": 350,
          "h": 16.5,
          "size": 11,
          "color": "muted",
          "bold": false
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        },
        {
          "type": "button",
          "name": "说话人分组与音色分配",
          "text": "说话人分组与音色分配",
          "x": 20,
          "y": 795,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": 17,
          "state": "Default"
        }
      ]
    },
    {
      "name": "14 离线翻译",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "让语言互相理解",
          "text": "让语言互相理解",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "NLLB 200 · 202 个语言 / 文字体系选项",
          "text": "NLLB 200 · 202 个语言 / 文字体系选项",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "源语言与目标语言",
          "x": 20,
          "y": 210,
          "w": 350,
          "h": 149,
          "children": [
            {
              "type": "text",
              "name": "原文语言",
              "text": "原文语言",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "text",
              "name": "英语 · eng_Latn",
              "text": "英语 · eng_Latn",
              "x": 18,
              "y": 45,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "目标语言",
              "text": "目标语言",
              "x": 18,
              "y": 83,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "text",
              "name": "简体中文 · zho_Hans",
              "text": "简体中文 · zho_Hans",
              "x": 18,
              "y": 110,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "原文",
          "x": 20,
          "y": 375,
          "w": 350,
          "h": 164,
          "children": [
            {
              "type": "text",
              "name": "Hello, this is VoiceStudio.",
              "text": "Hello, this is VoiceStudio.",
              "x": 18,
              "y": 24,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": false
            },
            {
              "type": "text",
              "name": "请输入较短段落，长文请分开翻译。",
              "text": "请输入较短段落，长文请分开翻译。",
              "x": 18,
              "y": 114,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "翻译结果",
          "x": 20,
          "y": 555,
          "w": 350,
          "h": 116,
          "children": [
            {
              "type": "text",
              "name": "你好，这里是 VoiceStudio。",
              "text": "你好，这里是 VoiceStudio。",
              "x": 18,
              "y": 24,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "结果示例 · 可复制或导出 TXT",
              "text": "结果示例 · 可复制或导出 TXT",
              "x": 18,
              "y": 73,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "button",
          "name": "离线翻译并加入队列",
          "text": "离线翻译并加入队列",
          "x": 20,
          "y": 694,
          "w": 350,
          "h": 48,
          "primary": true,
          "link": 10,
          "state": "Default"
        },
        {
          "type": "button",
          "name": "返回工作台",
          "text": "返回工作台",
          "x": 20,
          "y": 768,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": 8,
          "state": "Default"
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        }
      ]
    },
    {
      "name": "15 字幕配音",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "为画面，换一种声音",
          "text": "为画面，换一种声音",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "先校对字幕，再生成对齐时间轴的音轨。",
          "text": "先校对字幕，再生成对齐时间轴的音轨。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "配音输入",
          "x": 20,
          "y": 210,
          "w": 350,
          "h": 151,
          "children": [
            {
              "type": "text",
              "name": "interview.mp4",
              "text": "interview.mp4",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 27.0,
              "size": 18,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "使用校对后的字幕与时间轴。\n配音音轨替换原始背景音。",
              "text": "使用校对后的字幕与时间轴。\n配音音轨替换原始背景音。",
              "x": 18,
              "y": 63,
              "w": 314,
              "h": 39.0,
              "size": 13,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "配音参数",
          "x": 20,
          "y": 377,
          "w": 350,
          "h": 279,
          "children": [
            {
              "type": "text",
              "name": "音色 · 首页当前音色",
              "text": "音色 · 首页当前音色",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "直接配音使用首页当前语言。",
              "text": "直接配音使用首页当前语言。",
              "x": 18,
              "y": 58,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            },
            {
              "type": "text",
              "name": "先翻译成目标语言    □",
              "text": "先翻译成目标语言    □",
              "x": 18,
              "y": 104,
              "w": 314,
              "h": 22.5,
              "size": 15,
              "color": "ink",
              "bold": false
            },
            {
              "type": "text",
              "name": "原文 英语  →  目标 简体中文",
              "text": "原文 英语  →  目标 简体中文",
              "x": 18,
              "y": 146,
              "w": 314,
              "h": 21.0,
              "size": 14,
              "color": "ink",
              "bold": false
            },
            {
              "type": "text",
              "name": "输出 MP4，保留原始画面    □",
              "text": "输出 MP4，保留原始画面    □",
              "x": 18,
              "y": 194,
              "w": 314,
              "h": 22.5,
              "size": 15,
              "color": "ink",
              "bold": false
            },
            {
              "type": "text",
              "name": "也可单独导出配音 WAV 与字幕。",
              "text": "也可单独导出配音 WAV 与字幕。",
              "x": 18,
              "y": 242,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "button",
          "name": "生成配音并加入队列",
          "text": "生成配音并加入队列",
          "x": 20,
          "y": 679,
          "w": 350,
          "h": 48,
          "primary": true,
          "link": 10,
          "state": "Default"
        },
        {
          "type": "button",
          "name": "返回字幕校对",
          "text": "返回字幕校对",
          "x": 20,
          "y": 754,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": 12,
          "state": "Default"
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        }
      ]
    },
    {
      "name": "16 模块模型",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "一次导入，离线使用",
          "text": "一次导入，离线使用",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "选择同一模块的全部分包，无需电脑拼接。",
          "text": "选择同一模块的全部分包，无需电脑拼接。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "OmniVoice · 646 项合成目录",
          "x": 20,
          "y": 210,
          "w": 350,
          "h": 72,
          "children": [
            {
              "type": "text",
              "name": "OmniVoice · 646 项合成目录",
              "text": "OmniVoice · 646 项合成目录",
              "x": 18,
              "y": 10,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "语音、长文、有声书与配音",
              "text": "语音、长文、有声书与配音",
              "x": 18,
              "y": 40,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "声音克隆编码器",
          "x": 20,
          "y": 294,
          "w": 350,
          "h": 72,
          "children": [
            {
              "type": "text",
              "name": "声音克隆编码器",
              "text": "声音克隆编码器",
              "x": 18,
              "y": 10,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "单人参考音频 → 保存参考音色",
              "text": "单人参考音频 → 保存参考音色",
              "x": 18,
              "y": 40,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "Whisper tiny · 99 项识别目录",
          "x": 20,
          "y": 378,
          "w": 350,
          "h": 72,
          "children": [
            {
              "type": "text",
              "name": "Whisper tiny · 99 项识别目录",
              "text": "Whisper tiny · 99 项识别目录",
              "x": 18,
              "y": 10,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "转写、麦克风与悬浮听写",
              "text": "转写、麦克风与悬浮听写",
              "x": 18,
              "y": 40,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "NLLB · 202 项语言 / 文字体系",
          "x": 20,
          "y": 462,
          "w": 350,
          "h": 72,
          "children": [
            {
              "type": "text",
              "name": "NLLB · 202 项语言 / 文字体系",
              "text": "NLLB · 202 项语言 / 文字体系",
              "x": 18,
              "y": 10,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "短段翻译；与合成目录分别管理",
              "text": "短段翻译；与合成目录分别管理",
              "x": 18,
              "y": 40,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "MDX-Net · 双声道人声分离",
          "x": 20,
          "y": 546,
          "w": 350,
          "h": 72,
          "children": [
            {
              "type": "text",
              "name": "MDX-Net · 双声道人声分离",
              "text": "MDX-Net · 双声道人声分离",
              "x": 18,
              "y": 10,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "导出人声 WAV 与伴奏 WAV",
              "text": "导出人声 WAV 与伴奏 WAV",
              "x": 18,
              "y": 40,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "CAM++ · 说话人分组",
          "x": 20,
          "y": 630,
          "w": 350,
          "h": 72,
          "children": [
            {
              "type": "text",
              "name": "CAM++ · 说话人分组",
              "text": "CAM++ · 说话人分组",
              "x": 18,
              "y": 10,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "按字幕片段聚类，可手动修正",
              "text": "按字幕片段聚类，可手动修正",
              "x": 18,
              "y": 40,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "text",
          "name": "保持分卷原名，一次选择同模块全部分卷。",
          "text": "保持分卷原名，一次选择同模块全部分卷。",
          "x": 20,
          "y": 735,
          "w": 350,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "button",
          "name": "返回工作台",
          "text": "返回工作台",
          "x": 20,
          "y": 782,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": 8,
          "state": "Default"
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": true
        }
      ]
    },
    {
      "name": "17 人声分离",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "拆开一段声音",
          "text": "拆开一段声音",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "在本机输出人声与伴奏，两份双声道 WAV。",
          "text": "在本机输出人声与伴奏，两份双声道 WAV。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "输入音轨",
          "x": 20,
          "y": 210,
          "w": 350,
          "h": 143,
          "children": [
            {
              "type": "text",
              "name": "song.wav",
              "text": "song.wav",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 27.0,
              "size": 18,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "音乐或视频音轨 · 保留左右声道",
              "text": "音乐或视频音轨 · 保留左右声道",
              "x": 18,
              "y": 57,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "选择音频 / 视频",
              "text": "选择音频 / 视频",
              "x": 18,
              "y": 81,
              "w": 314,
              "h": 48,
              "primary": false,
              "link": null,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "分离结果",
          "x": 20,
          "y": 371,
          "w": 350,
          "h": 222,
          "children": [
            {
              "type": "text",
              "name": "人声与伴奏",
              "text": "人声与伴奏",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 27.0,
              "size": 18,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "完成后分别试听、保存与分享。",
              "text": "完成后分别试听、保存与分享。",
              "x": 18,
              "y": 57,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "试听 / 导出人声",
              "text": "试听 / 导出人声",
              "x": 18,
              "y": 95,
              "w": 314,
              "h": 48,
              "primary": false,
              "link": null,
              "state": "Default"
            },
            {
              "type": "button",
              "name": "试听 / 导出伴奏",
              "text": "试听 / 导出伴奏",
              "x": 18,
              "y": 155,
              "w": 314,
              "h": 48,
              "primary": false,
              "link": null,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "text",
          "name": "可能残留音乐或人声，输出需要试听。",
          "text": "可能残留音乐或人声，输出需要试听。",
          "x": 20,
          "y": 622,
          "w": 350,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "button",
          "name": "开始分离并加入队列",
          "text": "开始分离并加入队列",
          "x": 20,
          "y": 674,
          "w": 350,
          "h": 48,
          "primary": true,
          "link": 10,
          "state": "Default"
        },
        {
          "type": "button",
          "name": "返回工作台",
          "text": "返回工作台",
          "x": 20,
          "y": 752,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": 8,
          "state": "Default"
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        }
      ]
    },
    {
      "name": "18 说话人分组",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "为每个人，选一种声音",
          "text": "为每个人，选一种声音",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "按转写片段聚类，分组与音色都可以修改。",
          "text": "按转写片段聚类，分组与音色都可以修改。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "分组设置",
          "x": 20,
          "y": 210,
          "w": 350,
          "h": 149,
          "children": [
            {
              "type": "text",
              "name": "聚类阈值 · 0.55",
              "text": "聚类阈值 · 0.55",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 25.5,
              "size": 17,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "先校对字幕；同段多人时先手动拆分。",
              "text": "先校对字幕；同段多人时先手动拆分。",
              "x": 18,
              "y": 55,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "运行说话人分组",
              "text": "运行说话人分组",
              "x": 18,
              "y": 88,
              "w": 314,
              "h": 48,
              "primary": true,
              "link": 10,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "说话人 1",
          "x": 20,
          "y": 378,
          "w": 350,
          "h": 143,
          "children": [
            {
              "type": "text",
              "name": "说话人 1 · 片段 1、3、5",
              "text": "说话人 1 · 片段 1、3、5",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "参考音色 · 我的声音",
              "text": "参考音色 · 我的声音",
              "x": 18,
              "y": 58,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "修改分组 / 指定音色",
              "text": "修改分组 / 指定音色",
              "x": 18,
              "y": 82,
              "w": 314,
              "h": 48,
              "primary": false,
              "link": 12,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "说话人 2",
          "x": 20,
          "y": 537,
          "w": 350,
          "h": 143,
          "children": [
            {
              "type": "text",
              "name": "说话人 2 · 片段 2、4",
              "text": "说话人 2 · 片段 2、4",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "原生自动音色",
              "text": "原生自动音色",
              "x": 18,
              "y": 58,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "修改分组 / 指定音色",
              "text": "修改分组 / 指定音色",
              "x": 18,
              "y": 82,
              "w": 314,
              "h": 48,
              "primary": false,
              "link": 12,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "button",
          "name": "校对后生成多音色配音",
          "text": "校对后生成多音色配音",
          "x": 20,
          "y": 704,
          "w": 350,
          "h": 48,
          "primary": true,
          "link": 14,
          "state": "Default"
        },
        {
          "type": "button",
          "name": "返回字幕",
          "text": "返回字幕",
          "x": 20,
          "y": 778,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": 12,
          "state": "Default"
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        }
      ]
    },
    {
      "name": "19 悬浮听写",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "随手说，随手记",
          "text": "随手说，随手记",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "主动录音，离线识别，再复制到需要的地方。",
          "text": "主动录音，离线识别，再复制到需要的地方。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "录音权限",
          "x": 20,
          "y": 210,
          "w": 350,
          "h": 149,
          "children": [
            {
              "type": "text",
              "name": "麦克风与悬浮窗权限",
              "text": "麦克风与悬浮窗权限",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 25.5,
              "size": 17,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "在工作台启动；麦克风录音需要授权。\n蓝牙耳机可在录音选项中选择。",
              "text": "在工作台启动；麦克风录音需要授权。\n蓝牙耳机可在录音选项中选择。",
              "x": 18,
              "y": 57,
              "w": 314,
              "h": 39.0,
              "size": 13,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "悬浮小窗示例",
          "x": 20,
          "y": 377,
          "w": 350,
          "h": 274,
          "children": [
            {
              "type": "text",
              "name": "VoiceStudio · 听写",
              "text": "VoiceStudio · 听写",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 25.5,
              "size": 17,
              "color": "ink",
              "bold": true
            },
            {
              "type": "button",
              "name": "点击开始 / 停止录音",
              "text": "点击开始 / 停止录音",
              "x": 18,
              "y": 62,
              "w": 314,
              "h": 48,
              "primary": true,
              "link": 20,
              "state": "Default"
            },
            {
              "type": "text",
              "name": "今天想记录一个新的想法。",
              "text": "今天想记录一个新的想法。",
              "x": 18,
              "y": 135,
              "w": 314,
              "h": 24.0,
              "size": 16,
              "color": "ink",
              "bold": false
            },
            {
              "type": "text",
              "name": "识别结果可校对，再自行粘贴。",
              "text": "识别结果可校对，再自行粘贴。",
              "x": 18,
              "y": 180,
              "w": 314,
              "h": 18.0,
              "size": 12,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "复制文字",
              "text": "复制文字",
              "x": 18,
              "y": 211,
              "w": 153,
              "h": 48,
              "primary": false,
              "link": null,
              "state": "Default"
            },
            {
              "type": "button",
              "name": "再录一次",
              "text": "再录一次",
              "x": 179,
              "y": 211,
              "w": 153,
              "h": 48,
              "primary": false,
              "link": null,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "text",
          "name": "录音启动说明",
          "text": "打开录音小窗后启动麦克风，再自动返回。",
          "x": 20,
          "y": 681,
          "w": 350,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "button",
          "name": "启动悬浮听写",
          "text": "启动悬浮听写",
          "x": 20,
          "y": 720,
          "w": 350,
          "h": 48,
          "primary": true,
          "link": null,
          "state": "Default"
        },
        {
          "type": "button",
          "name": "返回工作台",
          "text": "返回工作台",
          "x": 20,
          "y": 789,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": 8,
          "state": "Default"
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        }
      ]
    },
    {
      "name": "20 作品回收站",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "作品回收站",
          "text": "作品回收站",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "音频保留在手机里，恢复后回到作品列表。",
          "text": "音频保留在手机里，恢复后回到作品列表。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "已移除作品",
          "x": 20,
          "y": 210,
          "w": 350,
          "h": 191,
          "children": [
            {
              "type": "text",
              "name": "温柔的一天",
              "text": "温柔的一天",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 27.0,
              "size": 18,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "普通话 · WAV · 已移到回收站",
              "text": "普通话 · WAV · 已移到回收站",
              "x": 18,
              "y": 61,
              "w": 314,
              "h": 19.5,
              "size": 13,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "恢复到作品",
              "text": "恢复到作品",
              "x": 18,
              "y": 103,
              "w": 153,
              "h": 48,
              "primary": true,
              "link": 2,
              "state": "Default"
            },
            {
              "type": "button",
              "name": "彻底删除",
              "text": "彻底删除",
              "x": 179,
              "y": 103,
              "w": 153,
              "h": 48,
              "primary": false,
              "link": null,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "group",
          "name": "删除保护说明",
          "x": 20,
          "y": 422,
          "w": 350,
          "h": 174,
          "children": [
            {
              "type": "text",
              "name": "彻底删除前，再确认",
              "text": "彻底删除前，再确认",
              "x": 18,
              "y": 18,
              "w": 314,
              "h": 25.5,
              "size": 17,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "移除内部音频与对应任务。\n已导出的文件保留。\n队列运行时，先暂停并等待结束。",
              "text": "移除内部音频与对应任务。\n已导出的文件保留。\n队列运行时，先暂停并等待结束。",
              "x": 18,
              "y": 60,
              "w": 314,
              "h": 58.5,
              "size": 13,
              "color": "muted",
              "bold": false
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "text",
          "name": "作品与状态均为设计示例。",
          "text": "作品与状态均为设计示例。",
          "x": 20,
          "y": 628,
          "w": 350,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "button",
          "name": "返回设置",
          "text": "返回设置",
          "x": 20,
          "y": 687,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": 3,
          "state": "Default"
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": true
        }
      ]
    },
    {
      "name": "21 录音启动",
      "w": 390,
      "h": 960,
      "children": [
        {
          "type": "text",
          "name": "9:41",
          "text": "9:41",
          "x": 22,
          "y": 5,
          "w": 70,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "●",
          "text": "●",
          "x": 325,
          "y": 5,
          "w": 14,
          "h": 18.0,
          "size": 12,
          "color": "ink",
          "bold": false
        },
        {
          "type": "icon",
          "name": "battery",
          "icon": "battery",
          "x": 345,
          "y": 6,
          "w": 19,
          "h": 19,
          "color": "ink"
        },
        {
          "type": "text",
          "name": "VoiceStudio",
          "text": "VoiceStudio",
          "x": 22,
          "y": 41,
          "w": 240,
          "h": 34.5,
          "size": 23,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "让文字，拥有声音",
          "text": "让文字，拥有声音",
          "x": 22,
          "y": 73,
          "w": 270,
          "h": 18.0,
          "size": 12,
          "color": "muted",
          "bold": false
        },
        {
          "type": "icon",
          "name": "wave",
          "icon": "wave",
          "x": 330,
          "y": 43,
          "w": 39,
          "h": 39,
          "color": "accent"
        },
        {
          "type": "text",
          "name": "打开麦克风",
          "text": "打开麦克风",
          "x": 20,
          "y": 121,
          "w": 350,
          "h": 42.0,
          "size": 28,
          "color": "ink",
          "bold": true
        },
        {
          "type": "text",
          "name": "启动成功后，自动回到悬浮按钮。",
          "text": "启动成功后，自动回到悬浮按钮。",
          "x": 20,
          "y": 166,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "rect",
          "name": "底部导航",
          "x": 0,
          "y": 863,
          "w": 390,
          "h": 73,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "系统导航背景",
          "x": 0,
          "y": 936,
          "w": 390,
          "h": 24,
          "color": "surface",
          "radius": 0,
          "stroke": null
        },
        {
          "type": "rect",
          "name": "手势条",
          "x": 155,
          "y": 946,
          "w": 80,
          "h": 4,
          "color": "ink",
          "radius": 2,
          "stroke": null
        },
        {
          "type": "group",
          "name": "录音启动小窗",
          "x": 30,
          "y": 301,
          "w": 330,
          "h": 235,
          "children": [
            {
              "type": "text",
              "name": "VoiceStudio · 悬浮听写",
              "text": "VoiceStudio · 悬浮听写",
              "x": 20,
              "y": 20,
              "w": 290,
              "h": 27.0,
              "size": 18,
              "color": "ink",
              "bold": true
            },
            {
              "type": "text",
              "name": "正在打开麦克风…",
              "text": "正在打开麦克风…",
              "x": 20,
              "y": 80,
              "w": 290,
              "h": 22.5,
              "size": 15,
              "color": "muted",
              "bold": false
            },
            {
              "type": "button",
              "name": "取消",
              "text": "取消",
              "x": 20,
              "y": 150,
              "w": 290,
              "h": 48,
              "primary": false,
              "link": 18,
              "state": "Default"
            }
          ],
          "color": "surface",
          "radius": 22,
          "layout": null,
          "padding": 0,
          "gap": 0
        },
        {
          "type": "text",
          "name": "需要麦克风权限；可随时取消。",
          "text": "需要麦克风权限；可随时取消。",
          "x": 20,
          "y": 570,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "text",
          "name": "录音开始后，悬浮按钮变为“结束”。",
          "text": "录音开始后，悬浮按钮变为“结束”。",
          "x": 20,
          "y": 609,
          "w": 350,
          "h": 19.5,
          "size": 13,
          "color": "muted",
          "bold": false
        },
        {
          "type": "button",
          "name": "返回悬浮听写",
          "text": "返回悬浮听写",
          "x": 20,
          "y": 693,
          "w": 350,
          "h": 48,
          "primary": false,
          "link": 18,
          "state": "Default"
        },
        {
          "type": "group",
          "name": "导航 / 创作",
          "x": 8.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "edit",
              "icon": "edit",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "accent"
            },
            {
              "type": "text",
              "name": "创作",
              "text": "创作",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "accent",
              "bold": true,
              "align": "center"
            }
          ],
          "color": "tint",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 0,
          "navIcon": "edit",
          "selected": true
        },
        {
          "type": "group",
          "name": "导航 / 音色",
          "x": 101.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "wave",
              "icon": "wave",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "音色",
              "text": "音色",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 1,
          "navIcon": "wave",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 作品",
          "x": 195.0,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "folder",
              "icon": "folder",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "作品",
              "text": "作品",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 2,
          "navIcon": "folder",
          "selected": false
        },
        {
          "type": "group",
          "name": "导航 / 设置",
          "x": 288.5,
          "y": 871,
          "w": 93.5,
          "h": 57,
          "children": [
            {
              "type": "icon",
              "name": "settings",
              "icon": "settings",
              "x": 35.25,
              "y": 5,
              "w": 23,
              "h": 23,
              "color": "muted"
            },
            {
              "type": "text",
              "name": "设置",
              "text": "设置",
              "x": 0,
              "y": 34,
              "w": 93.5,
              "h": 16.5,
              "size": 11,
              "color": "muted",
              "bold": false,
              "align": "center"
            }
          ],
          "color": "surface",
          "radius": 15,
          "layout": null,
          "padding": 0,
          "gap": 0,
          "link": 3,
          "navIcon": "settings",
          "selected": false
        }
      ]
    }
  ]
};
const ICON_PATHS = {"battery": "M3 8H26V24H3Z M29 13V19 M7 12H21V20H7Z", "chevron": "M7 12L16 21L25 12", "wave": "M6 13V19 M11 8.5V23.5 M16 4.5V27.5 M21 8.5V23.5 M26 13V19", "folder": "M4 10V7H13L16 10H28V26H4Z M4 13H28", "edit": "M9 5H23Q26 5 26 9V23Q26 27 22 27H9Q5 27 5 23V9Q5 5 9 5 M11 20L23 8 M11 20L10 24L14 23", "settings": "M16 3V6 M16 26V29 M3 16H6 M26 16H29 M7 7L9 9 M23 23L25 25 M7 25L9 23 M23 9L25 7 M25 16A9 9 0 1 0 7 16A9 9 0 1 0 25 16 M19 16A3 3 0 1 0 13 16A3 3 0 1 0 19 16"};
async function buildVoiceStudio() {
  const existing = figma.root.children.find(p=>p.name==='VoiceStudio 1.3.2 · 离线工作室 21屏');
  if(existing){await figma.setCurrentPageAsync(existing);figma.viewport.scrollAndZoomIntoView(existing.children);return {status:'existing',pageId:existing.id};}
  const fonts=await figma.listAvailableFontsAsync();
  const families=['Noto Sans SC','Noto Sans CJK SC','Source Han Sans SC'];
  const family=families.find(f=>fonts.some(x=>x.fontName.family===f&&x.fontName.style==='Regular'));
  if(!family)throw new Error('请安装 Noto Sans SC 字体后重试。');
  const regular={family,style:'Regular'};
  const boldEntry=fonts.find(f=>f.fontName.family===family&&f.fontName.style==='Bold');
  const bold=boldEntry?boldEntry.fontName:regular;
  await Promise.all([figma.loadFontAsync(regular),figma.loadFontAsync(bold)]);
  const page=figma.createPage();page.name='VoiceStudio 1.3.2 · 导入中';await figma.setCurrentPageAsync(page);
  const ids=[page.id],vars={},buttons={},links=[],navComponents={};let cardComponent=null,cardProperties={};
  const androidColors={bg:'MainActivity.BG',surface:'Color.WHITE',ink:'MainActivity.INK',muted:'MainActivity.MUTED',accent:'MainActivity.PURPLE',tint:'MainActivity.PALE',border:'MainActivity.LINE',success:'MainActivity.SUCCESS'};
  const color=h=>({r:parseInt(h.slice(1,3),16)/255,g:parseInt(h.slice(3,5),16)/255,b:parseInt(h.slice(5,7),16)/255});
  const primitives=figma.variables.createVariableCollection('VoiceStudio / Primitives');primitives.renameMode(primitives.modes[0].modeId,'Value');
  const semantic=figma.variables.createVariableCollection('VoiceStudio / Semantic');semantic.renameMode(semantic.modes[0].modeId,'Light');
  for(const [name,hex] of Object.entries(DESIGN.colors)){
    const p=figma.variables.createVariable('palette/'+name,primitives,'COLOR');p.setValueForMode(primitives.modes[0].modeId,color(hex));p.scopes=[];p.hiddenFromPublishing=true;p.setVariableCodeSyntax('ANDROID',androidColors[name]);p.setVariableCodeSyntax('WEB','var(--vs-palette-'+name+')');
    const v=figma.variables.createVariable('color/'+name,semantic,'COLOR');v.setValueForMode(semantic.modes[0].modeId,figma.variables.createVariableAlias(p));v.scopes=['FRAME_FILL','SHAPE_FILL','TEXT_FILL','STROKE_COLOR'];v.setVariableCodeSyntax('ANDROID',androidColors[name]);v.setVariableCodeSyntax('WEB','var(--vs-'+name+')');vars[name]=v;
  }
  for(const [name,value,scope] of [['radius/card',22,'CORNER_RADIUS'],['radius/button',15,'CORNER_RADIUS'],['space/padding',14,'GAP'],['space/section',20,'GAP'],['space/control',8,'GAP'],['space/icon-label',6,'GAP'],['space/card',18,'GAP'],['space/compact',9,'GAP'],['radius/input',13,'CORNER_RADIUS'],['radius/voice',20,'CORNER_RADIUS']]){
    const v=figma.variables.createVariable(name,semantic,'FLOAT');v.setValueForMode(semantic.modes[0].modeId,value);v.scopes=[scope];v.setVariableCodeSyntax('ANDROID','dp('+value+')');v.setVariableCodeSyntax('WEB','var(--vs-'+name.replace(/\//g,'-')+')');vars[name]=v;
  }
  function paint(name){return figma.variables.setBoundVariableForPaint({type:'SOLID',color:color(DESIGN.colors[name])},'color',vars[name]);}
  const styles={};
  for(const size of [11,12,13,14,15,16,17,18,22,23,25,28])for(const isBold of [false,true]){
    const style=figma.createTextStyle();style.name='VoiceStudio / '+size+' / '+(isBold?'Bold':'Regular');style.fontName=isBold?bold:regular;style.fontSize=size;style.lineHeight={unit:'PERCENT',value:150};styles[size+'-'+isBold]=style;
  }
  const componentArea=figma.createFrame();page.appendChild(componentArea);ids.push(componentArea.id);componentArea.name='组件 / Button';componentArea.resize(740,1450);componentArea.x=100;componentArea.y=1080;componentArea.fills=[];
  const variants=[];
  for(let i=0;i<4;i++){
    const type=i===3?'Secondary':'Primary',state=['Default','Loading','Disabled','Default'][i];
    const c=figma.createComponent();ids.push(c.id);componentArea.appendChild(c);c.name='Style='+type+', State='+state;c.resize(314,48);c.layoutMode='HORIZONTAL';c.primaryAxisSizingMode='FIXED';c.counterAxisSizingMode='FIXED';c.primaryAxisAlignItems='CENTER';c.counterAxisAlignItems='CENTER';c.cornerRadius=15;c.setBoundVariable('cornerRadius',vars['radius/button']);c.fills=[paint(type==='Primary'?'accent':'tint')];c.opacity=state==='Disabled'?.5:1;c.description='VoiceStudio 配音工作室的操作按钮。Label 为可编辑文字，状态为 Default、Loading 或 Disabled。';
    const label=figma.createText();ids.push(label.id);c.appendChild(label);label.fontName=bold;label.fontSize=15;await label.setTextStyleIdAsync(styles['15-true'].id);label.characters=state==='Loading'?'取消生成':'操作按钮';label.fills=[paint(type==='Primary'?'surface':'accent')];const key=c.addComponentProperty('Label','TEXT',label.characters);label.componentPropertyReferences={characters:key};variants.push(c);buttons[type+'/'+state]={component:c,key};
  }
  const set=figma.combineAsVariants(variants,componentArea);ids.push(set.id);set.name='VoiceStudio / Button';set.description='独立手机应用的主按钮、辅助按钮及生成状态。';set.x=16;set.y=16;set.resize(670,160);
  variants.forEach((c,i)=>{c.x=(i%2)*340;c.y=Math.floor(i/2)*72;});
  const screens=[];
  function frame(parent,name,x,y,w,h,fill,radius){const n=figma.createFrame();ids.push(n.id);parent.appendChild(n);n.name=name;n.resize(w,h);n.x=x;n.y=y;n.fills=fill?[paint(fill)]:[];n.cornerRadius=radius||0;n.clipsContent=false;return n;}
  async function layer(parent,s){
    let n;
    if(s.type==='group'){
      if(s.navIcon){
        const key=s.navIcon+'/'+s.selected;
        if(!navComponents[key]){
          const c=figma.createComponent();componentArea.appendChild(c);ids.push(c.id);c.name='Nav / '+s.navIcon+' / '+(s.selected?'Selected':'Default');c.resize(s.w,s.h);c.x=(Object.keys(navComponents).length%4)*165;c.y=230+Math.floor(Object.keys(navComponents).length/4)*85;c.layoutMode='VERTICAL';c.primaryAxisSizingMode='FIXED';c.counterAxisSizingMode='FIXED';c.primaryAxisAlignItems='CENTER';c.counterAxisAlignItems='CENTER';c.itemSpacing=6;c.setBoundVariable('itemSpacing',vars['space/icon-label']);c.fills=[paint(s.color)];c.cornerRadius=15;c.setBoundVariable('cornerRadius',vars['radius/button']);c.description='底部导航入口，分别提供默认与选中状态。';
          for(const child of s.children)await layer(c,child);
          navComponents[key]=c;
        }
        n=navComponents[key].createInstance();ids.push(n.id,...n.findAll(()=>true).map(x=>x.id));parent.appendChild(n);n.x=s.x;n.y=s.y;
      }else if(s.name==='作品卡片'){
        if(!cardComponent){
          const c=figma.createComponent();componentArea.appendChild(c);ids.push(c.id);c.name='VoiceStudio / WorkCard';c.resize(s.w,s.h);c.x=16;c.y=440;c.fills=[paint('surface')];c.cornerRadius=22;c.setBoundVariable('cornerRadius',vars['radius/card']);c.layoutMode='VERTICAL';c.primaryAxisSizingMode='FIXED';c.counterAxisSizingMode='FIXED';c.paddingLeft=18;c.paddingRight=18;c.paddingTop=18;c.paddingBottom=18;c.itemSpacing=9;for(const edge of ['paddingLeft','paddingRight','paddingTop','paddingBottom'])c.setBoundVariable(edge,vars['space/card']);c.setBoundVariable('itemSpacing',vars['space/compact']);c.description='作品条目，标题、音色时间与文件信息均可覆盖；含播放、导出、分享及管理菜单。';
          for(const child of s.children)await layer(c,child);
          for(const name of ['作品标题','作品信息','音频属性']){const text=c.findAll(n=>n.type==='TEXT'&&n.name===name)[0];const key=c.addComponentProperty(name,'TEXT',text.characters);text.componentPropertyReferences={characters:key};cardProperties[name]=key;}
          cardComponent=c;
        }
        n=cardComponent.createInstance();ids.push(n.id,...n.findAll(()=>true).map(x=>x.id));parent.appendChild(n);n.x=s.x;n.y=s.y;const properties={};
        function collect(nodes){for(const node of nodes){if(cardProperties[node.name])properties[cardProperties[node.name]]=node.text;if(node.children)collect(node.children);}}collect(s.children);n.setProperties(properties);
      }else{
        n=frame(parent,s.name,s.x,s.y,s.w,s.h,s.color,s.radius);if(s.radius===22)n.setBoundVariable('cornerRadius',vars['radius/card']);else if(s.radius===15)n.setBoundVariable('cornerRadius',vars['radius/button']);else if(s.radius===13)n.setBoundVariable('cornerRadius',vars['radius/input']);else if(s.radius===20)n.setBoundVariable('cornerRadius',vars['radius/voice']);
        if(s.layout){n.layoutMode=s.layout;n.primaryAxisSizingMode='FIXED';n.counterAxisSizingMode='FIXED';n.itemSpacing=s.gap||0;if(s.gap===8)n.setBoundVariable('itemSpacing',vars['space/control']);n.counterAxisAlignItems='CENTER';}
        for(const child of s.children)await layer(n,child);
      }
    }else if(s.type==='text'){
      n=figma.createText();ids.push(n.id);parent.appendChild(n);n.fontName=s.bold?bold:regular;n.fontSize=s.size;await n.setTextStyleIdAsync(styles[s.size+'-'+s.bold].id);n.characters=s.text;n.fills=[paint(s.color)];n.textAutoResize='HEIGHT';n.resize(s.w,Math.max(1,s.h));n.textAlignHorizontal=s.align==='center'?'CENTER':'LEFT';n.x=s.x;n.y=s.y;
    }else if(s.type==='rect'){
      n=figma.createRectangle();ids.push(n.id);parent.appendChild(n);n.resize(s.w,s.h);n.x=s.x;n.y=s.y;n.cornerRadius=s.radius;n.fills=[paint(s.color)];if(s.stroke){n.strokes=[paint(s.stroke)];n.strokeWeight=1;}
    }else if(s.type==='icon'){
      const svg='<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32"><path d="'+ICON_PATHS[s.icon]+'" fill="none" stroke="'+DESIGN.colors[s.color]+'" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
      n=figma.createNodeFromSvg(svg);parent.appendChild(n);n.resize(s.w,s.h);n.x=s.x;n.y=s.y;for(const vector of n.findAll(x=>x.type==='VECTOR')){if(Array.isArray(vector.strokes))vector.strokes=vector.strokes.map(p=>p.type==='SOLID'?figma.variables.setBoundVariableForPaint(p,'color',vars[s.color]):p);}ids.push(n.id,...n.findAll(()=>true).map(x=>x.id));
    }else if(s.type==='button'){
      const b=buttons[(s.primary?'Primary':'Secondary')+'/'+(s.state||'Default')];n=b.component.createInstance();ids.push(n.id,...n.findAll(()=>true).map(x=>x.id));parent.appendChild(n);n.resize(s.w,s.h);n.setProperties({[b.key]:s.text});n.x=s.x;n.y=s.y;
    }else throw new Error('Unknown layer '+s.type);
    if(s.opacity!==undefined)n.opacity=s.opacity;n.name=s.name;if(Number.isInteger(s.link))links.push({node:n,target:s.link});return n;
  }
  for(let i=0;i<DESIGN.screens.length;i++){
    const spec=DESIGN.screens[i],screen=frame(page,spec.name,100+(i%2)*490,100+Math.floor(i/2)*1060,spec.w,spec.h,'bg',0);screens.push(screen);
    for(const child of spec.children)await layer(screen,child);
    screen.clipsContent=true;
  }
  // Components stay outside the four mobile screen frames.
  componentArea.x=1120;componentArea.y=100;
  for(const link of links)await link.node.setReactionsAsync([{trigger:{type:'ON_CLICK'},actions:[{type:'NODE',destinationId:screens[link.target].id,navigation:'NAVIGATE',transition:null,preserveScrollPosition:false}]}]);
  page.name='VoiceStudio 1.3.2 · 离线工作室 21屏';figma.currentPage.selection=screens;figma.viewport.scrollAndZoomIntoView(screens);
  return {status:'created',pageId:page.id,font:family,screenIds:screens.map(s=>s.id),nodeIds:ids,variables:Object.values(vars).map(v=>v.id),prototypeLinks:links.length,navComponents:Object.keys(navComponents).length,workCardComponent:cardComponent.id};
}
buildVoiceStudio().then(result=>{console.log(JSON.stringify(result));figma.closePlugin(result.status==='existing'?'VoiceStudio 界面已存在，已打开。':'已创建 21 个 VoiceStudio 可编辑界面。');}).catch(error=>{console.error(error);figma.closePlugin('导入失败：'+error.message);});
