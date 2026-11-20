"""Generates WarSteel armour plates and damage-model modules.
# Usage (from the project root): python3 tools/gen_armour.py src/main/resources/data/warsteel

Frames: 'Vehicle' (x = left+, y = up, z = front+, origin on the ground under the vehicle origin)
and 'Turret' (origin at the turret pivot, rotates with the turret). Plates sit on the faces of
Superb Warfare's hit boxes so a shell's impact point always lands on a plate.

Thickness values are the historical plate thickness; sloped plates carry an explicit outward
Normal so the angle of incidence uses the real slope.
"""
import json, math, os, sys

RES = sys.argv[1]  # src/main/resources/data/warsteel
ARMOUR = os.path.join(RES, 'warsteel', 'armor')
MODULES = os.path.join(RES, 'warsteel', 'modules')
os.makedirs(MODULES, exist_ok=True)

T_HALF = 0.06


def r(v):
    return [round(x, 4) for x in v]


def tilt(up_deg, axis='z', sign=1):
    """Outward normal of a plate facing +axis*sign, tilted upward by up_deg (negative = downward)."""
    a = math.radians(up_deg)
    n = [0.0, math.sin(a), 0.0]
    i = {'x': 0, 'z': 2}[axis]
    n[i] = sign * math.cos(a)
    return r(n)


def plate(name, transform, thickness, centre, half, ric=1.0, normal=None):
    d = {'Name': name, 'Transform': transform, 'Thickness': thickness, 'RicochetChance': ric,
         'Size': r(half), 'Position': r(centre)}
    if normal:
        d['Normal'] = normal
    return d


def faces(prefix, transform, c, h, spec, y_split=None):
    """Plates on the faces of box (centre c, half h). spec: face -> (thickness, ric, normal) or None.
    Faces: front(+z) rear(-z) left(+x) right(-x) roof(+y) floor(-y). 'front_upper'/'front_lower'
    split the front at y_split; 'left_upper'/'left_lower' (and right) split the sides likewise."""
    cx, cy, cz = c
    hx, hy, hz = h
    out = []

    def add(key, name, centre, half):
        s = spec.get(key)
        if s:
            s = list(s) if isinstance(s, (list, tuple)) else [s]
            t, ric, n = s[0], (s[1] if len(s) > 1 else 1.0), (s[2] if len(s) > 2 else None)
            out.append(plate(prefix + name, transform, t, centre, half, ric, n))

    lo, top = cy - hy, cy + hy
    ys = y_split if y_split is not None else cy
    up_c, up_h = (ys + top) / 2, (top - ys) / 2
    lw_c, lw_h = (lo + ys) / 2, (ys - lo) / 2
    if 'front' in spec:
        add('front', 'Front', (cx, cy, cz + hz), (hx, hy, T_HALF))
    else:
        add('front_upper', 'UpperFront', (cx, up_c, cz + hz), (hx, up_h, T_HALF))
        add('front_lower', 'LowerFront', (cx, lw_c, cz + hz), (hx, lw_h, T_HALF))
    add('rear', 'Rear', (cx, cy, cz - hz), (hx, hy, T_HALF))
    for side, sx, nm in (('left', 1, 'Left'), ('right', -1, 'Right')):
        if side in spec:
            add(side, 'Side' + nm, (cx + sx * hx, cy, cz), (T_HALF, hy, hz))
        else:
            add(side + '_upper', 'UpperSide' + nm, (cx + sx * hx, up_c, cz), (T_HALF, up_h, hz))
            add(side + '_lower', 'LowerSide' + nm, (cx + sx * hx, lw_c, cz), (T_HALF, lw_h, hz))
    add('roof', 'Roof', (cx, top, cz), (hx, T_HALF, hz))
    add('floor', 'Floor', (cx, lo, cz), (hx, T_HALF, hz))
    return out


def box(name, typ, pos, half, transform='Vehicle', **kw):
    d = {'Name': name, 'Type': typ, 'Transform': transform, 'Position': r(pos), 'Size': r(half)}
    d.update(kw)
    return d


