"""Converts the T-22 (Elbing-class torpedo boat) Blender scene into a Simple Bedrock Model geometry.

Run with Blender (headless):
  blender -b T-22.blend --python tools/t22_export.py -- <out.geo.json> <out_pivots.json>

The scene is 1:1 in metres (97 m long), with x forward, y to port, z up and the waterline at z = 0.
The model keeps that scale (16 px per block = 1 m), with its origin on the keel (z = -3.3) amidships,
front = -z and up = +y.

Moving parts become bones:
  turret / barrel             mount A (forward), aimed by Superb Warfare from the captain's seat
  turret_b..d / barrel_b..d    mounts B-D, slaved to A by WarSteel's renderer (C and D face aft)
  flakz_p / flakz_s (+ _gun)  3.7 cm Flakzwilling, port and starboard (gunner seats)
  flakv / flakv_gun           2 cm Flakvierling (gunner seat)
  flak_1..4 / flak_1..4_gun   single 2 cm Flak on the bridge wings
  torp_1 / torp_2             triple torpedo tube mounts
  prop_l / prop_r, rudder_l / rudder_r, radar
Railings (SideFence) are dropped: they tile one texture hundreds of times and are too thin to see.
Each part is decimated (see RATIO) and all textures are packed into one 2048 atlas (see ATLAS).
"""
import bpy, json, math, sys

argv = sys.argv[sys.argv.index('--') + 1:]
OUT, PIVOTS = argv[0], argv[1]
TEX_DIR = argv[2] if len(argv) > 2 else None

S = 16.0          # px per metre
KEEL = -3.3       # waterline is z = 0; the model origin sits on the keel
X0 = 1.3          # amidships (hull runs x = -47.3 .. 49.9)

# Atlas: 4x4 cells of 512 px; (col, row, span)
ATLAS = {
    'Mat_T-22_Hull': (0, 0, 2), 'Mat_T-22_Props': (2, 0, 1), 'Mat_T-22_Main': (3, 0, 1),
    'Mat_T-22_TorpedoTube': (2, 1, 1), 'Mat_T-22_Vessel': (3, 1, 1), 'Mat_Flakvierling': (0, 2, 1),
    'Mat_AA_Flak.001': (1, 2, 1), 'Mat_Flakzwilling_3': (2, 2, 1), 'Mat_T-22_Radar_1': (3, 2, 1),
    'Mat_T-22_Rudder': (0, 3, 1), 'Mat_T-22_Propellor': (1, 3, 1), 'CABLE': (2, 3, 1),
}
# Decimation ratio by part (fraction of triangles kept).
RATIO = {'T-22_Props': 0.22, 'T-22_Hull': 0.7, 'T-22_TorpedoTubes': 0.35, 'T-22_Main_turret': 0.45, 'T-22_Main_barrel': 0.35,
         'Flakvierling _Turret': 0.4, 'Flakvierling _Barrel': 0.35, 'Flakzwilling2_turret': 0.45, 'Flakzwilling2_barrel': 0.45,
         'Flak_turret': 0.4, 'Flak_barrel': 0.4, 'T-22_Radar_2': 0.4, 'T-22_Radar_1': 0.6, 'T-22_Vessel': 0.5}


def ratio_for(name):
    for k, v in RATIO.items():
        if name.startswith(k):
            return v
    return 1.0


def to_model(v):
    return [round(-v.y * S, 3), round((v.z - KEEL) * S, 3), round(-(v.x - X0) * S, 3)]


def to_model_dir(n):
    return [-n.y, n.z, -n.x]


def mc_space(p):
    return [-p[0], p[1], p[2]]


def cross(a, b):
    return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]


# --------------------------------------------------------------------------------------- prepare
objs = {o.name: o for o in bpy.data.objects if o.type == 'MESH'}
origins = {n: o.matrix_world.translation.copy() for n, o in objs.items()}
for n, o in objs.items():
    if n.startswith('T-22_SideFence'):
        continue
    bpy.ops.object.select_all(action='DESELECT')
    bpy.context.view_layer.objects.active = o
    o.select_set(True)
    o.data = o.data.copy()          # instanced meshes: make each object's mesh its own
    r = ratio_for(n)
    if r < 0.999:
        d = o.modifiers.new('dec', 'DECIMATE')
        d.decimate_type = 'COLLAPSE'
        d.ratio = r
        d.use_collapse_triangulate = True
    t = o.modifiers.new('tri', 'TRIANGULATE')
    deps = bpy.context.evaluated_depsgraph_get()
    ev = o.evaluated_get(deps)
    o['_mesh'] = None
    me = bpy.data.meshes.new_from_object(ev, preserve_all_data_layers=True, depsgraph=deps)
    me.transform(o.matrix_world)
    o['_mesh_name'] = me.name


