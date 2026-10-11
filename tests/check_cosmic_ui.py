"""Text contrast, pinned OSS integrity and source integration guards; not device QA."""
from pathlib import Path
import re,hashlib,json,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1];src=root/'app/src/main/java/com/voice/studio'
shared=(src/'CosmicUi.java').read_text();colors={k:int(v,16)&0xffffff for k,v in re.findall(r'(\w+)\s*=\s*0x([0-9a-fA-F]{8})',shared)}
def luminance(c):
    channels=[((c>>n)&255)/255 for n in (16,8,0)]
    channels=[v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4 for v in channels]
    return sum(a*b for a,b in zip(channels,[.2126,.7152,.0722]))
def contrast(a,b):
    aa,bb=sorted([luminance(a),luminance(b)]);return (bb+.05)/(aa+.05)
checks=0;minimum=100
for fg in ['INK','MUTED','CYAN','PURPLE','SUCCESS']:
    for bg in ['BG','SURFACE','SECONDARY']:
        ratio=contrast(colors[fg],colors[bg]);assert ratio>=4.5,(fg,bg,ratio);minimum=min(minimum,ratio);checks+=1
for i in range(101):
    t=i/100;a=0x7153D9;b=0x365EAF;c=sum(round(((a>>n)&255)*(1-t)+((b>>n)&255)*t)<<n for n in (16,8,0))
    assert contrast(0xffffff,c)>=4.5;checks+=1
vendor=root/'third_party/androidx-interpolator'
for item in json.loads((vendor/'source.json').read_text()):
    data=(vendor/item['file']).read_bytes();assert hashlib.sha256(data).hexdigest()==item['sha256'];checks+=1
    assert (src/item['file']).read_text()==data.decode().replace('package androidx.interpolator.view.animation;','package com.voice.studio;');checks+=1
    assert b'Licensed under the Apache License' in data;checks+=1
for name in ['MainActivity.java','WorkspaceActivity.java','ModelDownloadsActivity.java','ModelInstallActivity.java']:
    s=(src/name).read_text();assert 'new AlertDialog.Builder(this)' not in s;checks+=1
for name in ['MainActivity.java','WorkspaceActivity.java','ModelViews.java','RecordingActivity.java']:
    assert 'CosmicUi.install(' in (src/name).read_text();checks+=1
styles=(root/'app/src/main/res/values/styles.xml').read_text();ET.fromstring(styles);assert 'Theme.Material.Light' not in styles;checks+=1
main=(src/'MainActivity.java').read_text();assert 'add(content, generate)' not in main and 'add(root,dock)' in main;checks+=1
assert 'putBoolean("ui-motion"' in main;checks+=1
assert 'return false;' in shared;checks+=1
assert 'onActivityPaused' in (src/'StudioApplication.java').read_text();checks+=1
orb=(src/'SignalOrbView.java').read_text();assert 'removeCallbacks(tick);if(moving)postDelayed' in orb;checks+=1
dream=(src/'DreamProgressView.java').read_text();assert 'foreground && CosmicUi.isForeground(this) && CosmicUi.motionAllowed' in dream and 'postInvalidateDelayed(50)' in dream;checks+=1
print(f'Cosmic UI: {checks} source/contrast checks passed; minimum shared text contrast {minimum:.2f}:1. No native visual or lifecycle simulation.')
