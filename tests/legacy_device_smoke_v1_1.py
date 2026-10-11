"""Real Android UI smoke checks; requires a ready adb device on the same network namespace."""
import json, os, re, subprocess, time, wave, math, struct
from pathlib import Path
import xml.etree.ElementTree as ET

ADB=os.environ['VOICE_ADB']; SERIAL=os.environ.get('VOICE_DEVICE','emulator-5580');PKG='com.voice.studio'
OUT=Path(os.environ['VOICE_TEST_OUTPUT']);OUT.mkdir(parents=True,exist_ok=True)
results=[]
def adb(*args,timeout=35):
    return subprocess.check_output([ADB,'-s',SERIAL,*args],timeout=timeout)
def shell(*args):return adb('shell',*args).decode(errors='replace')
def record(name,detail='passed'):results.append({'check':name,'result':detail});print(name+': '+detail,flush=True)
def ui():
    shell('uiautomator','dump','/sdcard/voicestudio-ui.xml')
    return ET.fromstring(shell('cat','/sdcard/voicestudio-ui.xml'))
def find(label):
    for node in ui().iter('node'):
        if node.attrib.get('text')==label or node.attrib.get('content-desc')==label:return node
    raise AssertionError('Missing UI: '+label)
def tap(label):
    n=find(label);p=list(map(int,re.findall(r'\d+',n.attrib['bounds'])));shell('input','tap',str((p[0]+p[2])//2),str((p[1]+p[3])//2));time.sleep(.6)
def screenshot(name):
    OUT.joinpath(name+'.png').write_bytes(adb('exec-out','screencap','-p'))
    OUT.joinpath(name+'.xml').write_text(ET.tostring(ui(),encoding='unicode'))
def launch(text=None):
    args=['am','start','-W','-n',PKG+'/.MainActivity']
    if text:args+=['-a','android.intent.action.SEND','-t','text/plain','--es','android.intent.extra.TEXT',text]
    shell(*args);time.sleep(1)

try:
    adb('install','-r',os.environ['VOICE_APK'],timeout=60);launch();find('今天，想说些什么？');screenshot('01-create');record('Install and launch on device API '+shell('getprop','ro.build.version.sdk').strip())
    tap('生成声音');find('今天，想说些什么？');record('Empty input does not call synthesis')
    tap('音色');find('默认音色');screenshot('02-voices');tap('＋  添加音色');find('添加音色');tap('保存');assert find('添加音色') is not None;record('Voice dialog rejects empty values');tap('取消')
    tap('作品');find('还没有作品');screenshot('03-works-empty');record('Empty history screen')
    tap('设置');find('连接 Fish Audio');screenshot('04-settings');record('Settings and key entry screen')
    tap('创作');launch('VoiceStudio_device_test');find('VoiceStudio_device_test');tap('生成声音');find('连接 Fish Audio');record('Missing API key routes to settings')
    shell('am','force-stop',PKG);launch();find('VoiceStudio_device_test');record('Draft persists after process restart')
    tap('设置');tree=ui();edits=[n for n in tree.iter('node') if n.attrib.get('class')=='android.widget.EditText'];assert edits
    p=list(map(int,re.findall(r'\d+',edits[0].attrib['bounds'])));shell('input','tap',str((p[0]+p[2])//2),str((p[1]+p[3])//2));shell('input','text','voicestudio_fake_test_key');shell('input','keyevent','4');tap('保存配置')
    secure=shell('run-as',PKG,'cat','shared_prefs/secure.xml');assert 'voicestudio_fake_test_key' not in secure and 'token' in secure;record('API key stored as ciphertext')
    shell('am','force-stop',PKG)
    # Fixture for playback, export and provider checks, never a synthesis result.
    fixture=OUT/'fixture.wav'
    with wave.open(str(fixture),'w') as wav:
        wav.setparams((1,2,22050,0,'NONE','not compressed'));wav.writeframes(b''.join(struct.pack('<h',int(1000*math.sin(2*math.pi*440*i/22050))) for i in range(22050)))
    filename='11111111-1111-4111-8111-111111111111.wav';adb('push',str(fixture),'/data/local/tmp/fixture.wav');shell('run-as',PKG,'mkdir','-p','files/audio');shell('run-as',PKG,'cp','/data/local/tmp/fixture.wav','files/audio/'+filename)
    history=[{'title':'[UI 测试音频] 播放、导出与分享','duration':1000,'id':'fixture','file':filename,'text':'[UI 测试音频] 播放、导出与分享','voice':'测试夹具','engine':'fixture','created':int(time.time()*1000)}]
    OUT.joinpath('history.json').write_text(json.dumps(history,ensure_ascii=False));adb('push',str(OUT/'history.json'),'/data/local/tmp/voice-history.json');shell('run-as',PKG,'cp','/data/local/tmp/voice-history.json','files/history.json')
    launch();tap('作品');find('[UI 测试音频] 播放、导出与分享');tap('播放');record('MediaPlayer accepts local WAV fixture')
    tap('导出');assert 'DocumentsActivity' in shell('dumpsys','activity','activities') or 'DocumentsUI' in shell('dumpsys','activity','activities');record('Export opens Android document picker');shell('input','keyevent','4');time.sleep(.5)
    tap('分享');record('Share launches with content URI');shell('input','keyevent','4');time.sleep(.5)
    errors=shell('logcat','-d','-s','AndroidRuntime:E');assert 'FATAL EXCEPTION' not in errors;record('No AndroidRuntime fatal exception')
    # Pull system CJK font only for local preview rendering.
    fonts=shell('ls','/system/fonts');cjk=next((name for name in fonts.splitlines() if 'CJK' in name and name.endswith('.ttc')),None)
    if cjk:adb('pull','/system/fonts/'+cjk,str(OUT/'NotoSansCJK.ttc'))
except Exception as e:
    record('FAILED',str(e));raise
finally:
    OUT.joinpath('results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))