def crew(name, role, pos, transform='Vehicle', half=(0.28, 0.42, 0.28), seat=None):
    kw = {'Role': role}
    if seat is not None:
        kw['Seat'] = seat
    return box(name, 'CREW', pos, half, transform, **kw)


def tracks(lc, lh, rc, rh, armour=20):
    return [box('TrackLeft', 'TRACK', lc, lh, Side='LEFT', Armour=armour),
            box('TrackRight', 'TRACK', rc, rh, Side='RIGHT', Armour=armour)]


def write(vid, armour_comment, ric, plates, model_comment, min_crew, modules, also=()):
    a = {'_comment': armour_comment, 'Vehicle': 'warsteel:' + vid, 'GeneralRicochetChance': ric, 'Plates': plates}
    m = {'_comment': model_comment, 'Vehicle': 'warsteel:' + vid, 'MinCrew': min_crew, 'MainGunSeat': 0,
         'RepairSeconds': 25, 'CrewReplaceSeconds': 60, 'CrewSwapSeconds': 5, 'FireSeconds': 14,
         'FallbackDamageMultiplier': 0.25, 'AmmoExplosionRadius': 6, 'Modules': modules}
    for v in (vid,) + tuple(also):
        a['Vehicle'] = m['Vehicle'] = 'warsteel:' + v
        json.dump(a, open(os.path.join(ARMOUR, v + '.json'), 'w'), indent=2)
        json.dump(m, open(os.path.join(MODULES, v + '.json'), 'w'), indent=2)


ARMOUR_NOTE = ("Armour plates on the faces of the hit boxes. Thickness is the real plate thickness in mm; "
               "Normal (optional) is the plate's outward direction in the Transform frame, used for the angle "
               "of incidence on sloped plates. Size is HALF-extents in blocks. Final ricochet chance = "
               "GeneralRicochetChance * RicochetChance * angle factor; overmatch forces a penetration.")
MODEL_NOTE = ("Internal modules for the War Thunder style damage model. Position/Size (half-extents) are in "
              "blocks in the Transform frame. Crew with a Seat pass part of the damage to the player in that seat. "
              "The tank is knocked out when an ammo rack detonates or fewer than MinCrew crew remain. "
              "Use /warsteel xray to see plates (orange) and modules in game.")

# ------------------------------------------------------------------------------------------------
# KV-1 (L-11 / F-32 era, 1940-41). Hull box x+-1.8 y0.55-1.75 z+-3.9; turret box (turret frame)
# centre (0,0.2,1.0) half (1.1,0.5,1.2). Hull 75 mm front (upper @30, lower @25), sides 75, rear 60,
# roof/floor 30-40. Turret 75 mm all round, mantlet 90.
HULL_KV = ((0, 1.15, 0), (1.8, 0.6, 3.9))
kv_hull = faces('', 'Vehicle', *HULL_KV, {
    'front_upper': (75, 1.0, tilt(30)), 'front_lower': (75, 0.8, tilt(-25)),
    'rear': (60, 0.9, tilt(15, sign=-1)), 'left': (75, 0.9), 'right': (75, 0.9),
    'roof': (30, 1.0), 'floor': (30, 1.0)})


def kv_turret(h, front, roof):
    c = (0, 0.2, 1.0)
    return [plate('Mantlet', 'Turret', 90 if front < 90 else front + 20, (0, 0.2, 2.2), (0.42, 0.32, T_HALF), 0.7)] + faces(
        'Turret', 'Turret', c, (1.1, h, 1.2), {'front': (front, 0.9), 'rear': (75, 0.9), 'left': (75, 0.9), 'right': (75, 0.9), 'roof': (roof, 1.0)})


