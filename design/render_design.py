"""Render editable SVG previews; requires CairoSVG, Pillow and a CJK system font."""
from pathlib import Path
import json
import cairosvg
from PIL import Image, ImageDraw

root = Path(__file__).parent
scenes = json.loads((root/'scenes.json').read_text())['screens']
paths = sorted(root.glob('[0-9][0-9]-*.svg'))
assert len(paths) == len(scenes)
for path in paths:
    cairosvg.svg2png(url=str(path),write_to=str(path.with_suffix('.png')),output_width=390,output_height=960)
# Preview panels are references, never imported as full-screen image layers.
selected = [paths[n].with_suffix('.png') for n in [4,10,19,20]]
preview = Image.new('RGB',(1640,1024),'#EEEBFF')
for n,path in enumerate(selected):
    panel = Image.open(path).convert('RGB')
    preview.paste(panel,(20+n*405,32))
preview.save(root/'new-features-preview.png')
print('Rendered 21 standalone screen previews and new feature overview.')