def poly_mesh(names, cable=False):
    positions, uvs, normals, polys, pindex, nindex = [], [], [], [], {}, {}
    for n in names:
        o = objs[n]
        me = bpy.data.meshes[o['_mesh_name']]
        me.calc_normals_split()
        uvl = me.uv_layers[0].data if me.uv_layers else None
        mat = me.materials[0].name if me.materials else ''
        key = 'CABLE' if n.startswith('T-22_Cables') else mat
        col, row, span = ATLAS.get(key, ATLAS['CABLE'])
        base = len(positions)
        for p in me.polygons:
            raw = [tuple(uvl[li].uv) if uvl else (0.5, 0.5) for li in p.loop_indices]
            if key == 'CABLE':
                raw = [(0.5, 0.5)] * len(raw)
            su = math.floor(min(u for u, _ in raw) + 1e-4)
            sv = math.floor(min(v for _, v in raw) + 1e-4)
            corners = []
            for li, (u, v) in zip(p.loop_indices, raw):
                vi = me.loops[li].vertex_index
                pk = (n, vi)
                if pk not in pindex:
                    pindex[pk] = len(positions)
                    positions.append(to_model(me.vertices[vi].co))
                nn = to_model_dir(me.loops[li].normal.normalized())
                nk = tuple(round(c, 3) for c in nn)
                if nk not in nindex:
                    nindex[nk] = len(normals)
                    normals.append(list(nk))
                u = min(max(u - su, 0.0), 1.0)
                v = min(max(v - sv, 0.0), 1.0)
                # Atlas cell; SBM stores v bottom-up.
                au = (col + u * span) / 4.0
                av = 1.0 - (row + (1.0 - v) * span) / 4.0
                uvs.append([round(au, 5), round(av, 5)])
                corners.append([pindex[pk], nindex[nk], len(uvs) - 1])
            a, b, c = (mc_space(positions[k[0]]) for k in corners[:3])
            fn = mc_space(to_model_dir(p.normal.normalized()))
            e = cross([b[i] - a[i] for i in range(3)], [c[i] - a[i] for i in range(3)])
            if sum(e[i] * fn[i] for i in range(3)) < 0:
                corners = [corners[0], corners[2], corners[1]]
            corners.append(corners[2])
            polys.append(corners)
    return {'normalized_uvs': True, 'positions': positions, 'normals': normals, 'uvs': uvs, 'polys': polys}


def piv(name):
    return to_model(origins[name])


bones = [{'name': 't22', 'pivot': [0, 0, 0]}]
pivots = {}


def add(name, parent, pivot, meshes=None):
    b = {'name': name, 'parent': parent, 'pivot': pivot}
    if meshes:
        b['poly_mesh'] = poly_mesh(meshes)
    bones.append(b)
    pivots[name] = pivot


add('hull', 't22', [0, 0, 0], ['T-22_Hull', 'T-22_Props', 'T-22_Vessel_1', 'T-22_Vessel_2', 'T-22_Cables'])
# Main mounts. Mount A uses Superb Warfare's own "turret"/"barrel" bones.
for suffix, tn, bn in (('', 'T-22_Main_turret', 'T-22_Main_barrel'), ('_b', 'T-22_Main_turret.001', 'T-22_Main_barrel.001'),
                       ('_c', 'T-22_Main_turret.002', 'T-22_Main_barrel.002'), ('_d', 'T-22_Main_turret.003', 'T-22_Main_barrel.003')):
    add('turret' + suffix, 't22', piv(tn), [tn])
    add('barrel' + suffix, 'turret' + suffix, piv(bn), [bn])
    # Muzzle: the barrel's far end along its own axis (x in Blender).
    bo = objs[bn]
    xs = [(bo.matrix_world @ v.co).x for v in bo.data.vertices]
    tip_x = max(xs) if abs(max(xs) - origins[bn].x) > abs(min(xs) - origins[bn].x) else min(xs)
    m = origins[bn].copy()
    m.x = tip_x
    add('flare' + suffix, 'barrel' + suffix, to_model(m))
for name, tn, bn in (('flakz_p', 'Flakzwilling2_turret.000', 'Flakzwilling2_barrel.000'), ('flakz_s', 'Flakzwilling2_turret.002', 'Flakzwilling2_barrel.002'),
                     ('flakv', 'Flakvierling _Turret', 'Flakvierling _Barrel'), ('flak_1', 'Flak_turret', 'Flak_barrel'),
                     ('flak_2', 'Flak_turret.001', 'Flak_barrel.001'), ('flak_3', 'Flak_turret.002', 'Flak_barrel.002'), ('flak_4', 'Flak_turret.003', 'Flak_barrel.003')):
    add(name, 't22', piv(tn), [tn])
    add(name + '_gun', name, piv(bn), [bn])
add('torp_1', 't22', piv('T-22_TorpedoTubes'), ['T-22_TorpedoTubes'])
add('torp_2', 't22', piv('T-22_TorpedoTubes.001'), ['T-22_TorpedoTubes.001'])
add('radar', 't22', piv('T-22_Radar_2'), ['T-22_Radar_2', 'T-22_Radar_1'])
add('prop_l', 't22', piv('T-22_Propellor.001'), ['T-22_Propellor.001'])
add('prop_r', 't22', piv('T-22_Propellor'), ['T-22_Propellor'])
add('rudder_l', 't22', piv('T-22_Rudder.001'), ['T-22_Rudder.001'])
add('rudder_r', 't22', piv('T-22_Rudder'), ['T-22_Rudder'])

geo = {'format_version': '1.12.0', 'minecraft:geometry': [{
    'description': {'identifier': 'geometry.t-22', 'texture_width': 2048, 'texture_height': 2048,
                    'visible_bounds_width': 110, 'visible_bounds_height': 35, 'visible_bounds_offset': [0, 12, 0]},
    'bones': bones}]}
json.dump(geo, open(OUT, 'w'), separators=(',', ':'))
json.dump(pivots, open(PIVOTS, 'w'), indent=1)
total = 0
for b in bones:
    if 'poly_mesh' in b:
        total += len(b['poly_mesh']['polys'])
        print('BONE', b['name'], len(b['poly_mesh']['polys']))
print('TOTAL', total)
for n in ('T-22_Main_turret', 'T-22_Main_barrel', 'T-22_Main_turret.002', 'Flakzwilling2_turret.000', 'Flak_turret', 'T-22_TorpedoTubes'):
    print('ORIGIN', n, tuple(round(c, 2) for c in origins[n]))
