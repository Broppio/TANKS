# WarSteel damage model

WarSteel tanks don't use Superb Warfare's hit points. Like War Thunder, a tank is a set of
**armour plates** and **internal modules** (crew, engine, ammo racks...). A tank is knocked out
when its ammunition detonates or when too few crew are left to fight it.

## What happens when a cannon shell hits a tank

1. **External modules.** If the impact point is on a track or the barrel, that module takes the
   hit first and acts as spaced armour: its `Armour` (mm) is subtracted from the shell's
   penetration. HE and HESH rounds detonate there.
2. **Armour plate.** The plate that was struck is found and the angle of incidence is measured
   from its normal. Effective thickness = thickness / cos(angle).
3. **Outcome.**
   - **Ricochet**: chance rises with angle (shell `MinAngle` → `AlwaysAngle`), unless the
     shell overmatches the plate (penetration ≥ `OvermatchRatio` × effective thickness).
   - **Penetration** if penetration at that range ≥ effective thickness. Penetration falls
     with distance: `Muzzle − DropoffPerBlock × blocks`, never below `Minimum`.
   - **No penetration** otherwise. HE that fails to penetrate still bursts on the hull and can
     damage tracks and the barrel.
4. **Inside the tank** (after a penetration), depending on the shell `Class`:
   - `AP`: the shell body keeps going through the hull, wrecking everything in its path, and
     throws a 24° cone of spall.
   - `APCR` / `APDS`: same, with a narrower, weaker spall cone.
   - `APHE` / `APCBC` / `APHEBC`: spall cone, then the shell travels `FuzeDelay` blocks and
     bursts (blast plus a sphere of fragments, scaled by `Filler`). If the plate was thinner
     than `FuzeSensitivity` mm the fuze doesn't arm and the shell over-penetrates.
   - `HE`: bursts just behind the plate (only possible when it defeats the armour).
   - `HESH`: wide spall cone off the inside of the plate.

   Fragments stop at the first crew member or after two modules, and never pass the far armour.

## Module effects

| Module | When destroyed |
|---|---|
| `CREW` with `Role` `DRIVER` | Immobilised until another crew member takes over |
| `CREW` with `Role` `GUNNER` | Can't fire the main gun or traverse the turret until replaced |
| `CREW` (any role) with `Seat` | The player in that seat can't fire and takes a share of the hit |
| `ENGINE`, `TRANSMISSION` | Immobilised |
| `TRACK` (`Side` `LEFT`/`RIGHT`) | Drags to one side (Superb Warfare's wheel damage) |
| `BREECH`, `BARREL` | Main gun out of action |
| `TURRET_DRIVE` | Turret can't traverse |
| `AMMO_RACK` | One roll per hit to detonate (`DetonationChance`, scaled by damage); detonation knocks the tank out |
| `FUEL_TANK`, `ENGINE` | `FireChance` per hit to start a fire |

**Fire** burns for `FireSeconds`. It hurts crew every second and can cook off ammunition
within 2.5 blocks of it.

**Crew swaps.** If the driver or gunner is knocked out, a loader, radio operator or commander
takes the position after `CrewSwapSeconds`.

**Repair.** After `RepairSeconds` without new damage (and no fire), all damaged components
are repaired. Lost crew are replaced after `CrewReplaceSeconds`.

**Knocked out.** An ammo detonation, or fewer than `MinCrew` crew alive, knocks the tank
out. Superb Warfare then wrecks it as usual.

**Other weapons.** Damage that isn't a cannon shell (bullets, rams, other mods' weapons)
reaches the hull at `FallbackDamageMultiplier` of normal, so nothing is immortal. Cannon shells
from Superb Warfare's own tanks get a generic profile and go through the full model.

## Ammunition

Each tank fires its own historical rounds. They're craftable items: load them into the tank's
inventory like Superb Warfare shells and switch rounds with the weapon-change key. Hover over a
round to see its stats.

