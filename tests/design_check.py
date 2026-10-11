"""Check exported screen geometry and measure text with the Android CJK font."""
from pathlib import Path
import json
from PIL import ImageFont
root=Path(__file__).resolve().parents[1]
data=json.loads((root/'design/scenes.json').read_text())
import os
font_path=Path(os.environ.get('VOICE_STUDIO_FONT',root.parents[1]/'tooling/NotoSansCJKsc-Regular.otf'))
errors=[];layers=0;texts=0
def check(n,pw,ph,path):
 global layers,texts
 layers+=1
 x,y,w,h=[n[k] for k in ['x','y','w','h']]
 if x<0 or y<0 or x+w>pw+.1 or y+h>ph+.1:errors.append(path+': outside parent')
 if n['type']=='text':
  texts+=1;font=ImageFont.truetype(str(font_path),n['size'],index=0)
  lines=n['text'].split('\n')
  for line in lines:
   if font.getlength(line)>w+2:errors.append(path+': text wider than layer: '+line)
  actual=len(lines)*n['size']*1.5
  if y+actual>ph+.1:errors.append(path+': text height exceeds parent')
 if n['type']=='group':
  for child in n['children']:check(child,w,h,path+'/'+child['name'])
for s in data['screens']:
 for n in s['children']:check(n,s['w'],s['h'],s['name']+'/'+n['name'])
assert len(data['screens'])==21
assert not errors,'\n'.join(errors)
print(json.dumps({'screens':21,'layers':layers,'editableTextLayers':texts,'geometry':'passed','textBounds':'passed'},ensure_ascii=False))
