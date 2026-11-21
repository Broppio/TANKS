"""Converts the Pak 38 OBJ into a Simple Bedrock Model geometry for WarSteel.

Run with Blender (headless):
  blender -b --python tools/pak38_export.py -- <pak38.obj> <out.geo.json> <ratio_base> <ratio_carriage> <ratio_gun>

The OBJ is one mesh of ~500 loose pieces. Each piece is sorted into the gun's moving parts by
where it sits (OBJ units, after import: x right, y forward, z up; 1 unit = 7.7 mm):
  base     - wheels, axle, trails and lower shield (static)
  carriage - shield, upper carriage, handwheels (traverses, bone "turret")
  gun      - barrel, cradle, breech, sight (elevates, bone "barrel")
Each part is decimated separately, scaled (real size x 1.2, like the other WarSteel vehicles),
converted to model space (16 px per block, front = -z, up = +y) and written as poly_mesh bones.
The two textures are stacked into one atlas (see tools/pak38_atlas.py): tex_8419 top, tex_9059 bottom.
"""
import bpy, json, math, sys
from mathutils import Vector

argv = sys.argv[sys.argv.index('--') + 1:]
SRC, OUT = argv[0], argv[1]
RATIOS = {'base': float(argv[2]), 'carriage': float(argv[3]), 'gun': float(argv[4]), 'tube': float(argv[4])}

K = 0.0077 * 1.2 * 16.0          # OBJ units -> model pixels
Y0 = 20.0                        # traverse axis (forward position), OBJ units
TRAVERSE_Z = 52.0                # height of the upper carriage pivot
TRUNNION_Y, TRUNNION_Z = -5.0, 104.0
ROW = {'tex_8419_0': 0, 'tex_9059_0': 1}


def to_model(v):
    return [round(v.x * K, 3), round(v.z * K, 3), round(-(v.y - Y0) * K, 3)]


def to_model_dir(n):
    return [n.x, n.z, -n.y]


def mc_space(p):
    """SBM mirrors x when it bakes a poly mesh: file (x, y, z) -> render (-x, y, z)."""
    return [-p[0], p[1], p[2]]


def cross(a, b):
    return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]


def classify(o):
    ws = [o.matrix_world @ v.co for v in o.data.vertices]
    mn = Vector([min(p[i] for p in ws) for i in range(3)])
    mx = Vector([max(p[i] for p in ws) for i in range(3)])
    c = (mn + mx) / 2
    width = mx.x - mn.x
    if abs(c.x) > 85 or c.z < 52 or c.y < -150 or (c.y < -95 and abs(c.x) > 28):
        return 'base'
    if abs(c.x) < 16 and c.z > 80 and width < 40:
        return 'gun'
    return 'carriage'


bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.wm.obj_import(filepath=SRC)
src = [o for o in bpy.data.objects if o.type == 'MESH'][0]
bpy.context.view_layer.objects.active = src
src.select_set(True)
bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
bpy.ops.object.mode_set(mode='EDIT')
bpy.ops.mesh.select_all(action='SELECT')
bpy.ops.mesh.separate(type='LOOSE')
bpy.ops.object.mode_set(mode='OBJECT')

groups = {'base': [], 'carriage': [], 'gun': [], 'tube': []}
for o in list(bpy.data.objects):
    if o.type == 'MESH':
        g = classify(o)
        if g == 'gun':
            ws = [o.matrix_world @ v.co for v in o.data.vertices]
            mx_y = max(p.y for p in ws)
            cy = (min(p.y for p in ws) + mx_y) / 2
            cx = (min(p.x for p in ws) + max(p.x for p in ws)) / 2
            # The tube (with its muzzle brake and breech ring) recoils; the cradle and sight don't.
            if mx_y > 250 or cy > 150 or (cy < -85 and abs(cx) < 14):
                g = 'tube'
        groups[g].append(o)

parts = {}
for g, objs in groups.items():
    bpy.ops.object.select_all(action='DESELECT')
    for o in objs:
        o.select_set(True)
    bpy.context.view_layer.objects.active = objs[0]
    bpy.ops.object.join()
    o = bpy.context.view_layer.objects.active
    o.name = g
    before = sum(len(p.vertices) - 2 for p in o.data.polygons)
    dec = o.modifiers.new('dec', 'DECIMATE')
    dec.decimate_type = 'COLLAPSE'
    dec.ratio = RATIOS[g]
    dec.use_collapse_triangulate = True
    bpy.ops.object.modifier_apply(modifier='dec')
    tri = o.modifiers.new('tri', 'TRIANGULATE')
    bpy.ops.object.modifier_apply(modifier='tri')
    print('PART', g, 'pieces', len(objs), 'tris', before, '->', len(o.data.polygons))
    parts[g] = o


