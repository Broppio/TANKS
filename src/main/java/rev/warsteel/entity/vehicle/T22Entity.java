package rev.rev.warsteel.entity.vehicle;

import com.atsuishio.superbwarfare.tools.OBB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import rev.rev.warsteel.damage.DamageModel;
import rev.rev.warsteel.damage.ModuleDef;
import rev.rev.warsteel.damage.ModuleType;
import rev.rev.warsteel.damage.Obb;
import rev.rev.warsteel.damage.TankDamage;
import rev.rev.warsteel.damage.TankState;
import rev.rev.warsteel.damage.WarSteelTank;
import rev.rev.warsteel.naval.NavalHull;
import rev.rev.warsteel.naval.ShipFlooding;

/**
 * Flottentorpedoboot T-22 (Elbing class, 1942): 102 m, 1,300 t, 33 kn; four 10.5 cm SK C/32, two
 * 3.7 cm Flakzwilling, a 2 cm Flakvierling, single 2 cm Flak and two triple 53.3 cm torpedo mounts.
 *
 * <p>Superb Warfare floats and drives it (a Ship engine); WarSteel adds what a warship needs:
 * <ul>
 * <li>Flooding ({@link ShipFlooding}): shell hits, torpedoes and any explosion near the hull hole
 *     it; water comes in by the depth of each hole, the ship settles, trims and lists, and once
 *     its reserve buoyancy is gone it goes down or capsizes.</li>
 * <li>Machinery: each boiler lost takes a quarter of its power; a turbine set lost, half.</li>
 * <li>A knocked-out ship (magazine detonation, crew lost) isn't blown away: its back is broken and
 *     it sinks. It becomes a wreck once it's under water.</li>
 * </ul>
 */
