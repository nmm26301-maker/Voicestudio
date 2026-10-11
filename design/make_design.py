from pathlib import Path
import json,html
ROOT=Path(__file__).parent
C={'bg':'#F6F7FB','surface':'#FFFFFF','ink':'#202239','muted':'#74788E','accent':'#6154DB','tint':'#EEEBFF','border':'#E6E7EF','success':'#2D8A6E'}
HEIGHT=960

def txt(label,x,y,w,size=14,color='ink',bold=False,name=None):
 return dict(type='text',name=name or label,text=label,x=x,y=y,w=w,h=len(label.split('\n'))*size*1.5,size=size,color=color,bold=bold)
def rect(name,x,y,w,h,color='surface',radius=20,stroke=None):return dict(type='rect',name=name,x=x,y=y,w=w,h=h,color=color,radius=radius,stroke=stroke)
def icon(name,x,y,w=32,color='accent'):return dict(type='icon',name=name,icon=name,x=x,y=y,w=w,h=w,color=color)
def button(label,x,y,w=350,primary=True,link=None,state='Default'):return dict(type='button',name=label,text=label,x=x,y=y,w=w,h=48,primary=primary,link=link,state=state)
def group(name,x,y,w,h,children,color='surface',radius=22,layout=None,padding=0,gap=0):return dict(type='group',name=name,x=x,y=y,w=w,h=h,children=children,color=color,radius=radius,layout=layout,padding=padding,gap=gap)
def base(name,title,subtitle):
 return dict(name=name,w=390,h=HEIGHT,children=[txt('9:41',22,5,70,12,bold=True),txt('●',325,5,14,12),icon('battery',345,6,19,'ink'),txt('VoiceStudio',22,41,240,23,bold=True),txt('让文字，拥有声音',22,73,270,12,'muted'),icon('wave',330,43,39),txt(title,20,121,350,28,bold=True),txt(subtitle,20,166,350,13,'muted'),rect('底部导航',0,863,390,73,radius=0),rect('系统导航背景',0,936,390,24,radius=0),rect('手势条',155,946,80,4,'ink',2)])
def nav(s,index):
 for i,(label,ic) in enumerate(zip(['创作','音色','作品','设置'],['edit','wave','folder','settings'])):
  x=8+i*93.5
  g=group('导航 / '+label,x,871,93.5,57,[icon(ic,35.25,5,23,'accent' if i==index else 'muted'),txt(label,0,34,93.5,11,'accent' if i==index else 'muted',i==index)],'tint' if i==index else 'surface',15)
  g['link']=i;g['navIcon']=ic;g['selected']=i==index;g['children'][1]['align']='center';s['children'].append(g)
def action_row(labels,y,widths=None,links=None):
 widths=widths or [153,153];links=links or [None]*len(labels)
 children=[];x=0
 for label,w,link in zip(labels,widths,links):children.append(button(label,x,0,w,False,link));x+=w+8
 return group('操作行',18,y,314,48,children,radius=0,layout='HORIZONTAL',gap=8)
def work_card(title,date,y,star=False,link=None):
 children=[group('标题行',18,18,314,48,[txt(('★ ' if star else '')+title,0,10,254,16,bold=True,name='作品标题'),button('···',262,0,52,False,4)],radius=0,layout='HORIZONTAL',gap=8),txt('默认音色 · '+date,18,77,314,12,'muted',name='作品信息'),txt('MP3 · 246.4 KB · 00:15',18,101,314,11,'muted',name='音频属性'),action_row(['播放','导出','分享'],133,[99,99,100]),rect('进度轨道',18,207,314,3,'border',2),txt('点击播放，可拖动定位',18,225,314,11,'muted')]
 return group('作品卡片',20,y,350,250,children)

