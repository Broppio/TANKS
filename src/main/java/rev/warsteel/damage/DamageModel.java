package rev.rev.warsteel.damage;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Per-vehicle damage model: the modules plus the rules that decide when the tank is knocked out. */
public record DamageModel(
   ResourceLocation vehicle,
   List<ModuleDef> modules,
   int minCrew,
   int mainGunSeat,
   float repairSeconds,
   float crewReplaceSeconds,
   float crewSwapSeconds,
   float fireSeconds,
   float fallbackDamageMultiplier,
   float ammoExplosionRadius
) {
   public int indexOf(String name) {
      for (ModuleDef m : this.modules) {
         if (m.name().equals(name)) {
            return m.index();
         }
      }

      return -1;
   }
}
