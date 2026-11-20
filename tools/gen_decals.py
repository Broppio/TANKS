#!/usr/bin/env python3
"""Generates the hit-mark atlas (assets/warsteel/textures/misc/hit_decals.png): 2x2 cells of 32x32.
(0,0) penetration hole, (1,0) non-penetration gouge, (0,1) ricochet scrape, (1,1) HE scorch."""
import math, os, random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'src/main/resources/assets/warsteel/textures/misc/hit_decals.png')
rnd = random.Random(88)
img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
px = img.load()


def cell(cx0, cy0, fn):
    for y in range(32):
        for x in range(32):
            dx, dy = x - 15.5, y - 15.5
            c = fn(dx, dy, math.hypot(dx, dy), math.atan2(dy, dx))
            if c:
                px[cx0 + x, cy0 + y] = tuple(max(0, min(255, int(v))) for v in c)


def hole(dx, dy, r, a):
    edge = 5.0 + 0.9 * math.sin(a * 5 + 1.3) + rnd.uniform(-0.4, 0.4)
    if r < edge:
        return (8, 6, 5, 255)                                  # through the plate
    if r < edge + 2.2:
        return (150, 120, 95, 255)                             # bright torn steel lip
    soot = 1.0 - (r - edge - 2.2) / 8.5 + rnd.uniform(-0.12, 0.12)
    if soot > 0:
        g = 30 + rnd.randint(0, 20)
        return (g, g - 4, g - 8, 255 * min(1.0, soot * 1.15))
    return None


def gouge(dx, dy, r, a):
    edge = 6.0 + 0.7 * math.sin(a * 4) + rnd.uniform(-0.4, 0.4)
    if r < edge * 0.55:
        v = 175 + rnd.randint(-15, 15)
        return (v, v, v + 8, 255)                              # bright bared metal
    if r < edge:
        v = 110 + rnd.randint(-15, 15)
        return (v, v - 4, v - 8, 255)
    soot = 1.0 - (r - edge) / 7.0 + rnd.uniform(-0.1, 0.1)
    if soot > 0:
        return (45, 40, 35, 220 * min(1.0, soot))
    return None


def scrape(dx, dy, r, a):
    w = 2.2 * max(0.0, 1.0 - abs(dx) / 15.0) + 0.4
    if abs(dy) < w and abs(dx) < 15:
        v = 185 + rnd.randint(-20, 20) - abs(dx) * 3
        return (v, v, v + 6, 255 * (1.0 - abs(dx) / 16.0))
    if abs(dy) < w + 2.5 and abs(dx) < 14:
        return (40, 36, 32, 120)
    return None


def scorch(dx, dy, r, a):
    lim = 13.0 + 2.5 * math.sin(a * 7 + 0.4) + 1.5 * math.sin(a * 3)
    if r < lim:
        k = 1.0 - (r / lim) ** 2 + rnd.uniform(-0.15, 0.15)
        g = 18 + rnd.randint(0, 22)
        return (g, g - 3, g - 6, 255 * max(0.0, min(1.0, k * 1.2)))
    return None


cell(0, 0, hole)
cell(32, 0, gouge)
cell(0, 32, scrape)
cell(32, 32, scorch)
os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT)
print('wrote', OUT)
