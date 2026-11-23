#!/usr/bin/env python3
"""Packs the T-22's diffuse textures into the 2048 atlas used by t-22.geo.json (cells as in
tools/t22_export.py: 4x4 cells of 512 px, the hull taking 2x2). Alpha is dropped (the source
textures use it for other data).
Usage: python3 tools/t22_atlas.py <T-22 Texture folder>"""
import os, sys
from PIL import Image

src = sys.argv[1]
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
out = os.path.join(ROOT, 'src/main/resources/assets/warsteel/textures/entity/t-22.png')
CELLS = {
    'Tex_T-22_Hull_diffuse.png': (0, 0, 2), 'Tex_Z39_Props_diffuse.png': (2, 0, 1), 'Tex_T-22_MainTurret_diffuse.png': (3, 0, 1),
    'Tex_Z23_TorpedoTubes_diffuse.png': (2, 1, 1), 'Tex_Vessel_diffuse.png': (3, 1, 1), 'Tex_Flakvierling_diffuse.png': (0, 2, 1),
    'Tex_AA_Flak_diffuse.png': (1, 2, 1), 'Tex_Flakzwilling_3_diffuse.png': (2, 2, 1), 'Tex_T-22_Radar_1_diffuse.png': (3, 2, 1),
    'Tex_T-22_Rudder_diffuse.png': (0, 3, 1), 'Tex_Scharnhorst_Propellor_diffuse.png': (1, 3, 1),
}
atlas = Image.new('RGB', (2048, 2048), (70, 72, 74))
for name, (c, r, span) in CELLS.items():
    im = Image.open(os.path.join(src, name)).convert('RGB').resize((512 * span, 512 * span), Image.LANCZOS)
    atlas.paste(im, (c * 512, r * 512))
atlas.paste((38, 38, 38), (2 * 512, 3 * 512, 3 * 512, 4 * 512))   # cables and rigging
atlas.convert('RGBA').save(out, optimize=True)
print('wrote', out, os.path.getsize(out), 'bytes')
