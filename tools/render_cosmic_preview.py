"""Static layout illustration using app palette. Not Android/device screenshots."""
from PIL import Image,ImageDraw,ImageFont
from pathlib import Path
import math,sys
S=2;W,H=390,844
BG='#080F20';SURFACE='#121D34';INK='#EEF4FF';MUTED='#A7B6D1';CYAN='#7CEBFA';PURPLE='#B2A2FF';LINE='#354663';SECONDARY='#1B2A46'
font_path=sys.argv[1];output=sys.argv[2]
def font(n):return ImageFont.truetype(font_path,n*S)
def txt(d,x,y,s,n=14,c=INK):d.text((x*S,y*S),s,font=font(n),fill=c)
def rect(d,x,y,w,h,c=SURFACE,r=22,line=LINE):d.rounded_rectangle((x*S,y*S,(x+w)*S,(y+h)*S),radius=r*S,fill=c,outline=line,width=S)
def panel(d,y,h):
    rect(d,20,y,350,h);d.line((322*S,(y+8)*S,342*S,(y+8)*S),fill=PURPLE,width=2*S);d.line((42*S,(y+h-8)*S,62*S,(y+h-8)*S),fill=CYAN,width=2*S)
def button(im,d,x,y,w,s,primary=False):
    if primary:
        grad=Image.new('RGB',(int(w*S),48*S));g=ImageDraw.Draw(grad)
        a=(113,83,217);b=(54,94,175)
        for xx in range(int(w*S)):
            t=xx/max(1,w*S-1);color=tuple(round(aa*(1-t)+bb*t) for aa,bb in zip(a,b));g.line((xx,0,xx,48*S),fill=color)
        mask=Image.new('L',grad.size);ImageDraw.Draw(mask).rounded_rectangle((0,0,grad.width-1,grad.height-1),16*S,fill=255);im.paste(grad,(int(x*S),int(y*S)),mask)
    else:rect(d,x,y,w,48,SECONDARY,16)
    f=font(14);width=d.textlength(s,font=f);d.text(((x+w/2)*S-width/2,(y+13)*S),s,font=f,fill=INK if primary else PURPLE)
def orb(d,x=336,y=79):
    for r in range(36,5,-3):d.ellipse(((x-r)*S,(y-r)*S,(x+r)*S,(y+r)*S),fill=tuple([12+int((36-r)*.35),22+int((36-r)*.4),45+int((36-r)*.7)]))
    d.ellipse(((x-27)*S,(y-27)*S,(x+27)*S,(y+27)*S),outline=LINE,width=S)
    d.arc(((x-27)*S,(y-27)*S,(x+27)*S,(y+27)*S),-80,2,fill=CYAN,width=2*S)
    for i in range(7):
        hh=9+13*(.5+.5*math.sin(i*.9));xx=x+(i-3)*5.5;d.rounded_rectangle((int((xx-1)*S),int((y-hh/2)*S),int((xx+1)*S),int((y+hh/2)*S)),radius=2,fill=CYAN if i%2==0 else PURPLE)
def nav(d,page):
    rect(d,0,752,390,92,BG,0,BG)
    for i,label in enumerate(['创作','音色','作品','设置']):
        x=8+i*94;selected=label==page
        if selected:rect(d,x,763,90,61,'#1B3049',17,CYAN)
        color=CYAN if selected else MUTED;cx=x+45;cy=784
        if i==0:
            d.rounded_rectangle(((cx-9)*S,(cy-8)*S,(cx+9)*S,(cy+8)*S),radius=3*S,outline=color,width=S);d.line(((cx-4)*S,(cy+4)*S,(cx+6)*S,(cy-5)*S),fill=color,width=2*S)
        elif i==1:
            for k in range(5):
                height=[6,13,20,13,6][k];xx=cx+(k-2)*5;d.line((xx*S,(cy-height/2)*S,xx*S,(cy+height/2)*S),fill=color,width=2*S)
        elif i==2:d.rectangle(((cx-10)*S,(cy-6)*S,(cx+10)*S,(cy+8)*S),outline=color,width=2*S);d.line(((cx-10)*S,(cy-9)*S,(cx-2)*S,(cy-9)*S),fill=color,width=2*S)
        else:d.ellipse(((cx-8)*S,(cy-8)*S,(cx+8)*S,(cy+8)*S),outline=color,width=2*S);d.ellipse(((cx-3)*S,(cy-3)*S,(cx+3)*S,(cy+3)*S),outline=color,width=S)
        txt(d,x+31,801,label,11,color)
    d.rounded_rectangle((140*S,831*S,250*S,835*S),radius=2*S,fill=INK)
def base(page,badge):
    im=Image.new('RGB',(W*S,H*S),BG);d=ImageDraw.Draw(im)
    txt(d,23,11,'9:41',11);txt(d,323,11,'83%' ,10,MUTED)
    txt(d,22,43,'VoiceStudio',23);txt(d,22,80,'声音，值得被收藏' if page=='作品' else '让文字，拥有声音',12,MUTED);txt(d,22,105,badge,10,CYAN);orb(d)
    return im,d