| Tank | Gun | Rounds |
|---|---|---|
| T-26 | 45 mm 20-K | BR-240 (APHE, 62 mm), O-240 (HE) |
| M4A3 (75) Sherman | 75 mm M3 | M61 (APCBC-HE, 104 mm), M72 (AP shot, 91 mm), M48 (HE) |
| KV-1 | 76 mm F-34 | BR-350A (APHEBC, 84 mm), OF-350 (HE) |
| KV-2 | 152 mm M-10T | BR-540 (APHE, 110 mm), OF-530 (HE, 6.9 kg TNT) |
| Tiger H1 | 8.8 cm KwK 36 | PzGr 39 (APCBC-HE, 162 mm), PzGr 40 (APCR, 171 mm), Sprgr. L/4.5 (HE) |

Penetration figures are War Thunder's at 10 m. Muzzle velocities are historical (the gun sights'
range ladders are matched to them).

Recipes use one shape per round: the top row is the projectile, the middle row the case and
propellant, and the bottom row the base with a `primer`.

| Projectile (top row) | |
|---|---|
| AP | steel ingot |
| APHE | steel, high-energy explosive, steel |
| APCR | steel, tungsten rod, steel |
| HE | iron, high-energy explosive, iron |

| Calibre | Case (middle, bottom) | Yield |
|---|---|---|
| 45 mm | copper ingots and gunpowder / primer | 8 |
| 75 mm | copper plates and gunpowder / primer | 4 |
| 76 mm | as 75 mm, plus copper plates either side of the primer | 4 |
| 88 mm | as 75 mm, plus steel either side of the primer | 3 |
| 152 mm | copper plates and TNT / copper, primer, copper | 1 |

