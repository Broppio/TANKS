package rev.rev.warsteel.combat;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rev.rev.warsteel.damage.TankDamage;
import rev.rev.warsteel.damage.WarSteelTank;

/**
 * High-explosive detonation scaled from the charge, replacing Superb Warfare's fixed-radius
 * explosion for WarSteel shells.
 *
 * Blast: overpressure falls with the scaled distance Z = R / W^(1/3) (Hopkinson-Cranz, W in kg
 * TNT). It is lethal to a person around Z = 2 and harmless beyond Z = 6, so a 76 mm OF-350
 * (0.71 kg) kills within about 1.8 m and a 152 mm OF-530 (6.9 kg) within about 3.8 m.
 *
 * Fragments: the casing breaks into roughly 900 * sqrt(W) useful fragments thrown out evenly.
 * Each target in line of sight is hit by about N * area / (4 pi R^2) of them (half go into the
 * ground), each doing less damage with range. That makes a 76 mm shell dangerous out to about
 * 20 m without being reliably lethal past a few metres, as in reality.
 *
 * Craters: a small vanilla explosion (no entity damage) sized to the charge, only when Superb
 * Warfare's explosion block damage is enabled.
 */
public final class HeExplosion {
   private static final ExplosionDamageCalculator BLOCKS_ONLY = new ExplosionDamageCalculator() {
      @Override
      public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
         return false;
      }

