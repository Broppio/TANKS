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
| PaK 38 | 5 cm PaK 38 | PzGr 39 (APCBC-HE, 96 mm), PzGr 40 (APCR, 149 mm), Sprgr. 38 (HE) |

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
| 50 mm | copper ingots and gunpowder / copper, primer, copper | 6 |
| 75 mm | copper plates and gunpowder / primer | 4 |
| 76 mm | as 75 mm, plus copper plates either side of the primer | 4 |
| 88 mm | as 75 mm, plus steel either side of the primer | 3 |
| 152 mm | copper plates and TNT / copper, primer, copper | 1 |

All ammunition data is generated from one table in `tools/gen_ammo.py` (items, shell profiles,
the tanks' ammo lists, recipes, textures, names and sights). Edit the table and rerun it.

## High-explosive shells

WarSteel's HE (and APHE) shells don't use Superb Warfare's fixed explosion. Their effect is
worked out from the TNT-equivalent charge:

- **Blast** falls with scaled distance Z = R / W^(1/3) (W in kg). With the close-in fragments it's
  lethal to a person within Z ≈ 3.5 and harmless beyond Z ≈ 7.5. A 76 mm OF-350 (0.71 kg) kills
  within about 3-4 m and wounds to about 6 m; a 152 mm OF-530 (6.9 kg) kills within about 7 m.
- **Fragments**: about 900·√W fragments fly out evenly, reaching 30·∛W + 6 m. Each person in line of sight is hit by
  their share (by area and distance), so HE wounds at random out to tens of metres while being
  reliably lethal only close in. Walls and terrain block fragments.
- **Craters** are small and sized to the charge, only when Superb Warfare's explosion block
  damage setting is on.
- **Tanks caught in the blast** take module damage (tracks, barrel). HE that penetrates bursts
  inside; only a small, muffled share of its blast escapes.

## Panzerfaust 60

A disposable anti-tank launcher. It comes loaded; aim and fire like Superb Warfare's RPG. The
empty tube is thrown away a moment after firing, so carry spares (the HUD counts them).

- **Warhead:** 150 mm shaped charge (HEAT), **200 mm** of armour at any range. A shaped charge's
  penetration comes from its jet, not its speed, so it doesn't fall off with distance.
- **Flight:** 45 m/s with real gravity, so it drops about 9 m at 60 m: aim high at range, as with
  the real leaf sight (30/60/80 m).
- **Against armour:** the warhead bursts on the outside of the plate and its jet goes in: a short,
  very destructive line plus a narrow cone of molten spall, spent within a metre or two. Tracks
  and other spaced armour in front of the hull set it off early and cost it nearly half its
  penetration. Very steep hits can fail to fuze and glance off (from 72°).
- **Backblast:** anyone within 3 m behind the tube is burned.
- **Recipe:** copper plate, high-energy explosive, copper plate / steel ingot (middle) / gunpowder,
  steel ingot, primer.

The launcher uses Superb Warfare's RPG-7 first-person rig (arms, recoil, sway); its own mesh is drawn
by `PanzerfaustItemRenderer` (see `tools/panzerfaust_export.py`).

## Gun recoil, muzzle blast and sights

The Flak 88 and PaK 38 tubes run back through their cradles when they fire (about 0.9 m and 0.6 m)
with a muzzle flash. Every WarSteel gun throws a muzzle blast of smoke and dust.

The gun sight's reticle is drawn where the gun actually points (the bore line at the rangefinder
distance), not at the middle of the screen, so it stays true while the turret is still traversing
and allows for the sight sitting beside the gun. Its range marks use the loaded round's own
velocity.

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
tab, or get it from a factory (see "Building vehicles"). It traverses 360° and
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

- ARC shells have no time fuze (the Kz round bursts on impact too) and no lifetime limit, and they
  keep the chunks along their path loaded, so they fly as far as the charge and elevation send them;
- when a shell lands, the chat shows the splash: distance, bearing, height and time of flight.

