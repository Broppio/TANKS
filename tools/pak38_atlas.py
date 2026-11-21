#!/usr/bin/env python3
"""Packs the Pak 38's two 2048x1024 textures into the 1024x1024 atlas used by pak-38.geo.json
(tex_8419 on top, tex_9059 below; see tools/pak38_export.py).
Usage: python3 tools/pak38_atlas.py <folder with Tex_8419_0.png and Tex_9059_0.png>"""
import os, sys
from PIL import Image

src = sys.argv[1]
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
out = os.path.join(ROOT, 'src/main/resources/assets/warsteel/textures/entity/pak-38.png')
atlas = Image.new('RGB', (1024, 1024))
for i, name in enumerate(['Tex_8419_0.png', 'Tex_9059_0.png']):
    atlas.paste(Image.open(os.path.join(src, name)).convert('RGB').resize((1024, 512), Image.LANCZOS), (0, i * 512))
atlas.convert('RGBA').save(out, optimize=True)
print('wrote', out, os.path.getsize(out), 'bytes')
