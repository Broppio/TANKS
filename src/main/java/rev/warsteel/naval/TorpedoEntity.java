package rev.rev.warsteel.naval;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.tools.OBB;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rev.rev.warsteel.combat.HeExplosion;
import rev.rev.warsteel.damage.Obb;

/**
 * G7a 53.3 cm torpedo: 44 knots (1.13 blocks/tick), run at 1.5 m depth, 6 km range at that speed;
 * 280 kg hexanite (about 340 kg TNT). Arms after 20 m. Bursts against a ship's hull boxes or
 * anything solid; an underwater burst against a hull tears a hole tens of square metres across.
 */
public class TorpedoEntity extends Entity {
   public static final double SPEED = 1.13;
   public static final double RUN_DEPTH = 1.5;
   public static final double WARHEAD_KG = 340.0;
   private static final int LIFE = 5400;
   private static final double ARMING = 20.0;
   @Nullable
   private UUID owner;
   @Nullable
   private UUID launcher;
   private Vec3 start = Vec3.ZERO;
   private float heading;

   public TorpedoEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   public void launch(@Nullable Entity owner, @Nullable Entity launcher, Vec3 pos, float headingDeg) {
      this.owner = owner != null ? owner.getUUID() : null;
      this.launcher = launcher != null ? launcher.getUUID() : null;
      this.setPos(pos);
      this.start = pos;
      this.heading = headingDeg;
      this.setYRot(headingDeg);
      Vec3 dir = Vec3.directionFromRotation(0.0F, headingDeg);
      this.setDeltaMovement(dir.scale(0.6).add(0, -0.05, 0));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
   }

   @Override
   public void tick() {
      super.tick();
      Level level = this.level();
      Vec3 dir = Vec3.directionFromRotation(0.0F, this.heading);
      boolean inWater = level.getFluidState(this.blockPosition()).is(FluidTags.WATER);
      Vec3 v;
      if (!inWater) {
         // Off the launcher and dropping into the sea.
         v = this.getDeltaMovement().add(0, -0.06, 0);
      } else {
         double surface = this.surfaceY();
         double dy = Mth.clamp((surface - RUN_DEPTH) - this.getY(), -0.15, 0.15);
         v = dir.scale(Math.min(SPEED, this.getDeltaMovement().horizontalDistance() + 0.05)).add(0, dy, 0);
      }

      if (!level.isClientSide) {
         if (this.tickCount > LIFE) {
            this.discard();
            return;
         }

         boolean armed = this.position().distanceTo(this.start) > ARMING;
         Vec3 from = this.position();
         Vec3 to = from.add(v);
         if (armed) {
            Vec3 hit = this.findHit(from, to);
            if (hit != null) {
               this.detonate((ServerLevel)level, hit);
               return;
            }

            if (inWater && !level.getBlockState(BlockPos.containing(to)).getCollisionShape(level, BlockPos.containing(to)).isEmpty()) {
               this.detonate((ServerLevel)level, from);
               return;
            }
         } else if (!level.getBlockState(BlockPos.containing(to)).getCollisionShape(level, BlockPos.containing(to)).isEmpty() && this.tickCount > 40) {
            this.discard();   // ran into the shore unarmed: a dud
            return;
         }
      } else if (inWater && this.tickCount % 2 == 0) {
         // The bubble track a torpedo leaves on the surface.
         double surface = this.surfaceY();
         level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, this.getX(), this.getY(), this.getZ(), 0, 0.2, 0);
         level.addParticle(ParticleTypes.SPLASH, this.getX(), surface, this.getZ(), 0, 0.05, 0);
      }

      this.setDeltaMovement(v);
      this.move(MoverType.SELF, v);
   }

   private double surfaceY() {
      BlockPos.MutableBlockPos m = this.blockPosition().mutable();
      int i = 0;
      while (i++ < 16 && this.level().getFluidState(m.above()).is(FluidTags.WATER)) {
         m.move(0, 1, 0);
      }

      return m.getY() + 0.9;
   }

   @Nullable
   private Vec3 findHit(Vec3 from, Vec3 to) {
      Vec3 d = to.subtract(from);
      double len = d.length();
      if (len < 1.0E-6) {
         return null;
      }

      Vec3 dir = d.scale(1.0 / len);
      AABB sweep = new AABB(from, to).inflate(60.0);
      Vec3 best = null;
      double bestT = len;
      for (Entity e : this.level().getEntities(this, sweep, e -> e.isAlive() && !e.getUUID().equals(this.launcher) && !(e instanceof TorpedoEntity))) {
         if (e instanceof VehicleEntity v) {
            for (OBB o : v.getOBBs()) {
               Obb box = new Obb(new Vec3(o.center.x, o.center.y, o.center.z), o.rotation(), o.extents().x, o.extents().y, o.extents().z);
               double[] t = box.intersect(from, dir);
               if (t != null && t[0] >= 0 && t[0] <= bestT) {
                  bestT = t[0];
                  best = from.add(dir.scale(t[0]));
               }
            }
         } else if (e.getBoundingBox().inflate(0.2).clip(from, to).isPresent()) {
            Vec3 p = e.getBoundingBox().inflate(0.2).clip(from, to).get();
            double t = p.distanceTo(from);
            if (t <= bestT) {
               bestT = t;
               best = p;
            }
         }
      }

      return best;
   }

   private void detonate(ServerLevel level, Vec3 at) {
      Entity owner = this.owner != null ? level.getEntity(this.owner) : null;
      level.sendParticles(ParticleTypes.CLOUD, at.x, this.surfaceY() + 1.0, at.z, 80, 2.5, 6.0, 2.5, 0.25);
      level.sendParticles(ParticleTypes.SPLASH, at.x, this.surfaceY() + 0.5, at.z, 300, 3.0, 2.0, 3.0, 0.6);
      HeExplosion.detonate(level, at, WARHEAD_KG * 1000.0, this, owner, null, 1.0, true);
      this.discard();
   }

   @Override
   protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
      if (tag.hasUUID("Owner")) {
         this.owner = tag.getUUID("Owner");
      }
      if (tag.hasUUID("Launcher")) {
         this.launcher = tag.getUUID("Launcher");
      }
      this.heading = tag.getFloat("Heading");
      this.start = new Vec3(tag.getDouble("SX"), tag.getDouble("SY"), tag.getDouble("SZ"));
   }

   @Override
   protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
      if (this.owner != null) {
         tag.putUUID("Owner", this.owner);
      }
      if (this.launcher != null) {
         tag.putUUID("Launcher", this.launcher);
      }
      tag.putFloat("Heading", this.heading);
      tag.putDouble("SX", this.start.x);
      tag.putDouble("SY", this.start.y);
      tag.putDouble("SZ", this.start.z);
   }

   @Override
   public boolean isPickable() {
      return false;
   }
}