def creation():
    im,d=base('创作','创作舱');button(im,d,20,139,350,'离线工作台 · 转写 / 翻译 / 更多')
    txt(d,20,207,'让文字，拥有声音',27);txt(d,20,251,'写下想说的话，选择喜欢的音色。',12,MUTED)
    panel(d,284,277);txt(d,38,306,'创作文本',16);txt(d,310,309,'粘贴',12,PURPLE)
    for yy,ss in [(346,'例如：愿你今天遇见的，都是温柔'),(375,'的小事。'),(419,'也可以粘贴文章、旁白或想练习'),(448,'的句子。')]:txt(d,38,yy,ss,15,MUTED)
    txt(d,38,492,'0 / 12000',11,MUTED);button(im,d,38,516,135,'示例');button(im,d,190,516,162,'清空')
    panel(d,585,106);txt(d,38,605,'音色与语言',16);txt(d,38,639,'OmniVoice · 中文普通话',13,MUTED)
    rect(d,0,674,390,78,BG,0,LINE);button(im,d,20,685,350,'生成声音',True);txt(d,94,735,'已准备好 · 生成时留在当前页面',10,MUTED)
    nav(d,'创作');return im
def voice():
    im,d=base('音色','音色实验室');txt(d,20,144,'选择你的声音',27);txt(d,20,188,'云端与本机音色，按需选择。',12,MUTED)
    panel(d,222,112);txt(d,38,242,'合成引擎',16);button(im,d,38,273,99,'Fish Audio');button(im,d,146,273,105,'OmniVoice');button(im,d,260,273,91,'手机音色')
    panel(d,354,153);txt(d,38,376,'646 种语言',18);txt(d,38,412,'选择输入文字所属的语言。',13,MUTED);button(im,d,38,445,314,'中文普通话 · 选择语言')
    panel(d,529,106);txt(d,38,551,'原生自动音色',18);txt(d,38,589,'参考音色 · 描述 · 采样步数 · 种子',12,MUTED)
    panel(d,656,90);txt(d,38,675,'离线模型管理',17);txt(d,38,710,'安装状态以手机中的实际文件为准',12,MUTED);nav(d,'音色');return im
def works():
    im,d=base('作品','声音星库');panel(d,143,169);txt(d,38,164,'声音星库',28);txt(d,38,210,'保存在本机的声音，随时播放与分享。',12,MUTED)
    for x,value,label in [(38,'3','声音作品'),(145,'00:32','总时长'),(255,'1','已收藏')]:txt(d,x,247,value,23);txt(d,x,283,label,10,MUTED)
    rect(d,20,332,350,48,'#0C162B',16);txt(d,36,346,'搜索标题、文字或音色…',13,MUTED)
    button(im,d,20,399,100,'全部');button(im,d,130,399,112,'已收藏');button(im,d,252,399,118,'最新优先')
    panel(d,468,280);txt(d,38,487,'OMNIVOICE / WAV',10,CYAN);txt(d,293,481,'☆  ···',16,PURPLE);txt(d,38,521,'城市夜航 · 旁白',18);txt(d,38,556,'自动音色 · 10-11 06:49',11,MUTED)
    for i in range(65):
        hh=8+28*abs(math.sin(i*.36)*math.sin(i*.13+1));x=38+i*4.8;d.line((x*S,(605-hh/2)*S,x*S,(605+hh/2)*S),fill=CYAN if i<25 else '#596995',width=2*S)
    txt(d,38,638,'WAVE / 采样波形概览',9,MUTED);txt(d,315,635,'00:12',11,CYAN)
    button(im,d,38,663,125,'播放',True);button(im,d,173,663,80,'导出');button(im,d,263,663,89,'分享');txt(d,38,722,'示例作品 · 实际列表显示你的本机音频',10,MUTED)
    nav(d,'作品');return im
def settings():
    im,d=base('设置','控制中心');txt(d,20,144,'设置',28);txt(d,20,188,'云端与本机合成，按需选择。',12,MUTED)
    panel(d,221,181);txt(d,38,244,'界面与动效',18);txt(d,38,282,'深空配色、轨道光环与柔和转场。',12,MUTED);txt(d,38,307,'合成与省电模式下暂停装饰动效。',12,MUTED);button(im,d,38,339,314,'动效已开启 · 点击关闭')
    panel(d,423,294);txt(d,38,446,'Fish Audio 云端合成',18);txt(d,38,484,'配置 API Key 后即可使用云端音色。',12,MUTED)
    rect(d,38,522,314,48,'#0C162B',14);txt(d,52,536,'填写 API Key',14,MUTED)
    rect(d,38,584,314,48,SECONDARY,14);txt(d,52,598,'选择合成模型',14,MUTED);button(im,d,38,650,314,'保存 Fish 配置',True)
    nav(d,'设置');return im
pages=[creation(),voice(),works(),settings()]
board=Image.new('RGB',(820*S,1870*S),'#050A16');d=ImageDraw.Draw(board)
txt(d,20,14,'VoiceStudio 1.5.0 · 深空声学界面',25);txt(d,20,57,'原生界面配色与布局设计预览 · 示例状态与数据 · 非真机截图',12,MUTED)
for i,page in enumerate(pages):board.paste(page,((15+(i%2)*400)*S,(98+(i//2)*866)*S))
txt(d,20,1842,'原生 Canvas 光环 / AndroidX 缓动 / 固定生成栏 / 可关闭动效',11,MUTED)
Path(output).parent.mkdir(parents=True,exist_ok=True);board.save(output);print(output)
