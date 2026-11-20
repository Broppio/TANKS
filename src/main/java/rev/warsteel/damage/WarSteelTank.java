package rev.rev.warsteel.damage;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for WarSteel tanks. Superb Warfare's hit-point model is replaced by the module
 * damage model in {@link TankDamage} whenever the vehicle has a damage model in the data pack;
 * without one it behaves exactly like a plain SBW vehicle.
 */
public abstract class WarSteelTank extends VehicleEntity {
   public static final EntityDataAccessor<String> MODULE_STATUS = SynchedEntityData.defineId(WarSteelTank.class, EntityDataSerializers.STRING);
   public static final EntityDataAccessor<String> DECALS = SynchedEntityData.defineId(WarSteelTank.class, EntityDataSerializers.STRING);
   private final TankState state = new TankState();

   protected WarSteelTank(EntityType<?> type, Level level) {
      super(type, level);
   }

   @Nullable
   public DamageModel damageModel() {
      return DamageModelRegistry.get(BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()));
   }

   /** Server: the authoritative state. Client: rebuilt from the synced status string. */
   public TankState damageState() {
      if (this.level().isClientSide) {
         this.state.decode(this.getEntityData().get(MODULE_STATUS));
      } else {
         this.state.bind(this.damageModel());
      }

      return this.state;
   }

   public boolean usesModuleDamage() {
      return !this.level().isClientSide ? this.damageModel() != null : !this.getEntityData().get(MODULE_STATUS).isEmpty();
   }

   @Override
   protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(MODULE_STATUS, "");
      builder.define(DECALS, "");
   }

   @Override
   public boolean hurt(@NotNull DamageSource source, float amount) {
      if (this.level().isClientSide || this.isWreck() || !this.usesModuleDamage() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return super.hurt(source, amount);
      }

      TankState st = this.damageState();
      if (st.knockedOut()) {
         return super.hurt(source, amount);
      }

      // Cannon shells were already resolved against armour and modules by the hit handler,
      // including the shell's own explosion.
      if (TankDamage.isHandled(source.getDirectEntity())) {
         return false;
      }

      // Everything else (small arms, rams, other mods' weapons) still wears the hull down,
      // just slowly, so a tank can't be immortal against weapons with no WarSteel profile.
      DamageModel model = this.damageModel();
      float mult = model != null ? model.fallbackDamageMultiplier() : 1.0F;
      return mult > 0.0F && super.hurt(source, amount * mult);
   }

   @Override
   public boolean canShoot(@Nullable LivingEntity living) {
      if (!super.canShoot(living)) {
         return false;
      } else if (living == null || !this.usesModuleDamage()) {
         return true;
      } else {
         boolean client = this.level().isClientSide;
         TankState st = this.damageState();
         if (st.knockedOut() || client && st.clientFlags().contains("K")) {
            return false;
         }

         int seat = this.getSeatIndex(living);
         DamageModel model = client ? null : st.model();
         int mainSeat = model != null ? model.mainGunSeat() : 0;
         if (seat == mainSeat && st.mainGunDisabled(client)) {
            return false;
         }

         return !st.seatCrewOut(seat, client);
      }
   }

   @Override
   public void adjustTurretAngle() {
      if (this.usesModuleDamage() && this.damageState().turretLocked(this.level().isClientSide)) {
         return;
      }

      super.adjustTurretAngle();
   }

   @Override
   public void baseTick() {
      super.baseTick();
      if (!this.level().isClientSide && this.isAlive()) {
         TankDamage.serverTick(this);
      }
   }

   @Override
   public void addAdditionalSaveData(@NotNull CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (!this.level().isClientSide) {
         this.state.bind(this.damageModel());
         tag.put("WarSteelDamage", this.state.save());
      }
   }

   @Override
   protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("WarSteelDamage") && !this.level().isClientSide) {
         this.state.load(tag.getCompound("WarSteelDamage"), this.damageModel());
      }
   }

   /** Client: each hit mark's position on the rendered model (see WarSteelTankRenderer). */
   public final java.util.Map<HitDecals.Decal, float[]> decalSnap = new java.util.HashMap<>();

   /** Client: the hit marks to draw. */
   public java.util.List<HitDecals.Decal> decals() {
      return this.state.clientDecals(this.getEntityData().get(DECALS));
   }

   /**
    * Superb Warfare's repair tool heals vehicles; on a WarSteel tank each pass also patches the
    * oldest hit mark and puts work into the most damaged component.
    */
   @Override
   public void heal(float amount) {
      super.heal(amount);
      if (!this.level().isClientSide && !this.isWreck()) {
         TankDamage.repairWork(this, amount);
      }
   }

   /** Used by debug tooling. */
   public void resetDamage() {
      DamageModel m = this.damageModel();
      this.state.bind(null);
      this.state.bind(m);
      this.state.knockedOut = false;
      this.state.detonated = false;
      this.state.fireTicks = 0;
      this.state.clearDecals();
      this.state.dirty = true;
   }

   @Nullable
   public Entity crewSeatEntity(int seat) {
      return seat >= 0 ? this.getNthEntity(seat) : null;
   }
}