s=base('01 创作','今天，想说些什么？','写下文字，制作属于你的第一段声音。')
s['children'] += [group('配音文本',20,210,350,280,[group('文本标题行',18,18,314,24,[txt('配音文本',0,0,266,15,bold=True),txt('粘贴',274,0,40,13,'accent',True)],radius=0,layout='HORIZONTAL',gap=8),txt('愿你今天遇见的，\n都是温柔的小事。',18,71,314,17),group('字数与保存',18,182,314,17,[txt('草稿自动保存',0,0,220,11,'muted'),txt('20 / 12000',228,0,86,11,'muted')],radius=0,layout='HORIZONTAL',gap=8),action_row(['示例文本','清空 / 撤销'],214)]),group('已选音色',20,504,350,84,[rect('图标背景',18,18,46,46,'tint',12),icon('wave',25,25,32),txt('OmniVoice · 中文普通话',78,20,225,16,bold=True),txt('646 种语言 · 本机离线合成',78,47,246,12,'muted'),txt('›',317,21,15,25,'muted')]),txt('语速',20,608,200,14,bold=True),txt('1.00×',315,608,55,14,'accent',True),rect('滑轨',20,642,350,3,'border',2),rect('速度',20,642,175,3,'accent',2),rect('滑块',187,635,16,16,'accent',8),button('生成声音',20,680,link=5),txt('模型已导入 · 本机离线合成',20,743,350,12,'muted'),txt('长文自动分段，任务在工作台查看。',20,771,350,12,'muted')]
s['children'][-2]['align']='center';next(n for n in s['children'] if n['name']=='已选音色')['link']=1;nav(s,0)

v=base('02 音色','找到你的声音','原模型的语言能力，在手机上离线运行。')
v['children'] += [button('OmniVoice',20,209,170,True),button('手机语音',200,209,170,False,6),group('原模型语言',20,282,350,193,[txt('646 种语言',18,18,314,22,bold=True),txt('完整保留原模型语言目录。\n选择文字的语言，不会自动翻译。',18,59,314,13,'muted'),button('中文普通话 · 选择语言',18,119,314,False,7)]),group('原生自动音色',20,492,350,124,[txt('原生自动音色',18,18,314,16,bold=True),txt('由 OmniVoice 生成自动音色。\n工作台可创建参考音色与设置声音描述。',18,57,314,13,'muted')]),group('离线状态',20,632,350,171,[txt('模型已导入',18,18,314,16,'success',True),txt('音频由本机计算，不发送文本。',18,54,314,13,'muted'),button('管理离线模型',18,101,314,False,3)]),txt('646 种语言尚未逐一实测。',20,819,350,12,'muted')]
nav(v,1)

w=base('03 作品','我的作品','已保存 2 段声音 · 本机存储')
w['children'] += [group('作品搜索',20,210,350,48,[txt('搜索标题、文字或音色',14,13,322,15,'muted')],radius=13),button('全部',20,274,170,True),button('已收藏',200,274,170,False),work_card('温柔的一天','10-08 08:10',336,True),work_card('视频开场旁白','10-08 08:06',600)]
nav(w,2)

settings=base('04 设置','设置','一次导入，离线创作。')
settings['children'] += [group('离线模型',20,211,350,311,[txt('OmniVoice 离线模型',18,18,314,18,bold=True),txt('INT8 原生引擎 · 646 种语言',18,54,314,13,'muted'),txt('模型约 715 MB，预留 1.5 GB 空间。\n首次导入：同时选择两份分包。',18,90,314,13,'muted'),txt('模型已导入 · 校验通过',18,146,314,13,'success',True),button('已导入完整模型',18,182,314,True,state='Disabled'),button('移除模型，释放空间',18,245,314,False)]),group('本机处理',20,539,350,137,[txt('本机处理',18,18,314,16,bold=True),txt('无需 API Key、电脑或服务器。\n应用不申请网络权限。\n运行库遥测在初始化前关闭。',18,54,314,13,'muted')]),group('离线预览说明',20,692,350,141,[txt('VoiceStudio 1.3 · 离线工作室',18,18,314,16,bold=True),txt('支持长文分段，需较充足的内存。\nAndroid 真机性能尚待验证。\n646 是原模型的语言覆盖数。',18,53,314,13,'muted')])]
nav(settings,3)

menu=json.loads(json.dumps(w));menu['name']='05 作品管理';menu['children'] += [rect('对话框遮罩',0,0,390,HEIGHT,'ink',0),group('作品操作菜单',30,290,330,372,[txt('温柔的一天',22,20,286,18,bold=True),txt('重命名',22,79,286,16),txt('取消收藏',22,130,286,16),txt('查看 / 复制文本',22,181,286,16),button('用这段文字再创作',22,228,286,False,0),txt('删除作品',22,300,286,16,'accent')])];menu['children'][-2]['opacity']=.32
busy=json.loads(json.dumps(s));busy['name']='06 生成中'
for n in busy['children']:
 if n['type']=='button' and n['text']=='生成声音':n.update(text='取消生成',name='取消生成',state='Loading',link=0)
 if n['type']=='text' and n['text']=='模型已导入 · 本机离线合成':n.update(text='本机合成 · 18 / 32 步 · 已等待 8 秒')
 if n['type']=='text' and n['text']=='端侧预览，每次合成约 10 秒以内。':n.update(text='完成后自动保存；取消后草稿会保留。')