Terrain beyond your render distance isn't sent to your game, so on long shots the camera follows
the shell over open sky; the splash report tells you where it came down.

The gun uses the same armour and module model as the tanks. It has no real armour, and its crew
(gunner, layer, loader) stand in the open, so HE landing nearby wounds or kills them like infantry.
It's knocked out when the whole crew is down.

## 5 cm PaK 38

A towed anti-tank gun, built from a 27,700-triangle model reduced to about 8,300 (see
`tools/pak38_export.py` and `tools/pak38_atlas.py`). Place it from the creative tab or get it from a
factory (see "Building vehicles"). The gun and shield traverse 32° either side of the trails and
elevate from −8° to +27°.

| Round | Type | Penetration at 10 m | Muzzle velocity |
|---|---|---|---|
| PzGr 39 | APCBC-HE | 96 mm | 835 m/s |
| PzGr 40 | APCR | 149 mm | 1,180 m/s |
| Sprgr. 38 | HE | — | 550 m/s |

The shield is two 4 mm plates; the gunner, loader and commander kneel behind it in the open.

## Second World War setting

With WarSteel installed, Superb Warfare is trimmed to fit the period:

- **Every Superb Warfare firearm is disabled**, with its perks, blueprints, gun parts and
  ammunition (WarSteel's Panzerfaust is unaffected). Use a WW2 gun pack for infantry weapons.
- **Superb Warfare's own vehicles are disabled**: they can't be assembled, deployed or spawned, and
  any already in a world disappear when their chunk loads. WarSteel's vehicles are unaffected.
- **Modern gear is disabled**: drones, missiles and rockets, C4, claymores and modern mines,
  tasers, modern body armour, the gun workbenches and other items that don't fit (196 in all).
  Period items stay: hand grenades, M18 smoke, lunge mines, the M35 helmet, sandbags, barbed
  wire, dragon's teeth, the FuMO 25 radar, knives, shovels, medical kits, parachutes, the
  repair tool, containers and heavy machine-gun ammunition.

Disabled items lose their recipes, are hidden from the creative tabs, can't be used, and are
removed from players' inventories, the ground and mobs' hands.

Server owners can adjust it with a data pack: the item tag `warsteel:disabled` lists the disabled
items (add more, or override it with `"replace": true`), and the entity type tag
`warsteel:allowed_vehicles` lets chosen Superb Warfare vehicles back in. Start the game with
`-Dwarsteel.allowModern=true` to turn the whole filter off.

## Building vehicles (Nations at War factories)

WarSteel vehicles can't be crafted. They're produced in **Nations at War** factories, as crates:

| Crate | Vehicle | One every |
|---|---|---|
| `warsteel:pak38_crate` | 5 cm PaK 38 | 8 days |
| `warsteel:t26_crate` | T-26 | 10 days |
| `warsteel:sherman_crate` | M4A3 (75) Sherman | 21 days |
| `warsteel:flak88_crate` | 8.8 cm Flak 36 | 24 days |
| `warsteel:kv1_crate` | KV-1 | 26 days |
| `warsteel:kv2_crate` | KV-2 | 34 days |
| `warsteel:tiger_crate` | Tiger H1 | 42 days |
| `warsteel:t22_crate` | T-22 torpedo boat | 60 days |

1. Request a factory for the crate like any other item, e.g.
   `/nation factory request "Tank Works" warsteel:t26_crate 1` (the amount is crates per
   production period). An admin approves it as usual.
2. Each period (real days, from approval) the factory delivers its crates to the nation's
   stockpile. WarSteel runs these factories on the vehicle's own production time instead of Nations
   at War's monthly cycle; members get a message when the line starts and on every delivery, and
   deliveries are posted to Discord. Production only runs while the factory is active (its land
   claimed); time it spends stopped doesn't count.
3. Withdraw the crate from the stockpile and right-click it to unpack it into Superb Warfare's
   deployable vehicle container, then place that.

Nations at War's own "next payout" display still shows its monthly cycle for these factories;
the crate's tooltip and the factory's start message give the real time. To test, start the server
with `-Dwarsteel.factoryDayMillis=60000` (one production day per minute).

Without Nations at War installed, vehicles come only from the creative tab or commands.

## T-22 torpedo boat (Flottentorpedoboot 1939)

A Superb Warfare ship (`warsteel:t-22`), 97 m long, 3.3 m draught, about 33 knots flat out.
The model is cut down to about 16,000 triangles (from 46,000).

### Crew and weapons

| Seat | Post | Weapon |
|---|---|---|
| 0 | Captain (drives) | Main battery: four 10.5 cm SK C/32 (Sprgr. HE, Psgr. APHE) |
| 1-2 | Port / starboard 3.7 cm Flak | 37 mm Sprgr. |
| 3 | 2 cm Flakvierling (aft) | 20 mm Sprgr. |
| 4-5 | Port / starboard 2 cm Flak | 20 mm Sprgr. |
| 6 | Torpedo officer | Spread of three G7a torpedoes |
| 7-12 | Deck passengers | none |

The captain sits at mount A like a tank gunner: look around and the mount trains after you (about
1 degree per tick; it can't train through the bridge, so 150 degrees either side of the bow). The
camera sits over the gun shield; zoom for the gunsight at the muzzle. Every other mount that bears on the same bearing fires with it, so one
person can sail her and fight her main battery alone:

- Ahead, mounts A and B fire.
- Abeam, all four fire.
- Astern, A, C and D fire.

The AA mounts and the torpedo tubes train to follow their own gunners.

G7a torpedoes:

- They run at 44 knots, 1.5 m deep, and arm after 20 m.
- Each carries 340 kg of TNT equivalent.
- Under water they tear holes tens of square metres across.
- The tubes alternate between the two mounts, and need `warsteel:torpedo_g7a` as ammunition.

### Damage, flooding and sinking

The ship floats on displacement, not hit points. Her hull is split into eight watertight
compartments. Holes let water in at a rate that depends on how far below the surface they are.
A full compartment whose deck is under water spills over its bulkheads into its neighbours. The
weight of the water takes her down, trims her by the head or stern and lists her towards the
flooded side. Lost boilers or turbines and the extra weight slow her down. While any boiler
works, her pumps fight the flooding.

- **Any explosion** near the ship holes the hull and damages the modules near it. That covers
  Superb Warfare's, WarSteel's, TNT, creepers and other mods' explosions. Holes are bigger the
  closer and heavier the blast, and three times bigger under water.
- A shell that penetrates the hull plating leaves a hole the size of its calibre.
- Small arms hit the exposed AA and torpedo crews. The hull doesn't care about them.
- Two magazines and the torpedo reserves can detonate, which breaks her back.
- When "killed", or once her magazines go up, she doesn't vanish: her back breaks, and she floods
  and sinks. When her deck has been well under water for 10 seconds, the crew are thrown clear
  and she becomes a wreck on the seabed.

## Hit marks and wrecks

*Hit marks are switched off in this version (`HitDecals.ENABLED`).*

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

## Sound

All world sounds are mono, so Minecraft places them where they happen and fades them with
distance (stereo sounds play at full volume everywhere). Each gun has three reports: the sharp
close one, a muffled far one with a ground echo, and a low rolling rumble for listeners furthest
away (`tools/localize_sounds.sh` builds them). Only the gunner's own firing sound is stereo.
HE bursts carry roughly 25-55 blocks sharp and up to ~220 blocks as a rumble, scaled by charge;
ricochets and armour strikes carry about 40-60 blocks.

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
- `/warsteel ship`: flooding report for the nearest ship (compartments, water, trim, list, holes).
- `/warsteel torpedo`: launch a G7a from where you're looking.
- `/warsteel obbs`: print the nearest tank's hitboxes in its own frames, for writing plate data.