def poly_mesh(o):
    positions, uvs, normals, polys, pindex, nindex = [], [], [], [], {}, {}
    me = o.data
    me.calc_normals_split()
    uvl = me.uv_layers.active.data
    for p in me.polygons:
        mat = me.materials[p.material_index].name if me.materials else 'tex_8419_0'
        row = ROW.get(mat.split('.')[0], 0)
        raw = [tuple(uvl[li].uv) for li in p.loop_indices]
        # Tiled UVs: move the whole face back into the 0..1 tile it mostly sits in.
        su = math.floor(min(u for u, _ in raw) + 1e-4)
        sv = math.floor(min(v for _, v in raw) + 1e-4)
        corners = []
        for li, (u, v) in zip(p.loop_indices, raw):
            vi = me.loops[li].vertex_index
            if vi not in pindex:
                pindex[vi] = len(positions)
                positions.append(to_model(me.vertices[vi].co))
            n = to_model_dir(me.loops[li].normal.normalized())
            nk = tuple(round(c, 3) for c in n)
            if nk not in nindex:
                nindex[nk] = len(normals)
                normals.append(list(nk))
            u = min(max(u - su, 0.0), 1.0)
            v = min(max(v - sv, 0.0), 1.0)
            # SBM stores v bottom-up (it flips v itself): tile row 0 is the top half.
            uvs.append([round(u, 5), round((v + (1 - row)) / 2.0, 5)])
            corners.append([pindex[vi], nindex[nk], len(uvs) - 1])
        a, b, c = (mc_space(positions[k[0]]) for k in corners[:3])
        fn = mc_space(to_model_dir(p.normal.normalized()))
        e = cross([b[i] - a[i] for i in range(3)], [c[i] - a[i] for i in range(3)])
        if sum(e[i] * fn[i] for i in range(3)) < 0:
            corners = [corners[0], corners[2], corners[1]]
        corners.append(corners[2])
        polys.append(corners)
    return {'normalized_uvs': True, 'positions': positions, 'normals': normals, 'uvs': uvs, 'polys': polys}


tv_y = round(TRAVERSE_Z * K, 3)
tr_y = round(TRUNNION_Z * K, 3)
tr_z = round(-(TRUNNION_Y - Y0) * K, 3)
bones = [
    {'name': 'pak38', 'pivot': [0, 0, 0]},
    {'name': 'base', 'parent': 'pak38', 'pivot': [0, 0, 0], 'poly_mesh': poly_mesh(parts['base'])},
    {'name': 'turret', 'parent': 'pak38', 'pivot': [0, tv_y, 0]},
    {'name': 'carriage', 'parent': 'turret', 'pivot': [0, tv_y, 0], 'poly_mesh': poly_mesh(parts['carriage'])},
    {'name': 'barrel', 'parent': 'turret', 'pivot': [0, tr_y, tr_z]},
    {'name': 'gun', 'parent': 'barrel', 'pivot': [0, tr_y, tr_z], 'poly_mesh': poly_mesh(parts['gun'])},
    # Recoils through the cradle (animation.cannon.fire); the flare bone is Superb Warfare's muzzle flash.
    {'name': 'tube', 'parent': 'barrel', 'pivot': [0, tr_y, tr_z], 'poly_mesh': poly_mesh(parts['tube'])},
    {'name': 'flare', 'parent': 'tube', 'pivot': [0, round(112.5 * K, 3), round(-(309.0 - Y0) * K, 3)]},
]
geo = {
    'format_version': '1.12.0',
    'minecraft:geometry': [{
        'description': {'identifier': 'geometry.pak-38', 'texture_width': 1024, 'texture_height': 1024,
                        'visible_bounds_width': 8, 'visible_bounds_height': 3, 'visible_bounds_offset': [0, 1, 0]},
        'bones': bones,
    }],
}
json.dump(geo, open(OUT, 'w'), separators=(',', ':'))
for b in bones:
    if 'poly_mesh' in b:
        print('BONE', b['name'], 'tris', len(b['poly_mesh']['polys']), 'verts', len(b['poly_mesh']['positions']))
print('TURRET_POS blocks', tv_y / 16, 'BARREL_POS blocks', (tr_y - tv_y) / 16, -tr_z / 16)
print('MUZZLE from trunnion blocks', (309.0 - TRUNNION_Y) * K / 16)