local=base('07 手机语音','找到你的声音','选择配音方式，也可以收藏自己的音色。')
local['children'] += [button('OmniVoice',20,209,170,False,1),button('手机语音',200,209,170,True),group('语言选择',20,282,350,127,[txt('朗读语言',18,18,314,16,bold=True),txt('普通话',18,55,280,15),icon('chevron',299,54,18,'muted'),txt('语言用于选择语音包，不会翻译文字。',18,91,314,12,'muted')]),group('手机音色 / 已选择',20,426,350,174,[txt('普通话 · 音色 1',18,18,314,16,bold=True),txt('可离线使用 · zh-cn-x-local',18,54,314,12,'muted'),action_row(['已选择','试听'],108)]),group('手机音色 / 可选择',20,616,350,174,[txt('普通话 · 音色 2',18,18,314,16,bold=True),txt('可离线使用 · zh-cn-y-local',18,54,314,12,'muted'),action_row(['选择','试听'],108)])]
nav(local,1)
languages=base('08 语言选择','选择语言','646 种原模型语言，支持名称与代码搜索。')
languages['children'] += [group('搜索语言',20,211,350,48,[txt('搜索中文名、英文名或代码',16,13,318,14,'muted')],radius=13),txt('常用语言',20,282,350,14,bold=True)]
for i,(label,code) in enumerate([('中文普通话','zh'),('粤语','yue'),('英语','en'),('日语','ja'),('韩语','ko'),('法语','fr')]):
 g=group('语言 / '+code,20,319+i*69,350,57,[txt(label,18,17,230,15,bold=i==0),txt(code,272,17,50,13,'accent' if i==0 else 'muted')],'tint' if i==0 else 'surface',13);g['link']=1;languages['children'].append(g)
