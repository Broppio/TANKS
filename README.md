# WarSteel (NeoForge 1.21.1)

WarSteel is a World War 2 tank add-on for Superb Warfare by Lilrevo, ported to NeoForge 1.21.1.
It replaces hit points with a War Thunder style damage model: armour penetration, spall, crew and
modules. It also adds each tank's own historical, craftable ammunition, realistic HE explosions and
a hit-camera shot replay, an 8.8 cm Flak 36 dual-purpose gun, and hit marks on armour. See [DAMAGE_MODEL.md](DAMAGE_MODEL.md).

## Download

Get the jar from the Releases page, or from `release/` in this repository.

Requires NeoForge 21.1.203+, Superb Warfare 0.8.9.2 (NeoForge 1.21.1) and Kotlin for Forge 5.x.

## Building

Place these in `libs/` (they're compile-only and aren't committed):

- `superbwarfare-0.8.9.2-hotfix-mc1.21.1-e0c2b27f.jar`
- `nationsatwar-0.4.0.jar` (for the optional factory integration)
- `simplebedrockmodel-*.jar`, `geckolib-*.jar` and `mae-*.jar`, taken from the Superb Warfare jar's
  `META-INF/jarjar/` folder (and SimpleBedrockModel's own `META-INF/jarjar/` for `mae`)

Then run `gradle build`.

## License

GPL-3.0, as the original mod.
