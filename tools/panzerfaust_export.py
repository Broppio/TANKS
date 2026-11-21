#!/usr/bin/env python3
"""Builds the Panzerfaust 60 assets from its OBJ model.

Usage: python3 tools/panzerfaust_export.py <Panzerfaust.obj> <textures folder> <superbwarfare jar>

Writes (under src/main/resources/assets/warsteel):
  models/panzerfaust/panzerfaust_60.mesh.json  triangle mesh, parts "tube", "sights" and "warhead"
  textures/item/panzerfaust_60.png             2x2 atlas: sights | tube / warhead | -
  geo/panzerfaust_60.geo.json                  GeckoLib skeleton (arms, camera, recoil and flare bones)
  animations/panzerfaust_60.animation.json     first-person animations
  textures/item/panzerfaust_60_icon.png, textures/gun_icon/panzerfaust_60_icon.png

Superb Warfare guns render through GeckoLib, which only draws cubes. The skeleton is Superb
Warfare's RPG-7 rig with its geometry removed (it carries the first-person arm poses, camera shake
and recoil), and PanzerfaustItemRenderer draws this mesh at the "panzerfaust" bone instead.

OBJ space: x lateral, y up, z forward (warhead), 1 unit = 2.79 cm (tube 0.8 m, warhead 150 mm).
Mesh space: Minecraft render units (1 = 1 block) in the bone's frame: front = -z, up = +y.
"""
import json, math, os, sys, zipfile
from PIL import Image, ImageDraw

OBJ, TEX, SBW_JAR = sys.argv[1], sys.argv[2], sys.argv[3]
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/warsteel')

PX = 1.813        # model pixels per OBJ unit: fits the 1.045 m Panzerfaust over the RPG's span
Z0 = -7.5         # pixel offset placing the warhead tip at the RPG warhead's tip (z = -38 px)
Y0 = 1.46         # bore axis height of the RPG rig, pixels
TILE = {'Sights': (0, 0), 'Tube': (1, 0), 'Warhead': (0, 1)}
PART = {'Sights': 'sights', 'Tube': 'tube', 'Warhead': 'warhead'}
FILES = {'Sights': 'panzersights.tga.png', 'Tube': 'panzertube.tga.png', 'Warhead': 'Panzerwarhead.tga.png'}


def to_render(x, y, z):
    """OBJ -> render space in blocks (a 180-degree turn about y, so no mirroring)."""
    return [round(-x * PX / 16, 5), round((y * PX + Y0) / 16, 5), round((-z * PX + Z0) / 16, 5)]


# ------------------------------------------------------------------------------------- mesh
V, VT, VN = [], [], []
faces = []
mat = None
for line in open(OBJ):
    p = line.split()
    if not p:
        continue
    if p[0] == 'v':
        V.append(tuple(map(float, p[1:4])))
    elif p[0] == 'vt':
        VT.append(tuple(map(float, p[1:3])))
    elif p[0] == 'vn':
        VN.append(tuple(map(float, p[1:4])))
    elif p[0] == 'usemtl':
        mat = p[1]
    elif p[0] == 'f':
        corners = []
        for t in p[1:]:
            i = t.split('/')
            corners.append((int(i[0]) - 1, int(i[1]) - 1 if len(i) > 1 and i[1] else -1, int(i[2]) - 1 if len(i) > 2 and i[2] else -1))
        faces.append((mat, corners))

parts = {'tube': [], 'sights': [], 'warhead': []}
for m, corners in faces:
    col, row = TILE[m]
    uvs = [VT[c[1]] if c[1] >= 0 else (0.0, 0.0) for c in corners]
    # Tiled UVs: shift the whole face into the 0..1 tile it starts in (the atlas can't wrap).
    su = math.floor(min(u for u, _ in uvs) + 1e-4)
    sv = math.floor(min(v for _, v in uvs) + 1e-4)
    verts = []
    for c, (u, v) in zip(corners, uvs):
        pos = to_render(*V[c[0]])
        n = VN[c[2]] if c[2] >= 0 else (0.0, 1.0, 0.0)
        u = min(max(u - su, 0.0), 1.0)
        v = min(max(v - sv, 0.0), 1.0)
        # OBJ v is bottom-up; Minecraft textures are top-down.
        verts.append(pos + [round((col + u) / 2, 5), round((row + 1 - v) / 2, 5), round(-n[0], 4), round(n[1], 4), round(-n[2], 4)])
    for i in range(1, len(verts) - 1):          # fan-triangulate quads and n-gons
        parts[PART[m]].extend(verts[0] + verts[i] + verts[i + 1])