languages['children'] += [txt('可搜索全部 646 种语言；此处展示常用项。',20,761,350,12,'muted'),button('返回音色',20,798,350,False,1)];nav(languages,1)
s['children'].append(button('打开离线工作台',20,796,350,False,8))
# Existing screen hierarchy and tokens are retained; added views use the same layer schema.
workbench=base('09 离线工作台','你的离线工作室','克隆、转写、翻译与配音，在手机里完成。')
workbench['children'] += [group('克隆与音色',20,210,350,154,[txt('声音克隆与音色设计',18,18,314,18,bold=True),txt('清晰单人参考，保留自己的声音。',18,56,314,13,'muted'),button('创建参考音色',18,91,314,False,9)]),group('转写与翻译',20,380,350,214,[txt('文字与语音互通',18,18,314,18,bold=True),txt('录音或导入音视频，可校对并导出字幕。',18,56,314,12,'muted'),button('导入音视频转写',18,94,314,False,11),button('输入文字并翻译',18,153,314,False,13)]),group('书籍与队列',20,610,350,207,[txt('有声书与批量任务',18,18,314,18,bold=True),txt('TXT / EPUB 按章节保存；批量文字逐条生成。',18,56,314,12,'muted'),button('查看任务与结果',18,91,314,False,10),button('管理模块模型',18,150,314,False,15)])];nav(workbench,0)
clone=base('10 创建参考音色','留住你的声音','参考音频对应文字，帮助模型还原说话方式。')
clone['children'] += [group('参考文件',20,210,350,139,[txt('my-reference.wav',18,18,314,18,bold=True),txt('单人清晰音频 · 参考 3—10 秒',18,57,314,13,'muted'),button('重新选择音频',18,82,314,False)]),group('克隆参数',20,365,350,329,[txt('音色名称',18,18,314,13,'muted'),txt('我的声音',18,46,314,16,bold=True),txt('起点 0 秒    参考 6 秒',18,92,314,14),txt('对应文字',18,140,314,13,'muted'),txt('欢迎使用 VoiceStudio。\n让每一句话，都拥有自己的声音。',18,172,314,15),txt('留空时使用 Whisper 自动转写。',18,247,314,12,'muted'),txt('创建后可选择音色，再生成新文字。',18,282,314,12,'muted')]),button('创建音色并加入队列',20,715,350,True,10),button('返回工作台',20,785,350,False,8)];nav(clone,1)
queue=base('11 任务队列','让创作继续','离开页面后继续；进程结束后可手动续做。')
queue['children'] += [button('暂停整个队列',20,210,350,False),group('运行中的任务',20,278,350,173,[txt('长文合成 · 产品介绍',18,18,314,18,bold=True),txt('本机合成 · 第 2 段 · 18 / 32 步',18,63,314,13,'accent'),rect('进度轨道',18,103,314,6,'border',3),rect('进度值',18,103,174,6,'accent',3),txt('展示任务状态的设计示例',18,130,314,11,'muted')]),group('完成的任务',20,467,350,175,[txt('转写 · interview.mp4',18,18,314,18,bold=True),txt('已完成 · 可校对与导出',18,62,314,13,'success'),button('查看文字与字幕',18,103,314,False,12)]),group('暂停的任务',20,658,350,152,[txt('有声书 · 第一章',18,18,314,17,bold=True),txt('已完成章节保留，继续当前章节。',18,58,314,12,'muted'),button('继续生成',18,91,314,False)])];nav(queue,2)
transcribe=base('12 语音转写','听见，再写下来','Whisper tiny · 语种与合成目录分别管理。')
transcribe['children'] += [group('选择转写输入',20,210,350,238,[txt('音频、视频与麦克风',18,18,314,18,bold=True),txt('音轨离线解码，识别结果可以校对。',18,56,314,13,'muted'),button('导入音频 / 视频',18,93,314,True,10),button('麦克风录音并转写',18,155,314,False,10)]),group('识别语言',20,464,350,152,[txt('自动检测',18,18,314,18,bold=True),txt('也可按模型支持范围指定语言。\n识别有误时，请先校对再配音。',18,63,314,13,'muted')]),button('查看已完成的转写',20,645,350,False,12),button('返回工作台',20,715,350,False,8),txt('646 项合成语言不等于识别语言范围。',20,799,350,12,'muted')];nav(transcribe,0)
subtitle=base('13 字幕校对','校对每一句话','编辑时间与文字，支持添加、拆分和删除。')
subtitle['children'] += [group('字幕条目 1',20,210,350,175,[txt('00:00:00,000 → 00:00:04,200',18,18,314,13,'accent',True),txt('欢迎使用 VoiceStudio。',18,58,314,17,bold=True),button('编辑时间与文字',18,103,314,False)]),group('字幕条目 2',20,401,350,175,[txt('00:00:04,200 → 00:00:08,600',18,18,314,13,'accent',True),txt('让文字，拥有声音。',18,58,314,17,bold=True),button('拆分 / 删除此条',18,103,314,False)]),button('导出 TXT / SRT / JSON',20,605,350,False),button('用校对字幕生成配音',20,674,350,True,14),txt('字幕内容为设计示例；导出使用真实识别结果。',20,760,350,11,'muted')];nav(subtitle,2)
translation=base('14 离线翻译','让语言互相理解','NLLB 200 · 202 个语言 / 文字体系选项')
translation['children'] += [group('源语言与目标语言',20,210,350,149,[txt('原文语言',18,18,314,12,'muted'),txt('英语 · eng_Latn',18,45,314,16,bold=True),txt('目标语言',18,83,314,12,'muted'),txt('简体中文 · zho_Hans',18,110,314,16,bold=True)]),group('原文',20,375,350,164,[txt('Hello, this is VoiceStudio.',18,24,314,16),txt('请输入较短段落，长文请分开翻译。',18,114,314,12,'muted')]),group('翻译结果',20,555,350,116,[txt('你好，这里是 VoiceStudio。',18,24,314,16,bold=True),txt('结果示例 · 可复制或导出 TXT',18,73,314,12,'muted')]),button('离线翻译并加入队列',20,694,350,True,10),button('返回工作台',20,768,350,False,8)];nav(translation,0)
dubbing=base('15 字幕配音','为画面，换一种声音','先校对字幕，再生成对齐时间轴的音轨。')
dubbing['children'] += [group('配音输入',20,210,350,151,[txt('interview.mp4',18,18,314,18,bold=True),txt('使用校对后的字幕与时间轴。\n配音音轨替换原始背景音。',18,63,314,13,'muted')]),group('配音参数',20,377,350,279,[txt('音色 · 首页当前音色',18,18,314,16,bold=True),txt('直接配音使用首页当前语言。',18,58,314,13,'muted'),txt('先翻译成目标语言    □',18,104,314,15),txt('原文 英语  →  目标 简体中文',18,146,314,14),txt('输出 MP4，保留原始画面    □',18,194,314,15),txt('也可单独导出配音 WAV 与字幕。',18,242,314,12,'muted')]),button('生成配音并加入队列',20,679,350,True,10),button('返回字幕校对',20,754,350,False,12)];nav(dubbing,2)
modules=base('16 模块模型','一次导入，离线使用','选择同一模块的全部分包，无需电脑拼接。')
modules['children'] += [group('基础合成模型',20,210,350,120,[txt('OmniVoice · 646 项合成目录',18,18,314,16,bold=True),txt('生成语音、长文、有声书与配音',18,54,314,12,'muted'),txt('SHA-256 校验后启用',18,84,314,12,'success')]),group('克隆模型',20,346,350,120,[txt('声音克隆编码器',18,18,314,16,bold=True),txt('从单人参考音频创建保存的音色',18,54,314,12,'muted'),txt('与基础模型分别导入',18,84,314,12,'muted')]),group('识别模型',20,482,350,120,[txt('Whisper tiny',18,18,314,16,bold=True),txt('音视频转写、录音与参考文字识别',18,54,314,12,'muted'),txt('导出 TXT / SRT / JSON',18,84,314,12,'muted')]),group('翻译模型',20,618,350,120,[txt('NLLB 200 · INT8',18,18,314,16,bold=True),txt('202 个语言 / 文字体系选项',18,54,314,12,'muted'),txt('非商业模型许可，说明见安装指南',18,84,314,12,'muted')]),button('返回工作台',20,772,350,False,8)];nav(modules,3)
# Additional native workflows, reusing the current foundation and components.
workbench['children']=workbench['children'][:11]
# base() contributes eleven layers, including the system bars.
for y,title,description,left,right,targets in [
 (210,'声音创作','参考音色、长文与批量任务',['创建音色'][0],'任务与有声书',[9,10]),
 (358,'文字与配音','先转写、校对，再生成目标语言配音','转写与字幕','离线翻译',[11,13]),
 (506,'音轨与说话人','双声道分离，按字幕片段识别说话人','人声分离','说话人分组',[16,17]),
 (654,'听写与模型','录音结果可复制，模型按模块导入','悬浮听写','模型管理',[18,15])]:
 workbench['children'].append(group(title,20,y,350,132,[txt(title,18,15,314,17,bold=True),txt(description,18,49,314,12,'muted'),button(left,18,75,153,False,targets[0]),button(right,179,75,153,False,targets[1])]))
