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
- `/warsteel fire <shell>`: fire a real cannon shell with a given profile from where you're
  looking (also works from command blocks with `execute positioned ... facing ...`).
- `/warsteel obbs`: print the nearest tank's hitboxes in its own frames, for writing plate data.
