#!/usr/bin/env python3
"""Builds the Flak 36 texture atlas used by flak-88.geo.json from the model's baked maps.

Usage: python3 tools/flak88_atlas.py <folder with the AO map / Beauty map PNGs> [size]

The source "Beauty map" images are lit renders (baked shading and dark backgrounds), which look
dark and blotchy once Minecraft lights them again. Instead each tile is painted as an albedo:
  paint colour x ambient occlusion x fine detail (beauty map / its blurred self)
and the UV islands are grown outwards so mip-mapping and decimated UVs never sample black.
Layout (2x2):  haut | sous haut  /  pied | pied detail
"""
import os, sys
import numpy as np
from PIL import Image, ImageFilter

src = sys.argv[1]
size = int(sys.argv[2]) if len(sys.argv) > 2 else 1024
half = size // 2
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
out = os.path.join(ROOT, 'src/main/resources/assets/warsteel/textures/entity/flak-88.png')

PAINT = np.array([104, 110, 110], float) / 255.0     # weathered dunkelgrau
TILES = [(('Ao map flak haut.png', 'Beauty map flak haut.png'), 0, 0),
         (('AO map flak sous haut.png', 'Beauty map flak sous haut.png'), 1, 0),
         (('AO map flak pied.png', 'Beauty map flak pied.png'), 0, 1),
         (('AO map flak detail pied.png', 'Beauty map flak detail pied.png'), 1, 1)]


def grey(path):
    return np.asarray(Image.open(path).convert('L').resize((half, half), Image.LANCZOS), float) / 255.0


def dilate(rgb, mask, steps):
    """Grow coloured pixels into the empty background, a pixel ring per step."""
    rgb, mask = rgb.copy(), mask.copy()
    for _ in range(steps):
        acc = np.zeros_like(rgb)
        cnt = np.zeros(mask.shape)
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            m = np.roll(mask, (dy, dx), (0, 1))
            acc += np.roll(rgb, (dy, dx), (0, 1)) * m[..., None]
            cnt += m
        grow = (~mask) & (cnt > 0)
        if not grow.any():
            break
        rgb[grow] = acc[grow] / cnt[grow][:, None]
        mask = mask | grow
    rgb[~mask] = PAINT * 0.6
    return rgb


atlas = np.zeros((size, size, 3))
for (ao_name, beauty_name), col, row in TILES:
    ao = grey(os.path.join(src, ao_name))
    beauty = grey(os.path.join(src, beauty_name))
    mask = ao > 0.03
    blur = np.asarray(Image.fromarray((beauty * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(6)), float) / 255.0
    detail = np.clip(beauty / np.maximum(blur, 0.04), 0.82, 1.18)
    shade = (0.52 + 0.48 * np.clip(ao, 0, 1)) * detail
    rgb = np.clip(PAINT[None, None, :] * shade[..., None], 0, 1)
    rgb = dilate(rgb, mask, 24)
    atlas[row * half:(row + 1) * half, col * half:(col + 1) * half] = rgb

Image.fromarray((atlas * 255).round().astype(np.uint8), 'RGB').convert('RGBA').save(out, optimize=True)
print('wrote', out, os.path.getsize(out), 'bytes')