def kv_hull_modules():
    return [
        crew('Driver', 'DRIVER', (0.45, 1.05, 3.2)),
        crew('Radio', 'RADIO', (-0.5, 1.05, 3.2)),
        box('Transmission', 'TRANSMISSION', (0, 0.95, -3.35), (0.85, 0.35, 0.35)),
        box('Engine', 'ENGINE', (0, 1.1, -2.3), (0.65, 0.48, 0.6)),
        box('FuelLeft', 'FUEL_TANK', (0.95, 1.2, -1.3), (0.28, 0.42, 0.45)),
        box('FuelRight', 'FUEL_TANK', (-0.95, 1.2, -1.3), (0.28, 0.42, 0.45)),
        box('AmmoFloorFront', 'AMMO_RACK', (0, 0.72, 1.4), (0.8, 0.14, 0.6)),
        box('AmmoFloorRear', 'AMMO_RACK', (0, 0.72, 0.2), (0.8, 0.14, 0.5)),
    ] + tracks((1.4, 0.55, -0.2), (0.42, 0.5, 3.9), (-1.4, 0.55, -0.2), (0.42, 0.5, 3.9))


write('kv-1', ARMOUR_NOTE, 1.0, kv_hull + kv_turret(0.5, 75, 30),
      MODEL_NOTE + " KV-1: 5 crew (driver and radio operator in the hull, commander, gunner and loader in the turret); engine and transmission at the rear; ammunition in floor bins and on the turret rear wall.",
      2,
      kv_hull_modules() + [
          crew('Gunner', 'GUNNER', (0.5, 0.15, 1.15), 'Turret', seat=0),
          crew('Commander', 'COMMANDER', (0.5, 0.15, 0.3), 'Turret', seat=1),
          crew('Loader', 'LOADER', (-0.55, 0.15, 0.7), 'Turret'),
          box('Breech', 'BREECH', (0, 0.25, 1.35), (0.22, 0.22, 0.5), 'Turret'),
          box('AmmoTurret', 'AMMO_RACK', (0, 0.3, -0.05), (0.7, 0.2, 0.12), 'Turret'),
          box('TurretDrive', 'TURRET_DRIVE', (0.85, -0.15, 1.6), (0.15, 0.15, 0.2), 'Turret'),
          box('Barrel', 'BARREL', (0, 0.2, 3.65), (0.1, 0.1, 1.3), 'Turret'),
      ])

# KV-2: same hull, tall MT-1 turret (turret frame centre (0,0.2,1.0) half (1.1,1.0,1.2)), 6 crew.
write('kv-2', ARMOUR_NOTE, 1.0, kv_hull + kv_turret(1.0, 75, 40),
      MODEL_NOTE + " KV-2: 6 crew (two loaders for the 152 mm M-10T); most ammunition in hull floor bins.",
      2,
      kv_hull_modules() + [
          crew('Gunner', 'GUNNER', (0.55, -0.15, 1.2), 'Turret', seat=0),
          crew('Commander', 'COMMANDER', (0.55, 0.05, 0.3), 'Turret', seat=1),
          crew('Loader', 'LOADER', (-0.55, -0.15, 1.0), 'Turret'),
          crew('Loader2', 'LOADER', (-0.55, -0.15, 0.1), 'Turret'),
          box('Breech', 'BREECH', (0, 0.15, 1.2), (0.32, 0.32, 0.65), 'Turret'),
          box('AmmoTurret', 'AMMO_RACK', (0, 0.3, -0.05), (0.8, 0.3, 0.12), 'Turret'),
          box('TurretDrive', 'TURRET_DRIVE', (0.85, -0.65, 1.6), (0.15, 0.15, 0.2), 'Turret'),
          box('Barrel', 'BARREL', (0, 0.15, 3.25), (0.14, 0.14, 0.9), 'Turret'),
      ])

# ------------------------------------------------------------------------------------------------
# M4A3(75)W Sherman. Hull box centre (-0.043,1.236,-0.237) half (1.38,0.915,3.025).
# Upper glacis 63.5 mm @ 47 deg, lower nose 50.8 mm @ ~30 down, sides 38, rear 38 @ 10, roof 19,
# floor 13. Turret (turret frame) box centre (0,-0.118,-0.115) half (1.01,0.525,1.075):
# front 76 @ 30 (curved), mantlet 89, sides 51 @ 5, rear 51, roof 25.
sh_c, sh_h = (-0.043, 1.236, -0.237), (1.38, 0.915, 3.025)
sh_plates = faces('', 'Vehicle', sh_c, sh_h, {
    'front_upper': (63.5, 1.0, tilt(47)), 'front_lower': (50.8, 0.8, tilt(-30)),
    'rear': (38, 0.9, tilt(10, sign=-1)), 'left': (38, 0.9), 'right': (38, 0.9),
    'roof': (19, 1.0), 'floor': (13, 1.0)}, y_split=1.2)
