#!/usr/bin/env python3
"""Writes the T-22's Superb Warfare vehicle definition from the model's bone pivots.

Usage: python3 tools/t22_data.py <t22_pivots.json from tools/t22_export.py>
Then run tools/gen_ammo.py (fills in the main battery's AmmoType) and tools/gen_armour.py.

Vehicle frame: x to port, y up from the keel, z forward, origin amidships; 1 block = 1 m.
Seats: 0 captain (drives, fires the main battery: mount A aims, every mount that bears fires),
1/2 3.7 cm Flakzwilling port/starboard, 3 2 cm Flakvierling, 4/5 2 cm Flak port/starboard
(bridge wings), 6 torpedo officer, 7-12 deck passengers.
"""
import json, os, sys

piv = json.load(open(sys.argv[1]))
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def v(name, dz=0.0, dy=0.0):
    mx, my, mz = piv[name]
    return [round(-mx / 16, 3), round(my / 16 + dy, 3), round(-mz / 16 + dz, 3)]


turret = v('turret')
barrel = v('barrel')
muzzle_len = round(abs(piv['flare'][2] - piv['barrel'][2]) / 16, 3)

HE_ICON = 'superbwarfare:textures/overlay/vehicle/weapon/icons/he_shell.png'
BOFORS = {'Fire1P': 'superbwarfare:bofos_fire_1p', 'Fire3P': 'superbwarfare:bofos_fire_3p',
          'Fire3PFar': 'superbwarfare:bofos_fire_far', 'Fire3PVeryFar': 'superbwarfare:bofos_fire_veryfar'}
MG = {'Fire1P': 'superbwarfare:m_2_hb_fire_1p', 'Fire3P': 'superbwarfare:m_2_hb_fire_3p',
      'Fire3PFar': 'superbwarfare:m_2_hb_far', 'Fire3PVeryFar': 'superbwarfare:m_2_hb_veryfar'}


def aa_weapon(ammo, name, rpm, mag, reload, vel, dmg, exp_dmg, exp_r, sounds, positions, icon):
    return {'Icon': icon, 'DefaultFireMode': 'Auto', 'AvailableFireModes': 'Auto', 'Projectile': 'superbwarfare:small_cannon_shell',
            'RPM': rpm, 'Magazine': mag, 'EmptyReloadTime': reload, 'Velocity': vel, 'Damage': dmg, 'ExplosionDamage': exp_dmg,
            'ExplosionRadius': exp_r, 'Spread': 0.35, 'HeatPerShoot': 1, 'NaturalCooldown': 1, 'Name': 'item.warsteel.' + ammo,
            'AmmoType': 'warsteel:' + ammo, 'Crosshair': '@VehicleCommonGun', 'CrosshairColor': '0xFFFFFF', 'SoundInfo': sounds, 'SoundRadius': 24,
            'ShootPos': {'Transform': 'Vehicle', 'ShootPositionForHud': positions[0], 'Positions': positions, 'Directions': ['Passenger'],
                         'DefaultBarrelDirection': [0, 0, 1]}}


def gunner_seat(mount, gun, weapon, orientation, min_yaw, max_yaw, min_pitch=-5, max_pitch=85):
    p = v(mount, dy=0.2)
    c = v(gun, dy=0.6)
    return {'HidePassenger': True, 'Transform': 'Vehicle', 'Position': p, 'Orientation': orientation, 'RotateWithVehicle': True,
            'MinYaw': min_yaw, 'MaxYaw': max_yaw, 'MinPitch': -max_pitch, 'MaxPitch': -min_pitch, 'Sensitivity': [0.5, 0.7, 0.9],
            'CameraPos': {'Transform': 'Vehicle', 'UseFixedCameraPos': True, 'Position': c}, 'Weapons': [weapon],
            'DismountInfo': {'Transform': 'Vehicle', 'Position': [0, 7.2, round(p[2], 2)]}}


def deck_seat(x, z):
    return {'Transform': 'Vehicle', 'Position': [x, 7.0, z], 'RotateWithVehicle': True, 'MinYaw': -120, 'MaxYaw': 120,
            'DismountInfo': {'Transform': 'Vehicle', 'Position': [x, 7.2, z]}}


captain = {'HidePassenger': True, 'Transform': 'Vehicle', 'Position': [0, 10.6, 22.5], 'CanRotateHead': False,
           'Sensitivity': [0.35, 0.6, 0.9],
           'CameraPos': {'Transform': 'Barrel', 'ZoomPosition': [0.6, 0.55, 0.6], 'ZoomDirection': 'Barrel'},
           'MinPitch': -10, 'MaxPitch': 40, 'Weapons': ['Cannon'],
           'DismountInfo': {'Transform': 'Vehicle', 'Position': [0, 7.2, 18.0]}}

