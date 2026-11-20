"""Converts the Flak 36 Blender scene into a Simple Bedrock Model geometry for WarSteel.

Run with Blender (headless):
  blender -b fLAK3.blend --python tools/flak88_export.py -- <out.geo.json> <ratio_gun> <ratio_carriage> <ratio_base> <ratio_base_detail>

The scene is one mesh with four materials, which map onto the gun's moving parts:
  "Mat pied" + "Mat pied detail" -> base   (cruciform platform, static)
  "mat sous haut"                -> turret (top carriage, traverses)
  "Mat haut"                     -> barrel (gun and cradle, elevates)
Each part is decimated separately, scaled to the real gun (x1.35: 2.2 m tall, 5.4 m outrigger
span), converted to model space (16 px per block, front = -z, up = +y) and written as poly_mesh
bones. UVs are remapped into a 2x2 atlas (see tools/flak88_atlas.py):
  haut | sous haut
  pied | pied detail
"""
import bpy, json, math, sys

argv = sys.argv[sys.argv.index('--') + 1:]
OUT = argv[0]
RATIOS = {'gun': float(argv[1]), 'carriage': float(argv[2]), 'base': float(argv[3]), 'base_detail': float(argv[4])}

SCALE = 1.35 * 16.0           # Blender metres -> model pixels at real size
XC, YC = -0.50, -0.065        # traverse axis (centre of the cruciform base), Blender world
TRUNNION_X, TRUNNION_Z = -0.42, 1.20   # elevation axis, Blender world

TILE = {'Mat haut': (0, 0), 'mat sous haut': (1, 0), 'Mat pied': (0, 1), 'Mat pied detail': (1, 1)}
PART = {'Mat haut': 'gun', 'mat sous haut': 'carriage', 'Mat pied': 'base', 'Mat pied detail': 'base_detail'}


def to_model(v):
    """Blender world (x forward, y left, z up) -> model px (front -z, up +y)."""
    return [round(-(v.y - YC) * SCALE, 3), round(v.z * SCALE, 3), round(-(v.x - XC) * SCALE, 3)]


src = bpy.data.objects['Asset Flak']
bpy.context.view_layer.objects.active = src
for o in bpy.context.view_layer.objects:
    o.select_set(o == src)
bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
bpy.ops.object.mode_set(mode='EDIT')
bpy.ops.mesh.select_all(action='SELECT')
bpy.ops.mesh.separate(type='MATERIAL')
bpy.ops.object.mode_set(mode='OBJECT')

meshes = {}
for o in list(bpy.context.view_layer.objects):
    if o.type != 'MESH' or not o.data.materials:
        continue
    mat = o.data.materials[0].name
    part = PART.get(mat)
    if part is None:
        continue
    bpy.context.view_layer.objects.active = o
    dec = o.modifiers.new('dec', 'DECIMATE')
    dec.decimate_type = 'COLLAPSE'
    dec.ratio = RATIOS[part]
    dec.use_collapse_triangulate = True
    bpy.ops.object.modifier_apply(modifier='dec')
    tri = o.modifiers.new('tri', 'TRIANGULATE')
    bpy.ops.object.modifier_apply(modifier='tri')
    meshes.setdefault(part, []).append((o, mat))


def poly_mesh(parts):
    positions, uvs, polys, pindex = [], [], [], {}
    for o, mat in parts:
        me = o.data
        uvl = me.uv_layers.active.data
        col, row = TILE[mat]
        for p in me.polygons:
            corners = []
            for li in p.loop_indices:
                vi = me.loops[li].vertex_index
                pk = (o.name, vi)
                if pk not in pindex:
                    pindex[pk] = len(positions)
                    positions.append(to_model(o.matrix_world @ me.vertices[vi].co))
                u, v = uvl[li].uv
                u = min(max(u, 0.0), 1.0)
                v = min(max(v, 0.0), 1.0)
                au = (u + col) / 2.0
                av = 1.0 - (v + (1 - row)) / 2.0   # top-down, like Blockbench exports
                uvs.append([round(au, 5), round(av, 5)])
                corners.append([pindex[pk], 0, len(uvs) - 1])
            if len(corners) == 3:
                corners.append(corners[2])
            polys.append(corners)
    return {'normalized_uvs': True, 'positions': positions, 'normals': [[0, 1, 0]], 'uvs': uvs, 'polys': polys}


tr_y = TRUNNION_Z * SCALE
tr_z = -(TRUNNION_X - XC) * SCALE
bones = [
    {'name': 'flak88', 'pivot': [0, 0, 0]},
    {'name': 'base', 'parent': 'flak88', 'pivot': [0, 0, 0], 'poly_mesh': poly_mesh(meshes['base'] + meshes['base_detail'])},
    {'name': 'turret', 'parent': 'flak88', 'pivot': [0, 0.29 * SCALE, 0]},
    {'name': 'carriage', 'parent': 'turret', 'pivot': [0, 0.29 * SCALE, 0], 'poly_mesh': poly_mesh(meshes['carriage'])},
    {'name': 'barrel', 'parent': 'turret', 'pivot': [0, round(tr_y, 3), round(tr_z, 3)]},
    {'name': 'gun', 'parent': 'barrel', 'pivot': [0, round(tr_y, 3), round(tr_z, 3)], 'poly_mesh': poly_mesh(meshes['gun'])},
]
geo = {
    'format_version': '1.12.0',
    'minecraft:geometry': [{
        'description': {'identifier': 'geometry.flak-88', 'texture_width': 1024, 'texture_height': 1024,
                        'visible_bounds_width': 10, 'visible_bounds_height': 5, 'visible_bounds_offset': [0, 1.5, 0]},
        'bones': bones,
    }],
}
json.dump(geo, open(OUT, 'w'), separators=(',', ':'))
for b in bones:
    if 'poly_mesh' in b:
        print('BONE', b['name'], 'tris', len(b['poly_mesh']['polys']), 'verts', len(b['poly_mesh']['positions']))
print('TRUNNION px', tr_y, tr_z)