tc, th = (0, -0.118, -0.115), (1.01, 0.525, 1.075)
sh_plates += [plate('Mantlet', 'Turret', 89, (0.1, -0.068, 0.96), (0.35, 0.28, T_HALF), 0.6)] + faces(
    'Turret', 'Turret', tc, th, {'front': (76, 0.8, tilt(30)), 'rear': (51, 0.9), 'left': (51, 0.9, tilt(5, 'x', 1)),
                                 'right': (51, 0.9, tilt(5, 'x', -1)), 'roof': (25, 1.0)})
write('m4a3-sherman', ARMOUR_NOTE, 1.0, sh_plates,
      MODEL_NOTE + " M4A3(75)W: 5 crew; transmission in the nose under the driver and assistant; Ford GAA engine and fuel at the rear; 'wet' water-jacketed ammunition bins in the hull floor (lower detonation chance).",
      2, [
          crew('Driver', 'DRIVER', (0.55, 1.4, 2.15)),
          crew('Radio', 'RADIO', (-0.62, 1.4, 2.15)),
          box('Transmission', 'TRANSMISSION', (-0.043, 0.72, 2.4), (0.75, 0.3, 0.3)),
          box('Engine', 'ENGINE', (-0.043, 1.2, -2.5), (0.75, 0.6, 0.62)),
          box('FuelLeft', 'FUEL_TANK', (0.95, 1.25, -1.65), (0.25, 0.55, 0.45)),
          box('FuelRight', 'FUEL_TANK', (-1.05, 1.25, -1.65), (0.25, 0.55, 0.45)),
          box('AmmoWetBins', 'AMMO_RACK', (-0.043, 0.55, 0.0), (0.9, 0.2, 0.85), DetonationChance=0.2),
          box('AmmoReady', 'AMMO_RACK', (-0.85, 1.6, 0.6), (0.2, 0.2, 0.3), DetonationChance=0.5),
          crew('Gunner', 'GUNNER', (0.45, -0.2, 0.3), 'Turret', seat=0),
          crew('Commander', 'COMMANDER', (0.45, -0.1, -0.5), 'Turret', seat=1),
          crew('Loader', 'LOADER', (-0.5, -0.2, 0.0), 'Turret'),
          box('Breech', 'BREECH', (0.15, -0.1, 0.4), (0.2, 0.2, 0.45), 'Turret'),
          box('TurretDrive', 'TURRET_DRIVE', (-0.6, -0.55, 0.5), (0.15, 0.1, 0.2), 'Turret'),
          box('Barrel', 'BARREL', (0.15, -0.07, 2.21), (0.08, 0.08, 1.1), 'Turret'),
      ] + tracks((1.071, 0.6, -0.094), (0.227, 0.5, 2.933), (-1.164, 0.6, -0.094), (0.227, 0.5, 2.933)))

# ------------------------------------------------------------------------------------------------
# T-26 (single-turret, 45 mm 20-K). 15 mm all round, roof 10, floor 6. Hull box centre
# (0,0.966,-0.296) half (1.44,0.79,2.665); turret (turret frame) centre (0,0.45,0.3) half (0.567,0.436,0.85).
t26 = faces('', 'Vehicle', (0, 0.966, -0.296), (1.44, 0.79, 2.665), {
    'front_upper': (15, 1.0, tilt(25)), 'front_lower': (15, 1.0, tilt(-25)), 'rear': (15, 1.0),
    'left': (15, 1.0), 'right': (15, 1.0), 'roof': (10, 1.0), 'floor': (6, 1.0)})
t26 += faces('Turret', 'Turret', (0, 0.45, 0.3), (0.567, 0.436, 0.85), {
    'front': (15, 1.0), 'rear': (15, 1.0), 'left': (15, 1.0), 'right': (15, 1.0), 'roof': (10, 1.0)})