os.makedirs(os.path.join(ASSETS, 'models/panzerfaust'), exist_ok=True)
json.dump({k: v for k, v in parts.items()}, open(os.path.join(ASSETS, 'models/panzerfaust/panzerfaust_60.mesh.json'), 'w'), separators=(',', ':'))
for k, v in parts.items():
    print('part', k, len(v) // 24, 'triangles')

# ------------------------------------------------------------------------------------- atlas
atlas = Image.new('RGBA', (1024, 1024), (60, 64, 52, 255))
for m, (col, row) in TILE.items():
    atlas.paste(Image.open(os.path.join(TEX, FILES[m])).convert('RGBA').resize((512, 512), Image.LANCZOS), (col * 512, row * 512))
atlas.save(os.path.join(ASSETS, 'textures/item/panzerfaust_60.png'), optimize=True)

# ------------------------------------------------------------------------------------- skeleton + animations
jar = zipfile.ZipFile(SBW_JAR)
geo = json.loads(jar.read('assets/superbwarfare/geo/rpg.geo.json'))
g = geo['minecraft:geometry'][0]
g['description']['identifier'] = 'geometry.panzerfaust_60'
g['description']['texture_width'] = g['description']['texture_height'] = 1024
for b in g['bones']:
    if b['name'] not in ('Lefthand', 'Righthand'):   # the arms are drawn as the player's own
        b.pop('cubes', None)
    if b['name'] == 'flare':
        b['pivot'] = [0, Y0, round(-8.24 * PX + Z0, 3)]   # front of the tube, behind the warhead
g['bones'].append({'name': 'panzerfaust', 'parent': 'rpg', 'pivot': [0, 0, 0]})
os.makedirs(os.path.join(ASSETS, 'geo'), exist_ok=True)
json.dump(geo, open(os.path.join(ASSETS, 'geo/panzerfaust_60.geo.json'), 'w'), indent=1)

anim = json.loads(jar.read('assets/superbwarfare/animations/rpg.animation.json'))
anims = {k.replace('animation.rpg.', 'animation.panzerfaust_60.'): v for k, v in anim['animations'].items() if not k.endswith('reload')}
os.makedirs(os.path.join(ASSETS, 'animations'), exist_ok=True)
json.dump({'format_version': anim.get('format_version', '1.8.0'), 'animations': anims},
          open(os.path.join(ASSETS, 'animations/panzerfaust_60.animation.json'), 'w'), indent=1)
display = json.loads(jar.read('assets/superbwarfare/models/displaysettings/rpg.item.json'))
os.makedirs(os.path.join(ASSETS, 'models/displaysettings'), exist_ok=True)
json.dump(display, open(os.path.join(ASSETS, 'models/displaysettings/panzerfaust_60.item.json'), 'w'), indent=2)


# ------------------------------------------------------------------------------------- icons
def silhouette(w, h, colour, side=True):
    pts = [p for k in ('tube', 'warhead', 'sights') for p in [parts[k][i:i + 24] for i in range(0, len(parts[k]), 24)]]
    tris = [[(t[j * 8 + 2], t[j * 8 + 1]) for j in range(3)] for t in pts]   # side view: z across, y up
    allp = [p for t in tris for p in t]
    mnx, mxx = min(p[0] for p in allp), max(p[0] for p in allp)
    mny, mxy = min(p[1] for p in allp), max(p[1] for p in allp)
    s = min((w * 8 - 16) / (mxx - mnx), (h * 8 - 16) / (mxy - mny))
    im = Image.new('L', (w * 8, h * 8), 0)
    d = ImageDraw.Draw(im)
    for t in tris:
        d.polygon([(w * 4 + (x - (mnx + mxx) / 2) * s, h * 4 - (y - (mny + mxy) / 2) * s) for x, y in t], fill=255)
    out = Image.new('RGBA', (w, h), colour + (0,))
    out.putalpha(im.resize((w, h), Image.LANCZOS))
    return out


os.makedirs(os.path.join(ASSETS, 'textures/gun_icon'), exist_ok=True)
silhouette(64, 16, (255, 255, 255)).save(os.path.join(ASSETS, 'textures/gun_icon/panzerfaust_60_icon.png'))
# Inventory icon: a shaded side view coloured from the atlas, drawn diagonally like other launchers.
tex = atlas.convert('RGB')
W = 256
tris = []
for k in ('tube', 'warhead', 'sights'):
    arr = parts[k]
    for i in range(0, len(arr), 24):
        t = arr[i:i + 24]
        cu = sum(t[j * 8 + 3] for j in range(3)) / 3
        cv = sum(t[j * 8 + 4] for j in range(3)) / 3
        c = tex.getpixel((min(1023, int(cu * 1024)), min(1023, int(cv * 1024))))
        n = (sum(t[j * 8 + 5] for j in range(3)), sum(t[j * 8 + 6] for j in range(3)), sum(t[j * 8 + 7] for j in range(3)))
        ln = math.sqrt(sum(x * x for x in n)) or 1
        shade = 0.55 + 0.45 * max(0.0, (0.3 * n[0] + 0.8 * n[1] + 0.5 * n[2]) / ln)
        depth = sum(t[j * 8] for j in range(3)) / 3            # view from +x
        tris.append((depth, [(t[j * 8 + 2], t[j * 8 + 1]) for j in range(3)], tuple(int(ch * shade) for ch in c)))
allp = [p for _, t, _ in tris for p in t]
mnx, mxx = min(p[0] for p in allp), max(p[0] for p in allp)
mny, mxy = min(p[1] for p in allp), max(p[1] for p in allp)
sc = (W - 24) / (mxx - mnx)
im = Image.new('RGBA', (W, W), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
for _, t, c in sorted(tris, key=lambda x: x[0]):
    d.polygon([(W / 2 + (x - (mnx + mxx) / 2) * sc, W / 2 - (y - (mny + mxy) / 2) * sc) for x, y in t], fill=c + (255,))
im = im.rotate(-35, resample=Image.BICUBIC)
im.resize((32, 32), Image.LANCZOS).save(os.path.join(ASSETS, 'textures/item/panzerfaust_60_icon.png'))
print('done')
