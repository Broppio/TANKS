#!/usr/bin/env python3
"""Packs the Flak 36 "Beauty map" textures into the 2x2 atlas used by flak-88.geo.json.
Usage: python3 tools/flak88_atlas.py <folder with the Beauty map PNGs> [size]"""
import os, sys
from PIL import Image

src = sys.argv[1]
size = int(sys.argv[2]) if len(sys.argv) > 2 else 1024
half = size // 2
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
out = os.path.join(ROOT, 'src/main/resources/assets/warsteel/textures/entity/flak-88.png')
tiles = [('Beauty map flak haut.png', 0, 0), ('Beauty map flak sous haut.png', 1, 0),
         ('Beauty map flak pied.png', 0, 1), ('Beauty map flak detail pied.png', 1, 1)]
atlas = Image.new('RGBA', (size, size), (0, 0, 0, 255))
for name, col, row in tiles:
    im = Image.open(os.path.join(src, name)).convert('RGBA').resize((half, half), Image.LANCZOS)
    atlas.paste(im, (col * half, row * half))
atlas.save(out, optimize=True)
print('wrote', out, os.path.getsize(out), 'bytes')