write('t-26-1931', ARMOUR_NOTE, 0.6, t26,
      MODEL_NOTE + " T-26: 3 crew (driver in the hull front, commander-gunner and loader in the turret); engine rear, transmission front; ammunition racked on the hull sides.",
      2, [
          crew('Driver', 'DRIVER', (-0.45, 0.95, 1.6)),
          box('Transmission', 'TRANSMISSION', (0.35, 0.65, 1.95), (0.55, 0.3, 0.3)),
          box('Engine', 'ENGINE', (0, 0.95, -2.1), (0.7, 0.45, 0.62)),
          box('Fuel', 'FUEL_TANK', (0.9, 1.0, -1.15), (0.3, 0.4, 0.4)),
          box('AmmoLeft', 'AMMO_RACK', (0.85, 0.75, 0.35), (0.25, 0.3, 0.55)),
          box('AmmoRight', 'AMMO_RACK', (-0.85, 0.75, 0.35), (0.25, 0.3, 0.55)),
          crew('Gunner', 'GUNNER', (0.27, 0.35, 0.35), 'Turret', seat=0),
          crew('Loader', 'LOADER', (-0.28, 0.35, 0.35), 'Turret'),
          box('Breech', 'BREECH', (0, 0.45, 0.55), (0.12, 0.12, 0.35), 'Turret'),
          box('TurretDrive', 'TURRET_DRIVE', (0.4, 0.1, 0.7), (0.1, 0.08, 0.12), 'Turret'),
          box('Barrel', 'BARREL', (0, 0.45, 2.0), (0.06, 0.06, 0.7), 'Turret'),
      ] + tracks((1.205, 0.65, -0.186), (0.157, 0.6, 2.73), (-1.205, 0.65, -0.186), (0.157, 0.6, 2.73), armour=15))

# ------------------------------------------------------------------------------------------------
# Tiger H1. Hull box (corrected): x+-1.8, y0.71-2.1, z -3.89..2.2. Driver's plate 100 @ 10,
# nose 100 @ 24 down, upper sides 80, lower sides (behind the wheels) 60, rear 80 @ 9, roof 25,
# floor 25. Turret (turret frame) centre (0,-0.565,-0.485) half (1.443,0.431,1.058): front 100,
# mantlet 120, sides/rear 80, roof 25.
tg_c, tg_h = (0, 1.405, -0.845), (1.8, 0.695, 3.045)
tiger = faces('', 'Vehicle', tg_c, tg_h, {
    'front_upper': (100, 1.0, tilt(10)), 'front_lower': (100, 0.8, tilt(-24)),
    'rear': (80, 0.9, tilt(9, sign=-1)), 'left_upper': (80, 0.9), 'right_upper': (80, 0.9),
    'left_lower': (60, 0.9), 'right_lower': (60, 0.9), 'roof': (25, 1.0), 'floor': (25, 1.0)}, y_split=1.45)
tiger = [plate('Mantlet', 'Turret', 120, (0, -0.55, 0.573), (0.5, 0.33, T_HALF), 0.6)] + tiger + faces(
    'Turret', 'Turret', (0, -0.565, -0.485), (1.443, 0.431, 1.058),
    {'front': (100, 0.9), 'rear': (80, 0.9), 'left': (80, 0.9), 'right': (80, 0.9), 'roof': (25, 1.0)})