nav(workbench,0)
modules['children']=modules['children'][:11]
for i,(title,detail) in enumerate([
 ('OmniVoice · 646 项合成目录','语音、长文、有声书与配音'),
 ('声音克隆编码器','单人参考音频 → 保存参考音色'),
 ('Whisper tiny · 99 项识别目录','转写、麦克风与悬浮听写'),
 ('NLLB · 202 项语言 / 文字体系','短段翻译；与合成目录分别管理'),
 ('MDX-Net · 双声道人声分离','导出人声 WAV 与伴奏 WAV'),
 ('CAM++ · 说话人分组','按字幕片段聚类，可手动修正')]):
 modules['children'].append(group(title,20,210+i*84,350,72,[txt(title,18,10,314,16,bold=True),txt(detail,18,40,314,12,'muted')],radius=15))
modules['children'] += [txt('保持分卷原名，一次选择同模块全部分卷。',20,735,350,12,'muted'),button('返回工作台',20,782,350,False,8)]
nav(modules,3)
separation=base('17 人声分离','拆开一段声音','在本机输出人声与伴奏，两份双声道 WAV。')
separation['children'] += [group('输入音轨',20,210,350,143,[txt('song.wav',18,18,314,18,bold=True),txt('音乐或视频音轨 · 保留左右声道',18,57,314,13,'muted'),button('选择音频 / 视频',18,81,314,False)]),group('分离结果',20,371,350,222,[txt('人声与伴奏',18,18,314,18,bold=True),txt('完成后分别试听、保存与分享。',18,57,314,13,'muted'),button('试听 / 导出人声',18,95,314,False),button('试听 / 导出伴奏',18,155,314,False)]),txt('可能残留音乐或人声，输出需要试听。',20,622,350,12,'muted'),button('开始分离并加入队列',20,674,350,True,10),button('返回工作台',20,752,350,False,8)];nav(separation,2)
speakers=base('18 说话人分组','为每个人，选一种声音','按转写片段聚类，分组与音色都可以修改。')
speakers['children'] += [group('分组设置',20,210,350,149,[txt('聚类阈值 · 0.55',18,18,314,17,bold=True),txt('先校对字幕；同段多人时先手动拆分。',18,55,314,12,'muted'),button('运行说话人分组',18,88,314,True,10)]),group('说话人 1',20,378,350,143,[txt('说话人 1 · 片段 1、3、5',18,18,314,16,bold=True),txt('参考音色 · 我的声音',18,58,314,13,'muted'),button('修改分组 / 指定音色',18,82,314,False,12)]),group('说话人 2',20,537,350,143,[txt('说话人 2 · 片段 2、4',18,18,314,16,bold=True),txt('原生自动音色',18,58,314,13,'muted'),button('修改分组 / 指定音色',18,82,314,False,12)]),button('校对后生成多音色配音',20,704,350,True,14),button('返回字幕',20,778,350,False,12)];nav(speakers,2)
floating=base('19 悬浮听写','随手说，随手记','主动录音，离线识别，再复制到需要的地方。')
floating['children'] += [group('录音权限',20,210,350,149,[txt('麦克风与悬浮窗权限',18,18,314,17,bold=True),txt('在工作台启动；麦克风录音需要授权。\n蓝牙耳机可在录音选项中选择。',18,57,314,13,'muted')]),group('悬浮小窗示例',20,377,350,274,[txt('VoiceStudio · 听写',18,18,314,17,bold=True),button('点击开始 / 停止录音',18,62,314,True),txt('今天想记录一个新的想法。',18,135,314,16),txt('识别结果可校对，再自行粘贴。',18,180,314,12,'muted'),button('复制文字',18,211,153,False),button('再录一次',179,211,153,False)]),txt('不自动读取或输入其他应用。',20,681,350,12,'muted'),button('启动悬浮听写',20,720,350,True),button('返回工作台',20,789,350,False,8)];nav(floating,0)
subtitle['children'].append(button('说话人分组与音色分配',20,795,350,False,17))
# 1.3.2: reuse the existing tokens, text and component schema.
for node in menu['children'][-1]['children']:
 if node.get('text')=='删除作品': node.update(text='移到回收站',name='移到回收站',link=19)