All ammunition data is generated from one table in `tools/gen_ammo.py` (items, shell profiles,
the tanks' ammo lists, recipes, textures, names and sights). Edit the table and rerun it.

## High-explosive shells

WarSteel's HE (and APHE) shells don't use Superb Warfare's fixed explosion. Their effect is
worked out from the TNT-equivalent charge:

- **Blast** falls with scaled distance Z = R / W^(1/3) (W in kg). It's lethal to a person within
  Z ≈ 2 and harmless beyond Z ≈ 6. A 76 mm OF-350 (0.71 kg) kills within about 1.8 m; a 152 mm
  OF-530 (6.9 kg) within about 3.8 m.
- **Fragments**: about 900·√W fragments fly out evenly. Each person in line of sight is hit by
  their share (by area and distance), so HE wounds at random out to tens of metres while being
  reliably lethal only close in. Walls and terrain block fragments.
- **Craters** are small and sized to the charge, only when Superb Warfare's explosion block
  damage setting is on.
- **Tanks caught in the blast** take module damage (tracks, barrel). HE that penetrates bursts
  inside; only a small, muffled share of its blast escapes.

## Shot replay

When your shell hits a WarSteel tank, a small x-ray replay plays in the bottom-right corner, like
War Thunder's hit camera:
- the shell flies in, and the struck plate is shown in red;
- the core, spall and fragments spread through the hull;
- modules turn orange (damaged) or black (destroyed) as they're reached;
- the outcome and the list of what broke appear below.

Ricochets and non-penetrations show the shell glancing off.

## 8.8 cm Flak 36

A dual-purpose emplaced gun, built from a Flak 36 model (reduced from about 46,500 to about 7,000
triangles; see `tools/flak88_export.py` and `tools/flak88_atlas.py`). Place it from the creative
tab or build it on Superb Warfare's vehicle assembling table ("defense"). It traverses 360° and
elevates from −3° to +85°.

| Role | Round |
|---|---|
| Anti-tank | PzGr 39 (APCBC-HE, 162 mm), PzGr 40 (APCR, 171 mm) |
| Field artillery | Sprgr. L/4.5 (HE) |
| Anti-aircraft | Sprgr. Patr. L/4.5 Kz (`FLAK`, 870 g TNT, time fuze) |

The anti-aircraft round bursts within `ProximityRange` (6 blocks) of an aircraft, standing in for
the predictor setting the fuze to the target's range, or when its longest fuze setting
(`FuzeTicks`, about 2.3 km) runs out. Aircraft caught in a burst take fragment damage scaled by
their size and distance: one burst 3 m from an AH-6 takes it to a sliver of health.

### ARC (artillery) mode

Press **B** while crewing the gun to switch between direct fire and ARC mode. In ARC mode:
- shells leave with a reduced propellant charge (Charge 1-4 or Full; change it with **[** and **]**),
  so they can be lobbed high onto targets out of sight;
- the predicted trajectory is drawn in the world with a ring at the impact point, and the HUD shows
  elevation, range and time of flight (marked ≈ when the impact is beyond the loaded world);
- after each shot the camera follows the shell, holds on the impact for three seconds, then returns
  to the gun. Press **B** to come back early. The gun stays laid while you watch.

Terrain beyond your render distance isn't loaded, so shells landing that far away fly over empty
space on camera.

The gun uses the same armour and module model as the tanks. It has no real armour, and its crew
(gunner, layer, loader) stand in the open, so HE landing nearby wounds or kills them like infantry.
It's knocked out when the whole crew is down.

## Hit marks and wrecks

Every hit leaves a mark on the armour where it struck: a hole for a penetration, a bright gouge
for a non-penetration, a scrape for a ricochet, and a soot splash for HE. A solid shot (AP, APCR,
or APHE whose fuze didn't arm) that still has enough penetration left to beat the far plate goes
out the other side and leaves a torn exit hole there too. On models built from cubes the marks
are laid onto the model's own surface. Marks are sized by
calibre, stretched by the angle of impact, and turn with the turret. They stay (up to 40 per
tank) until the tank is repaired with Superb Warfare's repair tool, where each pass patches the
oldest mark and repairs the worst-damaged component, or until `/warsteel reset`.

Knocked-out WarSteel vehicles keep their paint rather than turning black. Their destruction blast
doesn't destroy blocks.

## Data files

All data lives in `data/<namespace>/warsteel/`, so a data pack can override any of it.

- `shells/*.json`: one per shell. `UsedBy` lists the vehicles that fire it; the shell
  entity's AP/HE type picks between that vehicle's profiles, and `Damage` breaks ties.
  The bundled values are War Thunder's: penetration at 10 m, filler in grams of TNT.
- `armor/<vehicle>.json`: plates. `Position` / `Size` (half-extents, blocks) in the
  `Transform` frame (`Vehicle` or `Turret`), `Thickness` in mm, optional `Normal` for sloped
  plates. Plates should sit on the faces of the vehicle's hitbox (`OBB` in the vehicle file),
  because that's where Superb Warfare reports the impact point.
- `modules/<vehicle>.json`: the damage model (fields as above; `Type`, `Role`, `Side`,
  `Seat`, `Health`, `Armour`, `External`, `FireChance`, `DetonationChance`).

Frames: `Vehicle` has x pointing left, y up and z forward, origin on the ground. `Turret` has its
origin on the turret pivot and turns with the turret. One block is one metre.

## Commands (operators)

- `/warsteel xray [seconds] [hideTanks]`: outlines plates (orange), Superb Warfare hitboxes
  (grey) and modules (blue crew, red ammo, yellow engine, purple fuel, white gun, cyan
  drive/transmission, green tracks; black = destroyed).
- `/warsteel status`: module report for the nearest tank.
- `/warsteel damage <module> <amount>`: damage one module.
- `/warsteel reset`: restore the nearest tank.
- `/warsteel fire <shell>`: fire a real cannon shell with a given profile (e.g.
  `warsteel:88mm_pzgr39`) from where you're looking (also works from command blocks with `execute positioned ... facing ...`).
- `/warsteel obbs`: print the nearest tank's hitboxes in its own frames, for writing plate data.