write('tiger-h1', ARMOUR_NOTE, 1.0, tiger,
      MODEL_NOTE + " Tiger H1: 5 crew; driver and radio operator either side of the gearbox in the front hull; Maybach engine and fuel tanks at the rear; ammunition in the sponsons over the tracks and in floor bins.",
      2, [
          crew('Driver', 'DRIVER', (0.6, 1.45, 1.55)),
          crew('Radio', 'RADIO', (-0.6, 1.45, 1.55)),
          box('Transmission', 'TRANSMISSION', (0, 1.0, 1.75), (0.6, 0.28, 0.35)),
          box('Engine', 'ENGINE', (0, 1.35, -3.15), (0.7, 0.5, 0.6)),
          box('FuelLeft', 'FUEL_TANK', (1.05, 1.35, -3.1), (0.3, 0.45, 0.5)),
          box('FuelRight', 'FUEL_TANK', (-1.05, 1.35, -3.1), (0.3, 0.45, 0.5)),
          box('AmmoSponsonLeft', 'AMMO_RACK', (1.4, 1.75, -0.7), (0.3, 0.28, 1.05)),
          box('AmmoSponsonRight', 'AMMO_RACK', (-1.4, 1.75, -0.7), (0.3, 0.28, 1.05)),
          box('AmmoFloor', 'AMMO_RACK', (0, 0.85, -0.7), (0.75, 0.12, 0.7)),
          crew('Gunner', 'GUNNER', (0.5, -0.6, 0.0), 'Turret', seat=0),
          crew('Commander', 'COMMANDER', (0.55, -0.5, -0.85), 'Turret', seat=1),
          crew('Loader', 'LOADER', (-0.6, -0.6, -0.3), 'Turret'),
          box('Breech', 'BREECH', (0, -0.5, 0.05), (0.25, 0.22, 0.5), 'Turret'),
          box('TurretDrive', 'TURRET_DRIVE', (0.85, -0.9, 0.25), (0.15, 0.1, 0.2), 'Turret'),
          box('Barrel', 'BARREL', (0, -0.55, 2.45), (0.1, 0.1, 1.7), 'Turret'),
      ] + tracks((1.525, 0.6, -0.748), (0.288, 0.55, 3.209), (-1.529, 0.6, -0.748), (0.291, 0.55, 3.209), armour=25),
      also=('tiger-h1-grey', 'tiger-h1-sand'))
# ------------------------------------------------------------------------------------------------
# 8.8 cm Flak 36. Cruciform base (two boxes) and the top carriage (turret frame). Structural steel
# only - the crew stand on the platform, outside any armour, so they are marked External and are
# hurt by HE bursting nearby. Knocked out when the whole crew is down (MinCrew 1).
flak = faces('Base', 'Vehicle', (0, 0.33, 0), (0.45, 0.33, 2.7), {
    'front': (10, 0.8), 'rear': (10, 0.8), 'left': (10, 0.8), 'right': (10, 0.8), 'roof': (10, 1.0)})
flak += faces('Outrigger', 'Vehicle', (0, 0.33, 0), (2.3, 0.33, 0.45), {
    'front': (10, 0.8), 'rear': (10, 0.8), 'left': (10, 0.8), 'right': (10, 0.8), 'roof': (10, 1.0)})
flak += faces('Carriage', 'Turret', (0, 0.73, -0.17), (0.75, 0.72, 0.9), {
    'front': (8, 0.8), 'rear': (8, 0.8), 'left': (8, 0.8), 'right': (8, 0.8), 'roof': (6, 1.0)})
write('flak-88', ARMOUR_NOTE, 0.8, flak,
      MODEL_NOTE + " 8.8 cm Flak 36: gunner, layer and loader work in the open on the platform; ready rounds are stacked beside the gun.",
      1, [
          box('Gunner', 'CREW', (0.95, 0.55, -0.2), (0.25, 0.85, 0.25), 'Turret', Role='GUNNER', Seat=0, External=True),
          box('Layer', 'CREW', (-0.95, 0.55, -0.1), (0.25, 0.85, 0.25), 'Turret', Role='COMMANDER', External=True),
          box('Loader', 'CREW', (0.0, 0.55, -2.0), (0.3, 0.85, 0.3), 'Turret', Role='LOADER', External=True),
          box('Breech', 'BREECH', (0, 1.23, -1.2), (0.16, 0.16, 0.5), 'Turret'),
          box('Barrel', 'BARREL', (0, 1.23, 2.4), (0.09, 0.09, 1.35), 'Turret'),
          box('TraverseGear', 'TURRET_DRIVE', (0.35, 0.3, 0.3), (0.15, 0.15, 0.15), 'Turret'),
          box('AmmoReady', 'AMMO_RACK', (1.25, 0.95, -1.6), (0.3, 0.25, 0.35), External=True, DetonationChance=0.25),
      ])
print('ok')
