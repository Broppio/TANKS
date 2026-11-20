package rev.rev.warsteel.entity.vehicle;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import rev.rev.warsteel.damage.WarSteelTank;

/**
 * 8.8 cm Flak 36: a dual-purpose emplaced gun. It fires PzGr 39 / PzGr 40 against armour,
 * Sprgr. HE as field artillery, and time-fuzed Sprgr. Kz against aircraft. It uses the same
 * armour and module damage model as the tanks (its crew stand exposed on the platform).
 */
public class Flak88Entity extends WarSteelTank {
   public Flak88Entity(EntityType<?> type, Level level) {
      super(type, level);
   }
}