for node in settings['children']:
 if node.get('name')=='离线预览说明':
  node['children']=[txt('作品与回收站',18,18,314,16,bold=True),button('打开作品回收站',18,57,314,False,19)]
for node in queue['children']:
 if node.get('name')=='运行中的任务':
  node['children'][0]['w']=270
  node['children'].append(txt('···',290,16,42,14,'accent',True))
queue['children'].append(button('继续所有已暂停任务',20,821,350,False,10))
for node in floating['children']:
 if node.get('type')=='group' and node.get('name')=='悬浮小窗示例':
  node['children'][1]['link']=20
for node in floating['children']:
 if node.get('text')=='不自动读取或输入其他应用。':node.update(text='打开录音小窗后启动麦克风，再自动返回。',name='录音启动说明')
trash=base('20 作品回收站','作品回收站','音频保留在手机里，恢复后回到作品列表。')
trash['children'] += [group('已移除作品',20,210,350,191,[txt('温柔的一天',18,18,314,18,bold=True),txt('普通话 · WAV · 已移到回收站',18,61,314,13,'muted'),button('恢复到作品',18,103,153,True,2),button('彻底删除',179,103,153,False)]),group('删除保护说明',20,422,350,174,[txt('彻底删除前，再确认',18,18,314,17,bold=True),txt('移除内部音频与对应任务。\n已导出的文件保留。\n队列运行时，先暂停并等待结束。',18,60,314,13,'muted')]),txt('作品与状态均为设计示例。',20,628,350,12,'muted'),button('返回设置',20,687,350,False,3)];nav(trash,3)
recording=base('21 录音启动','打开麦克风','启动成功后，自动回到悬浮按钮。')
recording['children'] += [group('录音启动小窗',30,301,330,235,[txt('VoiceStudio · 悬浮听写',20,20,290,18,bold=True),txt('正在打开麦克风…',20,80,290,15,'muted'),button('取消',20,150,290,False,18)]),txt('需要麦克风权限；可随时取消。',20,570,350,13,'muted'),txt('录音开始后，悬浮按钮变为“结束”。',20,609,350,13,'muted'),button('返回悬浮听写',20,693,350,False,18)];nav(recording,0)
screens=[s,v,w,settings,menu,busy,local,languages,workbench,clone,queue,transcribe,subtitle,translation,dubbing,modules,separation,speakers,floating,trash,recording]

