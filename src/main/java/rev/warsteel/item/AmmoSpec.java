package rev.rev.warsteel.item;

import java.util.List;

/** One historical round, as listed in tools/gen_ammo.py. */
public record AmmoSpec(
   String id, String name, String gun, int calibre, String shellClass, double penetration, double fillerGrams, double velocity, double massKg, int stackSize,
   List<String> vehicles
) {
}
