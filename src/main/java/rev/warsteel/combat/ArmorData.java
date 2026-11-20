package rev.rev.warsteel.combat;

import java.util.List;
import kotlin.jvm.internal.Intrinsics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public final class ArmorData {
   @NotNull
   private final ResourceLocation vehicle;
   private final double generalRicochetChance;
   @NotNull
   private final List<ArmorPlate> plates;

   public ArmorData(@NotNull ResourceLocation vehicle, double generalRicochetChance, @NotNull List<ArmorPlate> plates) {
      super();
      this.vehicle = vehicle;
      this.generalRicochetChance = generalRicochetChance;
      this.plates = plates;
   }

   @NotNull
   public final ResourceLocation getVehicle() {
      return this.vehicle;
   }

   public final double getGeneralRicochetChance() {
      return this.generalRicochetChance;
   }

   @NotNull
   public final List<ArmorPlate> getPlates() {
      return this.plates;
   }
}