      @Override
      public float getKnockbackMultiplier(Entity entity) {
         return 0.0F;
      }
   };

   private HeExplosion() {
   }

   /**
    * @param fillerGrams  TNT-equivalent charge
    * @param shell        the projectile (for damage attribution), may be null
    * @param owner        whoever fired it
    * @param skip         a tank already handled by the armour model (its modules took the blast), or null
    * @param outsideScale 1 for an open-air burst; lower when the burst is inside a hull and mostly contained
    * @param craters      whether to damage blocks
    */
   public static void detonate(
      ServerLevel level, Vec3 pos, double fillerGrams, @Nullable Entity shell, @Nullable Entity owner, @Nullable Entity skip, double outsideScale, boolean craters
   ) {
      double kg = Math.max(0.01, fillerGrams / 1000.0);
      double cbrt = Math.cbrt(kg);
      RandomSource rnd = level.random;
      effects(level, pos, kg, cbrt);
      DamageSource source = ModDamageTypes.causeCustomExplosionDamage(level.registryAccess(), shell != null ? shell : owner, owner);
      double blastRange = 6.0 * cbrt;
      double fragRange = 22.0 * cbrt + 4.0;
      int fragments = (int)(900.0 * Math.sqrt(kg));
      List<Entity> near = level.getEntities(shell, new AABB(pos, pos).inflate(fragRange), e -> e != skip && e.isAlive() && e.getVehicle() == null);

      for (Entity e : near) {
         AABB box = e.getBoundingBox();
         Vec3 nearest = new Vec3(clamp(pos.x, box.minX, box.maxX), clamp(pos.y, box.minY, box.maxY), clamp(pos.z, box.minZ, box.maxZ));
         double r = Math.max(0.3, nearest.distanceTo(pos));
         boolean sighted = lineOfSight(level, pos, e, shell);
         if (e instanceof WarSteelTank tank && tank.usesModuleDamage()) {
            if (r < fragRange) {
               TankDamage.blastFromOutside(tank, level, pos, fillerGrams * outsideScale, source, owner);
            }
         } else if (e instanceof VehicleEntity vehicle) {
            float dmg = 0.0F;
            // Armoured vehicles: only heavy charges close by do much.
            if (r < blastRange) {
               dmg += (float)(kg * 70.0 * outsideScale * (1.0 - r / blastRange));
            }

            // Aircraft are thin-skinned: a flak burst riddles them with fragments.
            if (FlakFuze.isAircraft(vehicle) && sighted && r < fragRange) {
               double area = Math.max(1.0, vehicle.getBbWidth() * vehicle.getBbHeight());
               int hits = poisson(fragments * outsideScale * area / (4.0 * Math.PI * r * r), rnd);
               dmg += (float)(hits * (4.0 * (1.0 - r / fragRange) + 1.0));
            }

            if (shell != null && rev.rev.warsteel.damage.WarSteelCommands.isTestShell(shell)) {
               org.slf4j.LoggerFactory.getLogger("warsteel/he").info("HE {} g at {} m: vehicle {} {} damage (health {})", (int)fillerGrams,
                  String.format(java.util.Locale.ROOT, "%.1f", r), vehicle.getType().getDescriptionId(), String.format(java.util.Locale.ROOT, "%.1f", dmg), vehicle.getHealth());
            }

            if (dmg > 0.5F) {
               vehicle.hurt(source, dmg);
            }
         } else if (e instanceof LivingEntity living) {
            float damage = 0.0F;
            double z = r / cbrt;
            if (z < 6.0) {
               double blast = 45.0 * Math.pow(1.0 - z / 6.0, 2.0) * outsideScale;
               damage += (float)(sighted ? blast : blast * 0.5);
            }

            if (sighted && r < fragRange) {
               double area = Math.max(0.2, e.getBbWidth() * e.getBbHeight());
               double expected = fragments * outsideScale * area / (4.0 * Math.PI * r * r) * 0.5;
               int hits = poisson(expected, rnd);
               double perHit = 6.0 * (1.0 - r / fragRange) + 1.0;
               damage += (float)(hits * perHit);
            }

            if (shell != null && rev.rev.warsteel.damage.WarSteelCommands.isTestShell(shell)) {
               org.slf4j.LoggerFactory.getLogger("warsteel/he")
                  .info("HE {} g at {} m: {} {} damage (blast + {} fragments), sighted={}", (int)fillerGrams, String.format(java.util.Locale.ROOT, "%.1f", r),
                     e.getType().getDescriptionId(), String.format(java.util.Locale.ROOT, "%.1f", damage), sighted && r < fragRange ? "some" : "no", sighted);
            }

            if (damage > 0.0F) {
               living.invulnerableTime = 0;
               living.hurt(source, damage);
               double push = Math.max(0.0, 1.0 - z / 6.0) * 0.8 * outsideScale;
               if (push > 0.0) {
                  Vec3 away = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0).subtract(pos).normalize().scale(push);
                  e.push(away.x, away.y * 0.6 + push * 0.3, away.z);
                  e.hurtMarked = true;
               }
            }
         } else if (r < blastRange * 0.5 && e.isPickable()) {
            e.hurt(source, (float)(10.0 * outsideScale));
         }
      }

      if (craters && outsideScale > 0.5 && ExplosionConfig.EXPLOSION_DESTROY.get()) {
         float power = (float)Math.max(0.5, 1.3 * cbrt);
         level.explode(shell, null, BLOCKS_ONLY, pos.x, pos.y, pos.z, power, false, Level.ExplosionInteraction.TNT, ParticleTypes.SMOKE, ParticleTypes.SMOKE, net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvents.EMPTY));
      }
   }

   private static void effects(ServerLevel level, Vec3 pos, double kg, double cbrt) {
      if (kg < 0.2) {
         ParticleTool.spawnMiniExplosionParticles(level, pos);
      } else if (kg < 1.0) {
         ParticleTool.spawnSmallExplosionParticles(level, pos);
      } else if (kg < 3.0) {
         ParticleTool.spawnMediumExplosionParticles(level, pos);
      } else {
         ParticleTool.spawnLargeExplosionParticles(level, pos);
      }

      // Earth and debris thrown up from whatever was underneath.
      BlockPos below = BlockPos.containing(pos.x, pos.y - 0.3, pos.z);
      BlockState ground = level.getBlockState(below);
      if (!ground.isAir()) {
         level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), pos.x, pos.y + 0.2, pos.z, (int)(30 * cbrt) + 8, 0.6 * cbrt, 0.3, 0.6 * cbrt, 0.35);
      }

      SoundEvent close = kg >= 3.0 ? ModSounds.HUGE_EXPLOSION_CLOSE.get() : ModSounds.EXPLOSION_CLOSE.get();
      SoundEvent far = kg >= 3.0 ? ModSounds.HUGE_EXPLOSION_FAR.get() : ModSounds.EXPLOSION_FAR.get();
      SoundEvent veryFar = kg >= 3.0 ? ModSounds.HUGE_EXPLOSION_VERY_FAR.get() : ModSounds.EXPLOSION_VERY_FAR.get();
      float pitch = (float)(1.15 - Math.min(0.45, cbrt * 0.2)) + level.random.nextFloat() * 0.1F;
      // Minecraft hears a sound out to 16 x volume blocks, fading linearly. The sharp close blast
      // carries ~25-55 blocks, the duller far report ~60-120 and the low rumble ~120-220.
      level.playSound(null, pos.x, pos.y, pos.z, close, SoundSource.BLOCKS, (float)(1.0 + cbrt * 1.1), pitch);
      level.playSound(null, pos.x, pos.y, pos.z, far, SoundSource.BLOCKS, (float)(2.5 + cbrt * 2.5), pitch);
      level.playSound(null, pos.x, pos.y, pos.z, veryFar, SoundSource.BLOCKS, (float)(5.0 + cbrt * 4.0), pitch);
   }

   private static boolean lineOfSight(ServerLevel level, Vec3 from, Entity target, @Nullable Entity shell) {
      Vec3[] points = {target.getEyePosition(), target.position().add(0.0, target.getBbHeight() * 0.5, 0.0), target.position().add(0.0, 0.1, 0.0)};

      for (Vec3 to : points) {
         HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shell != null ? shell : target));
         if (hit.getType() == HitResult.Type.MISS) {
            return true;
         }
      }

      return false;
   }

   private static int poisson(double mean, RandomSource rnd) {
      if (mean <= 0.0) {
         return 0;
      } else if (mean > 30.0) {
         return (int)Math.max(0.0, Math.round(mean + Math.sqrt(mean) * rnd.nextGaussian()));
      } else {
         double l = Math.exp(-mean);
         double p = 1.0;
         int k = 0;

         do {
            k++;
            p *= rnd.nextDouble();
         } while (p > l);

         return k - 1;
      }
   }

   private static double clamp(double v, double lo, double hi) {
      return Math.max(lo, Math.min(hi, v));
   }
}
