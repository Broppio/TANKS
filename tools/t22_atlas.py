#!/usr/bin/env python3
"""Packs the T-22's diffuse textures into the 2048 atlas used by t-22.geo.json (cells as in
tools/t22_export.py: 4x4 cells of 512 px, the hull taking 2x2). Alpha only marks the UV islands: their colours are
bled out over the padding, then alpha is dropped.
Usage: python3 tools/t22_atlas.py <T-22 Texture folder>"""
import os, sys
import numpy as np
from PIL import Image


def bleed(im):
    """Grow each UV island's colours out over the transparent padding around it, so scaling the
    texture down (and Minecraft's mipmaps) don't pull black into the island edges as dark seams."""
    a = np.asarray(im.convert('RGBA')).astype(np.float32)
    rgb, ok = a[..., :3], a[..., 3] > 127
    rgb[~ok] = 0.0
    for _ in range(48):
        if ok.all():
            break
        acc = np.zeros_like(rgb)
        n = np.zeros(ok.shape, np.float32)
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            m = np.roll(ok, (dy, dx), (0, 1))
            acc += np.roll(rgb, (dy, dx), (0, 1)) * m[..., None]
            n += m
        grow = ~ok & (n > 0)
        rgb[grow] = acc[grow] / n[grow, None]
        ok = ok | grow
    if not ok.all():
        rgb[~ok] = rgb[ok].mean(0)
    return Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), 'RGB')


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
    im = bleed(Image.open(os.path.join(src, name))).resize((512 * span, 512 * span), Image.LANCZOS)
    atlas.paste(im, (c * 512, r * 512))
atlas.paste((38, 38, 38), (2 * 512, 3 * 512, 3 * 512, 4 * 512))   # cables and rigging
atlas.convert('RGBA').save(out, optimize=True)
print('wrote', out, os.path.getsize(out), 'bytes')
