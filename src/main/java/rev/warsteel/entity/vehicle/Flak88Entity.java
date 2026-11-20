package rev.rev.warsteel.entity.vehicle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rev.rev.warsteel.damage.WarSteelTank;

/**
 * 8.8 cm Flak 36: a dual-purpose emplaced gun. It fires PzGr 39 / PzGr 40 against armour,
 * Sprgr. HE as field artillery, and time-fuzed Sprgr. Kz against aircraft. It uses the same
 * armour and module damage model as the tanks (its crew stand exposed on the platform).
 *
 * <p>ARC mode turns it into a howitzer: shells leave with a reduced propellant charge so they can
 * be lobbed onto targets out of sight, a predicted trajectory is drawn, and the camera follows
 * each shell to its impact.
 */
public class Flak88Entity extends WarSteelTank {
   public static final EntityDataAccessor<Boolean> ARC_MODE = SynchedEntityData.defineId(Flak88Entity.class, EntityDataSerializers.BOOLEAN);
   public static final EntityDataAccessor<Integer> ARC_CHARGE = SynchedEntityData.defineId(Flak88Entity.class, EntityDataSerializers.INT);
   /** Muzzle velocity per charge, as a share of the full charge (820 m/s for HE). */
   private static final float[] CHARGES = {0.12F, 0.18F, 0.26F, 0.40F, 1.0F};
   private static final int DEFAULT_CHARGE = 2;

   public Flak88Entity(EntityType<?> type, Level level) {
      super(type, level);
   }

   public static int chargeCount() {
      return CHARGES.length;
   }

   public static float chargeFactor(int charge) {
      return CHARGES[Mth.clamp(charge, 0, CHARGES.length - 1)];
   }

   public static String chargeName(int charge) {
      int c = Mth.clamp(charge, 0, CHARGES.length - 1);
      return c == CHARGES.length - 1 ? "Full charge" : "Charge " + (c + 1);
   }

   public boolean arcMode() {
      return this.getEntityData().get(ARC_MODE);
   }

   public void setArcMode(boolean on) {
      this.getEntityData().set(ARC_MODE, on);
   }

   public int charge() {
      return this.getEntityData().get(ARC_CHARGE);
   }

   public void setCharge(int charge) {
      this.getEntityData().set(ARC_CHARGE, Mth.clamp(charge, 0, CHARGES.length - 1));
   }

   @Override
   protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ARC_MODE, false);
      builder.define(ARC_CHARGE, DEFAULT_CHARGE);
   }

   @Override
   public void addAdditionalSaveData(@NotNull CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("ArcMode", this.arcMode());
      tag.putInt("ArcCharge", this.charge());
   }

   @Override
   protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("ArcMode")) {
         this.setArcMode(tag.getBoolean("ArcMode"));
         this.setCharge(tag.getInt("ArcCharge"));
      }
   }

   // The shell camera: Superb Warfare asks the vehicle where the seated player's camera goes.

   @Nullable
   @Override
   public Vec3 getCameraPosition(float partialTicks, @NotNull Player player, boolean zoom, boolean firstPerson) {
      if (this.level().isClientSide) {
         Vec3 cam = rev.rev.warsteel.client.artillery.ShellCamera.position(this, player, partialTicks);
         if (cam != null) {
            return cam;
         }
      }

      return super.getCameraPosition(partialTicks, player, zoom, firstPerson);
   }

   @Nullable
   @Override
   public Vec2 getCameraRotation(float partialTicks, @NotNull Player player, boolean zoom, boolean firstPerson) {
      if (this.level().isClientSide) {
         Vec2 rot = rev.rev.warsteel.client.artillery.ShellCamera.rotation(this, player, partialTicks);
         if (rot != null) {
            return rot;
         }
      }

      return super.getCameraRotation(partialTicks, player, zoom, firstPerson);
   }
}