public class T22Entity extends WarSteelTank implements NavalHull, ShipFlooding.Hull {
   private static final EntityDataAccessor<Float> FLOOD = SynchedEntityData.defineId(T22Entity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> TRIM = SynchedEntityData.defineId(T22Entity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> LIST = SynchedEntityData.defineId(T22Entity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> WEIGHT = SynchedEntityData.defineId(T22Entity.class, EntityDataSerializers.FLOAT);
   /** Loaded displacement (t) and Superb Warfare's gravity per tick: flood water adds weight in proportion. */
   private static final double DISPLACEMENT = 1300.0;
   /** Superb Warfare's gravity plus vertical drag as the hull feels it (measured: 0.071 per tick). */
   private static final double EFFECTIVE_GRAVITY = 0.071;
   /** Mount A may train 150 degrees either side of the bow before the bridge is in the way. */
   public static final float MOUNT_A_ARC = 150.0F;

   private final ShipFlooding flooding = new ShipFlooding(8, -48.6, 97.2, 9.8, 6.7, DISPLACEMENT);
   private float appliedTrim;
   private float appliedList;
   private float trim;
   private float list;
   private int sunkTicks;
   @Nullable
   private Matrix4d frame;

   public T22Entity(EntityType<?> type, Level level) {
      super(type, level);
   }

   private int torpedoMount;

   /** The torpedo mounts fire in turn. */
   public int nextTorpedoMount() {
      int m = this.torpedoMount;
      this.torpedoMount = (this.torpedoMount + 1) % 2;
      return m;
   }

   public ShipFlooding flooding() {
      return this.flooding;
   }

   /** Synced share of reserve buoyancy flooded (client and server). */
   public float floodRatio() {
      return this.getEntityData().get(FLOOD);
   }

   @Override
   protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FLOOD, 0.0F);
      builder.define(TRIM, 0.0F);
      builder.define(LIST, 0.0F);
      builder.define(WEIGHT, 0.0F);
   }

   @Override
   public void adjustTurretAngle() {
      super.adjustTurretAngle();
      float yaw = Mth.wrapDegrees(this.getTurretYRot());
      if (Math.abs(yaw) > MOUNT_A_ARC) {
         this.setTurretYRot(Math.copySign(MOUNT_A_ARC, yaw));
      }
   }

   // ------------------------------------------------------------------------------------------ tick

   @Override
   public void baseTick() {
      // Take our trim and list off before Superb Warfare's ship engine damps the body's pitch and roll.
      this.setXRot(this.getXRot() - this.appliedTrim);
      this.setZRot(this.getRoll() - this.appliedList);
      this.frame = null;
      super.baseTick();
      this.frame = null;
      boolean server = !this.level().isClientSide;
      if (server) {
         this.serverShipTick();
      }

      // Both sides (the driver's client moves the ship): flood water is weight - as a share of her
      // displacement, against the buoyancy of the hull Superb Warfare sees in the water.
      float weight = this.getEntityData().get(WEIGHT);
      if (weight > 0.0F) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -EFFECTIVE_GRAVITY * weight, 0.0));
      }

      // Machinery and flooding limit speed.
      double cap = this.speedCap();
      Vec3 v = this.getDeltaMovement();
      double h = Math.sqrt(v.x * v.x + v.z * v.z);
      if (h > cap) {
         this.setDeltaMovement(v.x * cap / h, v.y, v.z * cap / h);
      }

      // Trim and list, eased in.
      this.trim += (this.getEntityData().get(TRIM) - this.trim) * 0.02F;
      this.list += (this.getEntityData().get(LIST) - this.list) * 0.015F;
      this.appliedTrim = this.trim;
      this.appliedList = this.list;
      this.setXRot(this.getXRot() + this.appliedTrim);
      this.setZRot(this.getRoll() + this.appliedList);
   }

   private double speedCap() {
      double cap = 0.9;
      if (!this.level().isClientSide) {
         TankState st = this.damageState();
         DamageModel m = st.model();
         if (m != null) {
            int boilers = 0;
            int boilersUp = 0;
            int turbines = 0;
            int turbinesUp = 0;
            for (ModuleDef d : m.modules()) {
               if (d.type() == ModuleType.ENGINE) {
                  boilers++;
                  boilersUp += st.destroyed(d.index()) ? 0 : 1;
               } else if (d.type() == ModuleType.TRANSMISSION) {
                  turbines++;
                  turbinesUp += st.destroyed(d.index()) ? 0 : 1;
               }
            }

            if (boilers > 0) {
               cap *= (double)boilersUp / boilers;
            }

            if (turbines > 0) {
               cap *= (double)turbinesUp / turbines;
            }
         }
      }

      return cap * Math.max(0.15, 1.0 - 0.75 * this.floodRatio());
   }

   private void serverShipTick() {
      ServerLevel level = (ServerLevel)this.level();
      TankState st = this.damageState();
      if ((st.knockedOut() || this.isWreck()) && !this.flooding.scuttled) {
         this.breakBack();
      }

      if (this.tickCount % 5 == 0) {
         DamageModel m = st.model();
         boolean steam = false;
         if (m != null) {
            for (ModuleDef d : m.modules()) {
               if (d.type() == ModuleType.ENGINE && !st.destroyed(d.index())) {
                  steam = true;
                  break;
               }
            }
         }

         double in = this.flooding.step(this, 0.25, steam && !this.flooding.scuttled ? 0.7 : 0.0);
         float ratio = (float)this.flooding.floodRatio();
         double trimTarget = this.flooding.trimDegrees();
         double listTarget = this.flooding.listDegrees();
         if (ratio > 0.9F) {
            // Going down: the flooded end drops away and she plunges.
            trimTarget += Math.copySign(Math.min(35.0, (ratio - 0.9) * 60.0), trimTarget == 0 ? 1 : trimTarget);
         }

         if (Math.abs(listTarget) > 30.0 && ratio > 0.55F) {
            // Lost her stability: she rolls over.
            listTarget = Math.copySign(Math.min(110.0, Math.abs(listTarget) * (1.0 + (ratio - 0.55) * 4.0)), listTarget);
         }

         this.getEntityData().set(FLOOD, ratio);
         this.getEntityData().set(WEIGHT, (float)this.flooding.weightRatio());
         this.getEntityData().set(TRIM, (float)Mth.clamp(trimTarget, -45.0, 45.0));
         this.getEntityData().set(LIST, (float)Mth.clamp(listTarget, -110.0, 110.0));
         if (ratio > 1.0F) {
            this.setPower(0.0F);
         }

         if (in > 0.05) {
            // Water rushing in through the holes below the waterline.
            for (ShipFlooding.Hole h : this.flooding.holes) {
               Vec3 at = this.toWorld(h.x(), h.y(), h.z());
               if (this.level().getFluidState(net.minecraft.core.BlockPos.containing(at)).isSource()) {
                  level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, at.x, at.y, at.z, 3 + (int)Math.min(12, h.area() * 2), 0.4, 0.3, 0.4, 0.05);
               }
            }
         }
      }

      // Under water: the crew abandon ship, and the hulk settles as a wreck.
      double deck = this.depthOfDeck();
      if (deck > 1.5) {
         this.ejectPassengers();
         if (++this.sunkTicks > 200 && !this.isWreck()) {
            super.setHealth(0.0F);
         }
      } else {
         this.sunkTicks = Math.max(0, this.sunkTicks - 1);
      }
   }

   /** How far the middle of the main deck is under water (0 while she's afloat). */
   private double depthOfDeck() {
      return ShipFlooding.depthBelowSurface(this.level(), this.toWorld(0, 6.7, 0));
   }

   /** Magazine detonation or the crew lost: her back is broken and she floods fast. */
   private void breakBack() {
      this.flooding.scuttled = true;
      TankState st = this.damageState();
      DamageModel m = st.model();
      double z = 0.0;
      if (m != null && st.detonated()) {
         for (ModuleDef d : m.modules()) {
            if (d.type() == ModuleType.AMMO_RACK && st.destroyed(d.index()) && d.position()[1] < 4.0) {
               z = d.position()[2];
               break;
            }
         }
      }

      int c = this.flooding.compartmentAt(z);
      for (int k = Math.max(0, c - 1); k <= Math.min(this.flooding.compartments() - 1, c + 1); k++) {
         this.flooding.addHole(k, 0.0F, 0.3F, (float)this.flooding.centreZ(k), k == c ? 30.0F : 12.0F);
      }
   }

   /** Health reaching zero through Superb Warfare means the ship is lost: she sinks, then becomes a wreck. */
   @Override
   public void setHealth(float health) {
      if (health <= 0.0F && !this.level().isClientSide && !this.isWreck() && this.depthOfDeck() < 1.5) {
         super.setHealth(Math.max(1.0F, this.getMaxHealth() * 0.02F));
         if (!this.flooding.scuttled) {
            this.breakBack();
         }
         return;
      }

      super.setHealth(health);
   }

   // ------------------------------------------------------------------------------------------ damage

   @Override
   public boolean hurt(@NotNull DamageSource source, float amount) {
      if (this.level().isClientSide) {
         return super.hurt(source, amount);
      }

      // Explosions are handled where they happen (naval.ShipExplosions, HeExplosion): they hole the
      // hull and wreck what's near them, wherever along the ship they go off.
      if (source.is(DamageTypeTags.IS_EXPLOSION)) {
         return false;
      }

      // Bullets and the like from any weapon: the exposed gun crews take them.
      Vec3 from = source.getSourcePosition();
      DamageModel m = this.damageModel();
      if (from != null && m != null && !TankDamage.isHandled(source.getDirectEntity()) && source.getDirectEntity() != null
         && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         ModuleDef nearest = null;
         double best = 9.0;
         for (ModuleDef d : m.modules()) {
            if (d.type() == ModuleType.CREW && d.external()) {
               Vec3 c = this.toWorld(d.position()[0], d.position()[1], d.position()[2]);
               double dist = c.distanceToSqr(from);
               if (dist < best) {
                  best = dist;
                  nearest = d;
               }
            }
         }

         if (nearest != null) {
            TankDamage.damageModule(this, (ServerLevel)this.level(), nearest.name(), amount * 2.0F, source.getEntity());
            return true;
         }
      }

      return super.hurt(source, amount);
   }

   @Override
   public void hullBlast(Vec3 pos, double kgTnt, @Nullable Entity attacker) {
      if (this.level().isClientSide || kgTnt <= 0) {
         return;
      }

      double reach = 0.9 * Math.cbrt(kgTnt) + 0.6;
      Vec3 nearest = null;
      double best = Double.MAX_VALUE;
      for (Obb hull : this.hullBoxes()) {
         Vector3d l = hull.toLocal(pos);
         Vec3 p = hull.toWorld(Mth.clamp(l.x, -hull.hx, hull.hx), Mth.clamp(l.y, -hull.hy, hull.hy), Mth.clamp(l.z, -hull.hz, hull.hz));
         double d = p.distanceTo(pos);
         if (d < best) {
            best = d;
            nearest = p;
         }
      }

      if (nearest == null || best > reach) {
         return;
      }

      // Plating torn open; under water the blast is tamped and does far more.
      boolean underwater = ShipFlooding.depthBelowSurface(this.level(), pos) > 0.3;
      double area = 0.45 * Math.pow(kgTnt, 2.0 / 3.0) * (1.0 - best / reach) * (underwater ? 3.0 : 1.0);
      this.addHole(nearest, (float)Math.min(40.0, area));
   }

   @Override
   public void hullHole(Vec3 pos, double areaM2) {
      if (!this.level().isClientSide) {
         this.addHole(pos, (float)areaM2);
      }
   }

   private void addHole(Vec3 world, float area) {
      if (area < 0.005F) {
         return;
      }

      Vector4d l = new Matrix4d(this.vehicleFrame()).invert().transform(new Vector4d(world.x, world.y, world.z, 1.0));
      int c = this.flooding.compartmentAt(l.z);
      this.flooding.addHole(c, (float)l.x, (float)Math.max(0.1, l.y), (float)l.z, area);
      if (area > 8.0F) {
         // A big hole spans a bulkhead.
         int next = l.z > this.flooding.centreZ(c) ? c + 1 : c - 1;
         if (next >= 0 && next < this.flooding.compartments()) {
            this.flooding.addHole(next, (float)l.x, (float)Math.max(0.1, l.y), (float)this.flooding.centreZ(next), area * 0.5F);
         }
      }
   }

   private java.util.List<Obb> hullBoxes() {
      java.util.List<Obb> out = new java.util.ArrayList<>();
      int i = 0;
      for (OBB o : this.getOBBs()) {
         if (i++ >= 3) {
            break;   // the first three boxes are the hull (midships, bow, stern)
         }
         out.add(new Obb(new Vec3(o.center.x, o.center.y, o.center.z), o.rotation(), o.extents().x, o.extents().y, o.extents().z));
      }
      return out;
   }

   private Matrix4d vehicleFrame() {
      if (this.frame == null) {
         this.frame = this.getTransformFromString("Vehicle");
      }
      return this.frame;
   }

   @Override
   public Vec3 toWorld(double x, double y, double z) {
      Vector4d v = this.vehicleFrame().transform(new Vector4d(x, y, z, 1.0));
      return new Vec3(v.x, v.y, v.z);
   }

   // ------------------------------------------------------------------------------------------ save

   @Override
   public void addAdditionalSaveData(@NotNull CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.put("WarSteelFlooding", this.flooding.save());
   }

   @Override
   protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("WarSteelFlooding")) {
         this.flooding.load(tag.getCompound("WarSteelFlooding"));
      }
   }

   @Override
   public void resetDamage() {
      super.resetDamage();
      this.flooding.reset();
   }
}
