package rev.rev.warsteel.entity.vehicle;

import com.atsuishio.superbwarfare.tools.OBB;
import net.minecraft.core.BlockPos;
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
   /** Share of full speed the surviving boilers and turbines allow; synced, so the driver's client agrees with the server. */
   private static final EntityDataAccessor<Float> MACHINERY = SynchedEntityData.defineId(T22Entity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> WEIGHT = SynchedEntityData.defineId(T22Entity.class, EntityDataSerializers.FLOAT);
   /** Loaded displacement (t) and Superb Warfare's gravity per tick: flood water adds weight in proportion. */
   private static final double DISPLACEMENT = 1300.0;
   /** Superb Warfare's gravity plus vertical drag as the hull feels it (measured: 0.071 per tick). */
   private static final double EFFECTIVE_GRAVITY = 0.071;
   /** Superb Warfare buoyancy per block submerged (t-22.json) and the hull's depth to the main deck. */
   private static final double BUOYANCY = 0.0215;
   private static final double HULL_DEPTH = 6.7;
   /** Mount A may train 150 degrees either side of the bow before the bridge is in the way. */
   public static final float MOUNT_A_ARC = 150.0F;

   private final ShipFlooding flooding = new ShipFlooding(8, -48.6, 97.2, 9.8, 6.7, DISPLACEMENT);
   private float trim;
   private float trimO;
   private float list;
   private float listO;
   private int sunkTicks;
   @Nullable
   private Matrix4d frame;

   public T22Entity(EntityType<?> type, Level level) {
      super(type, level);
   }

   private int torpedoMount;
   private boolean soundsStarted;
   private float turnRate;
   /** Per tick, how far the rate of turn moves towards what the helm asks for. */
   private static final float TURN_EASE = 0.06F;
   private int floodSoundCooldown;
   private int creakCooldown = 100;

   /** The torpedo mounts fire in turn. */
   public int nextTorpedoMount() {
      int m = this.torpedoMount;
      this.torpedoMount = (this.torpedoMount + 1) % 2;
      return m;
   }

   /** Bow-down trim and list to starboard (degrees) to draw, interpolated. */
   public float visualTrim(float partialTicks) {
      return Mth.lerp(partialTicks, this.trimO, this.trim);
   }

   public float visualList(float partialTicks) {
      return Mth.lerp(partialTicks, this.listO, this.list);
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
      builder.define(MACHINERY, 1.0F);
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

   /** Development only (-Dwarsteel.dev=true): tick timing and a throttle for automated tests. */
   private static final boolean DEV = Boolean.getBoolean("warsteel.dev");
   public float devThrottle;
   public float devRudder;
   private long devNanos;
   private int devTicks;

   @Override
   public void baseTick() {
      this.frame = null;
      long t0 = DEV ? System.nanoTime() : 0L;
      float yawBefore = this.getYRot();
      if (DEV && this.devThrottle != 0.0F) {
         this.setPower(this.devThrottle);
         this.setDeltaRot(this.devRudder);
      }
      super.baseTick();
      if (DEV) {
         this.devNanos += System.nanoTime() - t0;
         if (++this.devTicks == 100) {
            org.slf4j.LoggerFactory.getLogger("warsteel/shipdev").info("{} tick {} us (avg of 100), server mspt {}, pos {} {} {} yaw {} speed {}", this.level().isClientSide ? "client" : "server",
               this.devNanos / 100000, this.level().getServer() != null ? String.format("%.2f", this.level().getServer().getAverageTickTimeNanos() / 1.0E6) : "-", String.format("%.1f", this.getX()), String.format("%.2f", this.getY()), String.format("%.1f", this.getZ()),
               String.format("%.1f", this.getYRot()), String.format("%.3f", this.getDeltaMovement().horizontalDistance()));
            this.devNanos = 0L;
            this.devTicks = 0;
         }
      }
      this.frame = null;
      boolean server = !this.level().isClientSide;
      if (server) {
         this.serverShipTick();
      } else if (!this.soundsStarted) {
         this.soundsStarted = true;
         rev.rev.warsteel.client.sound.ShipSounds.start(this);
      }

      // The steam whistle, on the key Superb Warfare's trucks use for their horn.
      if (this.decoyInputDown()) {
         this.horn();
      }

      // A 1,300 t hull doesn't start or stop turning at once: ease the rate of turn Superb Warfare's
      // ship engine asks for, so she swings into a turn and keeps swinging a little after the rudder
      // is centred, instead of snapping.
      float wanted = Mth.wrapDegrees(this.getYRot() - yawBefore);
      if (this.isControlledByLocalInstance() && Math.abs(wanted) < 20.0F) {   // where she's simulated; not a teleport
         this.turnRate += (wanted - this.turnRate) * TURN_EASE;
         this.setYRot(yawBefore + this.turnRate);
      } else {
         this.turnRate = 0.0F;
      }

      // Superb Warfare's buoyancy grows with the water over the keel, measured across the whole
      // bounding box - which reaches up to the masthead. Only the hull displaces water: past the deck
      // the superstructure adds (next to) nothing, so take the excess back off (after its 0.85 damping).
      double submerged = this.getFluidTypeHeight(net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value());
      if (submerged > HULL_DEPTH) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.85 * BUOYANCY * (submerged - HULL_DEPTH), 0.0));
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

      // Trim and list, eased in. They're drawn by the renderer rather than written into the body's
      // pitch and roll: Superb Warfare damps those every tick and the network rounds them to 1.4
      // degrees, and fighting both made a sinking ship shake.
      this.trimO = this.trim;
      this.listO = this.list;
      this.trim += (this.getEntityData().get(TRIM) - this.trim) * 0.02F;
      this.list += (this.getEntityData().get(LIST) - this.list) * 0.015F;
   }


   private double speedCap() {
      return 0.9 * this.getEntityData().get(MACHINERY) * Math.max(0.15, 1.0 - 0.75 * this.floodRatio());
   }

   /** Boilers each give a share of the steam, the turbine sets each half the shaft power. */
   private float machineryShare() {
      TankState st = this.damageState();
      DamageModel m = st.model();
      if (m == null) {
         return 1.0F;
      }

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

      float share = 1.0F;
      if (boilers > 0) {
         share *= (float)boilersUp / boilers;
      }
      if (turbines > 0) {
         share *= (float)turbinesUp / turbines;
      }
      return share;
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
         this.getEntityData().set(MACHINERY, this.machineryShare());
         this.getEntityData().set(WEIGHT, (float)this.flooding.weightRatio());
         this.getEntityData().set(TRIM, (float)Mth.clamp(trimTarget, -45.0, 45.0));
         this.getEntityData().set(LIST, (float)Mth.clamp(listTarget, -110.0, 110.0));
         if (ratio > 1.0F) {
            this.setPower(0.0F);
         }

         if (in > 0.05 && (this.floodSoundCooldown -= 5) <= 0 && !this.flooding.holes.isEmpty()) {
            // The sea pouring in: from one of the holes under water, louder the more comes in.
            ShipFlooding.Hole h = this.flooding.holes.get(this.random.nextInt(this.flooding.holes.size()));
            Vec3 at = this.toWorld(h.x(), h.y(), h.z());
            level.playSound(null, at.x, at.y, at.z, rev.rev.warsteel.init.ModSounds.T22_FLOOD.get(), net.minecraft.sounds.SoundSource.NEUTRAL,
               (float)Math.min(4.0, 1.2 + in * 0.02), 0.85F + this.random.nextFloat() * 0.3F);
            this.floodSoundCooldown = 50;
         }

         // Plating and frames straining under the weight of water, more often as she settles.
         float strain = Math.abs(this.trim) + Math.abs(this.list) + ratio * 20.0F;
         if (strain > 4.0F && !this.isWreck() && (this.creakCooldown -= 5) <= 0) {
            Vec3 at = this.toWorld(0.0, 4.0, (this.random.nextDouble() - 0.5) * 70.0);
            level.playSound(null, at.x, at.y, at.z, rev.rev.warsteel.init.ModSounds.T22_CREAK.get(), net.minecraft.sounds.SoundSource.NEUTRAL,
               (float)Math.min(4.0, 1.5 + strain * 0.08), 0.8F + this.random.nextFloat() * 0.3F);
            this.creakCooldown = 40 + this.random.nextInt(Math.max(20, (int)(240 - strain * 6)));
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
            this.groan();
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
   private void groan() {
      this.level().playSound(null, this.getX(), this.getY() + 4.0, this.getZ(), rev.rev.warsteel.init.ModSounds.T22_GROAN.get(),
         net.minecraft.sounds.SoundSource.NEUTRAL, 5.0F, 0.9F + this.random.nextFloat() * 0.2F);
   }

   private void breakBack() {
      this.flooding.scuttled = true;
      this.groan();
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

   // ------------------------------------------------------------------------------------------ water

   /** Stations along the keel (Vehicle frame z) where the water is measured. */
   private static final double[] KEEL_STATIONS = {-44.0, -30.0, -15.0, 0.0, 15.0, 30.0, 44.0};

   /**
    * How deep the hull is in the water, from a few stations along the keel. Minecraft (and Superb
    * Warfare, for a vehicle with a collision box) would scan every block inside her bounding box
    * - 15 m by up to 97 m square - several times a tick, which made one ship cost a third of a
    * server tick. Currents don't move a 1,300 t ship, so they're skipped.
    */
   @Override
   public void updateFluidHeightAndDoFluidPushing() {
      if (this.touchingUnloadedChunk()) {
         return;
      }

      double minY = this.getBoundingBox().minY;
      int top = Mth.ceil(this.getBoundingBox().maxY);
      BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
      net.neoforged.neoforge.fluids.FluidType type = null;
      double height = 0.0;
      for (double z : KEEL_STATIONS) {
         Vec3 keel = this.toWorld(0.0, 0.05, z);
         at.set(Mth.floor(keel.x), Mth.floor(minY), Mth.floor(keel.z));
         while (at.getY() < top) {
            net.minecraft.world.level.material.FluidState fluid = this.level().getFluidState(at);
            if (fluid.isEmpty()) {
               break;
            }

            double surface = at.getY() + fluid.getHeight(this.level(), at);
            if (surface - minY > height) {
               height = surface - minY;
               type = fluid.getFluidType();
            }
            at.move(0, 1, 0);
         }
      }

      if (type != null && height > 0.0) {
         this.setFluidTypeHeight(type, height);
      }
   }

   /**
    * Superb Warfare collides the hull's collision box with every block in its world bounding box -
    * at a diagonal heading some 70 by 70 by 8 blocks - every tick it moves. In open water there's
    * nothing to hit, so first sample the hull's outline at the keel and the waterline where she's
    * going; only if one of those points is in something solid (shore, pier, seabed) does the full
    * collision run. A wreck at rest on the bottom stays put.
    */
   @Override
   public void move(net.minecraft.world.entity.MoverType type, Vec3 movement) {
      if (this.isWreck() && this.onGround() && movement.horizontalDistanceSqr() < 1.0E-4) {
         this.setDeltaMovement(Vec3.ZERO);
         return;
      }

      if (type != net.minecraft.world.entity.MoverType.SELF || !this.clearWater(movement)) {
         super.move(type, movement);
         return;
      }

      this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
      this.horizontalCollision = false;
      this.verticalCollision = false;
      this.verticalCollisionBelow = false;
      this.minorHorizontalCollision = false;
      this.setOnGround(false);
   }

   /** Hull half-breadth at the waterline (m) along her length, stern to bow (Vehicle frame z). */
   private static double halfBreadth(double z) {
      if (z > 28.0) {
         return Math.max(0.6, 4.9 - (z - 28.0) * 0.21);
      }
      if (z < -36.0) {
         return Math.max(2.2, 4.9 - (-36.0 - z) * 0.22);
      }
      return 4.9;
   }

   private boolean clearWater(Vec3 movement) {
      BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
      for (double z = -48.0; z <= 48.6; z += 3.0) {   // a piling can't slip between stations
         double hb = halfBreadth(z) + 0.4;
         for (double x : new double[]{-hb, -hb * 0.5, 0.0, hb * 0.5, hb}) {
            for (double y : new double[]{-0.2, 2.6}) {
               Vec3 p = this.toWorld(x, y, z).add(movement);
               at.set(Mth.floor(p.x), Mth.floor(p.y), Mth.floor(p.z));
               if (!this.level().getBlockState(at).getCollisionShape(this.level(), at).isEmpty()) {
                  return false;
               }
            }
         }
      }

      Vec3 bow = this.toWorld(0.0, 2.6, 49.5).add(movement);
      at.set(Mth.floor(bow.x), Mth.floor(bow.y), Mth.floor(bow.z));
      return this.level().getBlockState(at).getCollisionShape(this.level(), at).isEmpty();
   }

   /** Superb Warfare scans the collision box block by block here; the keel stations above are enough. */
   @Override
   public boolean isInFluidType(java.util.function.BiPredicate<net.neoforged.neoforge.fluids.FluidType, Double> predicate) {
      return this.isInFluidType(predicate, false);
   }

   /**
    * Off over the side nearest the seat (t-22.json); if a pier or the shore is in the way there,
    * the other side; if both are blocked, up onto whichever is clear one block higher.
    */
   @Override
   public Vec3 getDismountLocationForIndex(net.minecraft.world.entity.LivingEntity passenger, int index) {
      Vec3 first = super.getDismountLocationForIndex(passenger, index);
      Vector4d l = new Matrix4d(this.vehicleFrame()).invert().transform(new Vector4d(first.x, first.y, first.z, 1.0));
      Vec3 other = this.toWorld(-l.x, l.y, l.z);
      for (Vec3 at : new Vec3[]{first, other, first.add(0, 1.2, 0), other.add(0, 1.2, 0)}) {
         if (this.level().noCollision(passenger, passenger.getDimensions(passenger.getPose()).makeBoundingBox(at))) {
            return at;
         }
      }

      return first;
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
         if (o.part == OBB.Part.COLLISION) {
            continue;
         }
         if (i++ >= 3) {
            break;   // after the collision box, the first three boxes are the hull (midships, bow, stern)
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