d = {
    'ID': 'warsteel:t-22',
    'MaxHealth': 3000,
    'MaxEnergy': 400000000,
    'Type': 'Boat',
    'EngineType': 'Ship',
    # 33 kn (0.85 blocks/tick) at full power; a turning circle of about 300 m. Floats at its 3.3 m draught
    # (measured in game: buoyancy 0.0215 puts the keel 3.3 m under). Flooding is added on top of this by WarSteel (ShipFlooding).
    'EngineInfo': {'Buoyancy': 0.0215, 'EnergyCostRate': 40, 'MaxForwardSpeedRate': 3.0, 'MaxBackwardSpeedRate': -0.9,
                   'Increment': 0.004, 'Decrement': 0.004, 'SteeringSpeed': 0.012, 'BodyRollRate': 0.3, 'BodyPitchRate': 0.05,
                   'EngineSoundVolume': 1.4},
    'TurretControllerIndex': 0,
    'TurretPos': turret,
    'BarrelPos': [round(barrel[0] - turret[0], 3), round(barrel[1] - turret[1], 3), round(barrel[2] - turret[2], 3)],
    'TurretTurnSpeed': [0.9, 1.4],
    'TurretPitchRange': [-10, 40],
    'Seats': [
        captain,
        gunner_seat('flakz_p', 'flakz_p_gun', 'Flak37Port', 90, -120, 120),
        gunner_seat('flakz_s', 'flakz_s_gun', 'Flak37Starboard', -90, -120, 120),
        gunner_seat('flakv', 'flakv_gun', 'Flakvierling', 180, -150, 150),
        gunner_seat('flak_3', 'flak_3_gun', 'Flak20Port', 90, -110, 110),
        gunner_seat('flak_2', 'flak_2_gun', 'Flak20Starboard', -90, -110, 110),
        {'HidePassenger': True, 'Transform': 'Vehicle', 'Position': v('torp_1', dy=1.0), 'RotateWithVehicle': True,
         'MinYaw': -180, 'MaxYaw': 180, 'MinPitch': -10, 'MaxPitch': 10, 'Sensitivity': [0.4, 0.6, 0.9],
         'CameraPos': {'Transform': 'Vehicle', 'UseFixedCameraPos': True, 'Position': [0, 12.5, 18.0]},
         'Weapons': ['Torpedoes'], 'DismountInfo': {'Transform': 'Vehicle', 'Position': [0, 7.2, 7.0]}},
        deck_seat(2.0, 12.0), deck_seat(-2.0, 12.0), deck_seat(2.0, -3.0), deck_seat(-2.0, -3.0), deck_seat(2.0, -27.0), deck_seat(-2.0, -27.0),
    ],
    'Weapons': {
        'Cannon': {
            'Icon': HE_ICON, 'DefaultFireMode': 'Semi', 'AvailableFireModes': 'Semi', 'Projectile': 'superbwarfare:cannon_shell',
            'DefaultZoom': 4, 'Magazine': 1, 'EmptyReloadTime': 80, 'Velocity': 39.25, 'Damage': 1500, 'ExplosionDamage': 90,
            'ExplosionRadius': 1.8, 'RecoilTime': 20, 'RecoilForce': 0.3, 'ShootShake': [4, 6, 40], 'Spread': 0.15,
            'Crosshair': '@VehicleDynamicCross', 'CrosshairColor': '00000000', 'CrosshairZooming': '@VehicleCommonCannonZooming',
            'Name': 'item.warsteel.105mm_sprgr',
            'SoundInfo': {'Fire1P': 'warsteel:tiger1_1p_fire', 'Fire3P': 'warsteel:tiger1_3p_fire', 'Fire3PFar': 'warsteel:tiger1_3p_far',
                          'Fire3PVeryFar': 'warsteel:tiger1_3p_veryfar', 'VehicleReload': 'warsteel:default_reload',
                          'Change': 'superbwarfare:into_cannon', 'CancellableSounds': ['warsteel:default_reload']},
            'SoundRadius': 64,
            'ShootPos': {'Transform': 'Barrel', 'Positions': [[0.0, 0.0, muzzle_len]], 'Directions': ['Barrel']},
            'ProjectileLife': 800,
        },
        'Flak37Port': aa_weapon('37mm_sprgr', '3.7 cm', 120, 8, 50, 50.0, 90, 14, 2.4, BOFORS,
                                [v('flakz_p_gun', dz=0.0, dy=0.0)], 'superbwarfare:textures/overlay/vehicle/weapon/icons/cannon_40mm_he.png'),
        'Flak37Starboard': aa_weapon('37mm_sprgr', '3.7 cm', 120, 8, 50, 50.0, 90, 14, 2.4, BOFORS,
                                     [v('flakz_s_gun')], 'superbwarfare:textures/overlay/vehicle/weapon/icons/cannon_40mm_he.png'),
        'Flakvierling': aa_weapon('20mm_sprgr', '2 cm', 800, 80, 70, 45.0, 35, 6, 1.6, MG,
                                  [v('flakv_gun')], 'superbwarfare:textures/overlay/vehicle/weapon/icons/cannon_25mm_he.png'),
        'Flak20Port': aa_weapon('20mm_sprgr', '2 cm', 450, 20, 50, 45.0, 35, 6, 1.6, MG,
                                [v('flak_3_gun')], 'superbwarfare:textures/overlay/vehicle/weapon/icons/cannon_25mm_he.png'),
        'Flak20Starboard': aa_weapon('20mm_sprgr', '2 cm', 450, 20, 50, 45.0, 35, 6, 1.6, MG,
                                     [v('flak_2_gun')], 'superbwarfare:textures/overlay/vehicle/weapon/icons/cannon_25mm_he.png'),
        # Fired as a marker shell; WarSteel (NavalWeapons) replaces it with a spread of three G7a torpedoes.
        'Torpedoes': {'Icon': 'superbwarfare:textures/overlay/vehicle/weapon/icons/ap_shell.png', 'DefaultFireMode': 'Semi',
                      'AvailableFireModes': 'Semi', 'Projectile': 'superbwarfare:cannon_shell', 'Magazine': 1, 'EmptyReloadTime': 400,
                      'Velocity': 1.0, 'Damage': 4242, 'ExplosionDamage': 0, 'ExplosionRadius': 0, 'Name': 'item.warsteel.torpedo_g7a',
                      'AmmoType': 'warsteel:torpedo_g7a', 'Crosshair': '@VehicleCommonGun', 'CrosshairColor': '0xFFFFFF',
                      'SoundInfo': {'Fire3P': 'superbwarfare:mortar_fire', 'Fire1P': 'superbwarfare:mortar_fire'}, 'SoundRadius': 24,
                      'ShootPos': {'Transform': 'Vehicle', 'ShootPositionForHud': v('torp_1', dy=1.0), 'Positions': [v('torp_1', dy=1.0)],
                                   'Directions': ['Passenger'], 'DefaultBarrelDirection': [1, 0, 0]}},
    },
    'OBB': [
        {'Size': [4.9, 3.35, 30.0], 'Position': [0, 3.35, 0]},
        {'Size': [3.1, 3.7, 9.3], 'Position': [0, 3.7, 39.3]},
        {'Size': [3.8, 3.2, 9.3], 'Position': [0, 3.2, -39.3]},
        {'Size': [4.0, 4.3, 6.0], 'Position': [0, 10.9, 22.7]},
        {'Size': [3.0, 2.8, 15.0], 'Position': [0, 9.5, 1.7]},
        {'Size': [3.0, 1.7, 4.0], 'Position': [0, 8.3, -19.3]},
        {'Part': 'Turret', 'Size': [1.25, 1.35, 1.3], 'Position': [0, 1.2, 0], 'Transform': 'Turret', 'Rotation': 'Turret'},
    ],
    'Mass': 400,
    'UpStep': 0.0,
    'RotateOffsetHeight': 3.3,
    # Knocked out ships don't explode in a fireball here: WarSteel floods them and they go down.
    'DestroyInfo': {'ExplosionDamage': 0, 'ExplosionRadius': 0, 'ExplodeBlocks': False, 'ExplodePassengers': False},
    'ThirdPersonCameraPos': [0, 14, 60],
    'VehicleIcon': 'warsteel:textures/vehicle_icon/t-22_icon.png',
    'ContainerIcon': 'superbwarfare:textures/gui/vehicle/type/water.png',
    'VehicleContainerType': 'Huge',
    'HudType': '@Land',
    'CollisionLevel': {'Level': 5, 'PowerLimits': [[0, 0, True], [0, 0, True], [0.1, 0.05, False], [1, 0.3, False], [3, 0.5, False]]},
    'EngineSound': 'warsteel:tiger_engine',
    'InertiaRotateRate': 0.4,
}
out = os.path.join(ROOT, 'src/main/resources/data/warsteel/sbw/vehicles/t-22.json')
json.dump(d, open(out, 'w'), indent=2)
json.dump({'Animation': 'warsteel:animations/bedrock/vehicle/t-22.animation.json',
           'Models': [{'Model': 'warsteel:models/bedrock/vehicle/t-22.geo.json', 'Texture': 'warsteel:textures/entity/t-22.png'}]},
          open(os.path.join(ROOT, 'src/main/resources/assets/warsteel/sbw/vehicles/t-22.json'), 'w'), indent=2)
print('wrote', out, 'TurretPos', turret, 'muzzle', muzzle_len)