ROOT.joinpath('scenes.json').write_text(json.dumps({'colors':C,'screens':screens},ensure_ascii=False,indent=2))
PATHS={'battery':'M3 8H26V24H3Z M29 13V19 M7 12H21V20H7Z','chevron':'M7 12L16 21L25 12','wave':'M6 13V19 M11 8.5V23.5 M16 4.5V27.5 M21 8.5V23.5 M26 13V19','folder':'M4 10V7H13L16 10H28V26H4Z M4 13H28','edit':'M9 5H23Q26 5 26 9V23Q26 27 22 27H9Q5 27 5 23V9Q5 5 9 5 M11 20L23 8 M11 20L10 24L14 23','settings':'M16 3V6 M16 26V29 M3 16H6 M26 16H29 M7 7L9 9 M23 23L25 25 M7 25L9 23 M23 9L25 7 M25 16A9 9 0 1 0 7 16A9 9 0 1 0 25 16 M19 16A3 3 0 1 0 13 16A3 3 0 1 0 19 16'}
def svg_layer(n):
 x,y,w,h=n['x'],n['y'],n['w'],n['h'];name=html.escape(n['name'],quote=True)
 if n['type']=='group':return f'<g id="{name}" transform="translate({x},{y})"><rect width="{w}" height="{h}" rx="{n["radius"]}" fill="{C[n["color"]]}"/>'+''.join(svg_layer(c) for c in n['children'])+'</g>'
 if n['type']=='text':
  tx=x+w/2 if n.get('align')=='center' else x
  lines=''.join(f'<tspan x="{tx}" dy="{0 if i==0 else n["size"]*1.5}">{html.escape(t)}</tspan>' for i,t in enumerate(n['text'].split('\n')))
  return f'<text id="{name}" x="{tx}" y="{y+n["size"]}" font-family="Noto Sans CJK SC, Noto Sans SC, sans-serif" font-size="{n["size"]}" font-weight="{700 if n["bold"] else 400}" fill="{C[n["color"]]}" text-anchor="{"middle" if n.get("align")=="center" else "start"}">{lines}</text>'
 if n['type']=='rect':return f'<rect id="{name}" x="{x}" y="{y}" width="{w}" height="{h}" rx="{n["radius"]}" fill="{C[n["color"]]}" opacity="{n.get("opacity",1)}"'+(f' stroke="{C[n["stroke"]]}"' if n.get('stroke') else '')+'/>'
 if n['type']=='icon':return f'<g id="{name}" transform="translate({x},{y}) scale({w/32})"><path d="{PATHS[n["icon"]]}" fill="none" stroke="{C[n["color"]]}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></g>'
 if n['type']=='button':return f'<g id="{name}"><rect x="{x}" y="{y}" width="{w}" height="{h}" rx="15" fill="{C["accent" if n["primary"] else "tint"]}"/><text x="{x+w/2}" y="{y+30}" text-anchor="middle" font-family="Noto Sans CJK SC, Noto Sans SC, sans-serif" font-size="15" font-weight="700" fill="{C["surface" if n["primary"] else "accent"]}">{html.escape(n["text"])}</text></g>'
 raise ValueError(n)
for i,scene in enumerate(screens):
 svg=f'<svg xmlns="http://www.w3.org/2000/svg" width="390" height="{HEIGHT}" viewBox="0 0 390 {HEIGHT}"><rect width="390" height="{HEIGHT}" fill="{C["bg"]}"/>'+''.join(svg_layer(n) for n in scene['children'])+'</svg>'
 ROOT.joinpath(f'{i+1:02d}-{["create","voices","works","settings","work-menu","generating","local-voices","languages","workbench","clone","queue","transcribe","subtitles","translation","dubbing","modules","separation","speakers","floating","trash","recording"][i]}.svg').write_text(svg)
ROOT.joinpath('icon-paths.json').write_text(json.dumps(PATHS))
print('Prepared 21 editable screen/state descriptions.')
