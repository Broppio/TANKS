package rev.rev.warsteel.combat;

import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.mojang.datafixers.util.Pair;
import java.util.Locale;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.Nullable;
import rev.rev.warsteel.damage.ModuleDef;
import rev.rev.warsteel.damage.TankDamage;
import rev.rev.warsteel.damage.WarSteelTank;

import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent.HitEntity;
import com.atsuishio.superbwarfare.client.particle.CustomFlareOption;
import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.MinecraftUtil;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector4d;
import rev.rev.warsteel.init.ModSounds;

public final class RicochetHandler {
   @NotNull
   public static final RicochetHandler INSTANCE = new RicochetHandler();
   public static final boolean DEBUG_TO_CHAT = true;
   public static final boolean SHOOTER_CUES = true;
   @NotNull
   private static final HashMap<Integer, Pair<Integer, Long>> bounceImmunity = new HashMap<>();

   private RicochetHandler() {
   }

   @SubscribeEvent
   public final void onServerTick(@NotNull ServerTickEvent.Post event) {
      if (bounceImmunity.size() > 64) {
         bounceImmunity.clear();
      }
   }

   @SubscribeEvent
   public final void onAddReloadListeners(@NotNull AddReloadListenerEvent event) {
      RicochetHandlerKt.access$getLOG$p().info("registering warsteel shell + armour reload listeners");
      event.addListener((PreparableReloadListener)ShellRegistry.INSTANCE);
      event.addListener((PreparableReloadListener)ArmorRegistry.INSTANCE);
      event.addListener(rev.rev.warsteel.damage.DamageModelRegistry.INSTANCE);
   }

   /** The WarSteel profile for a shell fired from a WarSteel gun (or /warsteel fire), else null. */
   @Nullable
   private ShellData profileOf(CannonShellEntity shell, @Nullable Entity owner) {
      ShellData data = rev.rev.warsteel.damage.WarSteelCommands.forcedProfile(shell);
      return data != null ? data : ShellRegistry.INSTANCE.resolve(this.firingVehicleId(owner), shell.getDamage(), shell.canPassThroughFluid());
   }

   /** WarSteel HE and APHE rounds hitting the ground or a wall burst with the realistic charge. */
   @SubscribeEvent
   public final void onProjectileHitBlock(@NotNull com.atsuishio.superbwarfare.api.event.ProjectileHitEvent.HitBlock event) {
      if (!(event.getProjectile() instanceof CannonShellEntity shell) || !(shell.level() instanceof ServerLevel level)) {
         return;
      }

      ShellData data = this.profileOf(shell, event.getOwner());
      if (data == null || data.getFiller() <= 0.0) {
         return; // solid shot keeps Superb Warfare's block-penetration behaviour
      }

      event.setCanceled(true);
      Vec3 dir = shell.getDeltaMovement().lengthSqr() > 1.0E-6 ? shell.getDeltaMovement().normalize() : Vec3.ZERO;
      HeExplosion.detonate(level, event.getHitVec().subtract(dir.scale(0.15)), data.getFiller(), shell, event.getOwner(), null, 1.0, true);
      shell.discard();
   }

   @SubscribeEvent
   public final void onProjectileHitEntity(@NotNull HitEntity event) {
      if (!(event.getTarget() instanceof VehicleEntity target)) {
         this.onShellHitsSoftTarget(event);
         return;
      }

      if (!(event.getProjectile() instanceof CannonShellEntity shell)) {
         return;
      }

      if (!(target.level() instanceof ServerLevel level)) {
         return;
      }

      ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
      ArmorData armour = ArmorRegistry.INSTANCE.get(typeId);
      if (armour == null) {
         return;
      }

      long now = level.getGameTime();
      Pair<Integer, Long> immunity = bounceImmunity.get(shell.getId());
      if (immunity != null) {
         if (now >= immunity.getSecond()) {
            bounceImmunity.remove(shell.getId());
         } else if (immunity.getFirst() == target.getId()) {
            event.setCanceled(true);
            return;
         }
      }

      Vec3 velocity = shell.getDeltaMovement();
      if (velocity.lengthSqr() < 1.0E-6) {
         return;
      }

      Vec3 dir = velocity.normalize();
      Vec3 hitVec = event.getHitVec();
      WarSteelTank tank = target instanceof WarSteelTank t && t.usesModuleDamage() ? t : null;
      // SBW marks its armour-piercing cannon shell type as the only one that passes through fluids.
      boolean apType = shell.canPassThroughFluid();
      String firedFrom = this.firingVehicleId(event.getOwner());
      ShellData data = rev.rev.warsteel.damage.WarSteelCommands.forcedProfile(shell);
      if (data == null) {
         data = ShellRegistry.INSTANCE.resolve(firedFrom, shell.getDamage(), apType);
      }

      if (data == null) {
         if (tank == null) {
            this.debug(event, "foreign shell from " + (firedFrom == null ? "unknown" : firedFrom) + ", leaving SBW damage alone");
            return;
         }

         data = ShellData.generic(apType, shell.getDamage(), shell.getExplosionDamage(), shell.getExplosionRadius());
      }

      Entity owner = event.getOwner();
      DamageSource source = ModDamageTypes.causeProjectileHitDamage(level.registryAccess(), shell, owner);
      double pen = data.getPenetration().at(this.flightDistance(owner, shell, hitVec));
      MutableComponent prefix = Component.empty();

      // 1. External modules (tracks, barrel) in front of the hull act as spaced armour.
      if (tank != null) {
         for (ModuleDef ext : TankDamage.externalModulesOnPath(tank, hitVec, dir)) {
            if (tank.damageState().destroyed(ext.index())) {
               continue;
            }

            if (data.isContactFuzed()) {
               event.setCanceled(true);
               TankDamage.markHandled(shell);
               TankDamage.Report r = TankDamage.outsideBlast(tank, level, hitVec, data, source, owner);
               HeExplosion.detonate(level, hitVec.subtract(dir.scale(0.1)), data.getFiller(), shell, owner, tank, 1.0, true);
               shell.discard();
               this.report(event, Component.literal("HIT ").withStyle(ChatFormatting.YELLOW).append(Component.literal(ext.label()).withStyle(ChatFormatting.GRAY)), r, new ReplayCtx(tank, armour, -1, hitVec, dir, null));
               return;
            }

            TankDamage.Report r = TankDamage.hitExternal(tank, level, ext, data, source, owner);
            pen -= ext.armour();
            prefix = prefix.append(Component.literal(ext.label() + " ").withStyle(ChatFormatting.GRAY));
            if (pen <= 0.0) {
               event.setCanceled(true);
               TankDamage.markHandled(shell);
               shell.discard();
               this.nonPenetrationEffects(level, hitVec, dir, data.getCalibre());
               this.report(event, Component.literal("ABSORBED ").withStyle(ChatFormatting.YELLOW).append(prefix), r, new ReplayCtx(tank, armour, -1, hitVec, dir, null));
               return;
            }

            if (!r.isEmpty()) {
               prefix = prefix.append(r.summary()).append(Component.literal(" "));
            }
         }
      }

      // 2. Which hull plate was struck, and at what angle.
      int plateIndex = this.pickPlateIndex(armour.getPlates(), target, hitVec);
      if (plateIndex < 0) {
         if (tank != null) {
            // Grazed geometry that isn't armour (fenders, stowage): no effect.
            event.setCanceled(true);
            TankDamage.markHandled(shell);
            if (data.isContactFuzed()) {
               HeExplosion.detonate(level, hitVec.subtract(dir.scale(0.1)), data.getFiller(), shell, owner, null, 1.0, true);
            }

            shell.discard();
         }

         return;
      }

      ArmorPlate plate = armour.getPlates().get(plateIndex);
      Vec3 normal = this.plateNormal(plate, target, dir);
      double cosI = Math.min(1.0, Math.max(0.0, Math.abs(dir.dot(normal))));
      double angle = Math.toDegrees(Math.acos(cosI));
      double effective = plate.getThickness() / Math.max(cosI, 0.087);
      boolean overmatched = pen >= effective * data.getRicochet().getOvermatchRatio();
      double ricochetChance = overmatched ? 0.0 : armour.getGeneralRicochetChance() * plate.getRicochetChance() * data.getRicochet().angleFactor(angle);
      HitOutcome outcome;
      if (!overmatched && ricochetChance > 0.0 && level.random.nextDouble() < ricochetChance) {
         outcome = HitOutcome.RICOCHET;
      } else if (pen >= effective) {
         outcome = HitOutcome.PENETRATION;
      } else if (data.getDefeatedByArmour() || tank != null) {
         outcome = HitOutcome.NON_PENETRATION;
      } else {
         outcome = HitOutcome.PENETRATION;
      }

      String detail = String.format(Locale.ROOT, "%s %.0fmm @%.0f° = %.0f eff · pen %.0f", plate.getName(), plate.getThickness(), angle, effective, pen);
      if (tank != null) {
         // plateNormal() faces the shooter, i.e. outward from the struck face.
         byte mark = switch (outcome) {
            case RICOCHET -> rev.rev.warsteel.damage.HitDecals.RICOCHET;
            case NON_PENETRATION -> data.isContactFuzed() ? rev.rev.warsteel.damage.HitDecals.SCORCH : rev.rev.warsteel.damage.HitDecals.NON_PENETRATION;
            default -> rev.rev.warsteel.damage.HitDecals.PENETRATION;
         };
         rev.rev.warsteel.damage.HitDecals.add(tank, plate.getTransform(), hitVec, normal, dir, mark, data.getCalibre());
      }

      switch (outcome) {
         case RICOCHET: {
            event.setCanceled(true);
            Vec3 out = this.deflect(shell, level, hitVec, dir, normal, velocity.length(), data, data.getRicochet().getSpeedRetained());
            bounceImmunity.put(shell.getId(), new Pair<>(target.getId(), now + 6L));
            this.ricochetEffects(level, hitVec, out, data.getCalibre());
            this.report(event, Component.literal("RICOCHET ").withStyle(ChatFormatting.AQUA).append(Component.literal(detail).withStyle(ChatFormatting.GRAY)), null, new ReplayCtx(tank, armour, plateIndex, hitVec, dir, out));
            break;
         }
         case NON_PENETRATION: {
            event.setCanceled(true);
            if (tank != null) {
               TankDamage.markHandled(shell);
            }

            if (data.isContactFuzed()) {
               TankDamage.Report r = tank != null ? TankDamage.outsideBlast(tank, level, hitVec, data, source, owner) : null;
               HeExplosion.detonate(level, hitVec.subtract(dir.scale(0.1)), data.getFiller(), shell, owner, target, 1.0, true);
               shell.discard();
               this.report(event, Component.literal("HIT ").withStyle(ChatFormatting.YELLOW).append(prefix).append(Component.literal(detail).withStyle(ChatFormatting.GRAY)), r, new ReplayCtx(tank, armour, plateIndex, hitVec, dir, null));
            } else {
               shell.setDamage(0.0F);
               shell.setExplosionDamage(0.0F);
               Vec3 out = this.deflect(shell, level, hitVec, dir, normal, velocity.length(), data, 0.28);
               bounceImmunity.put(shell.getId(), new Pair<>(target.getId(), now + 6L));
               this.nonPenetrationEffects(level, hitVec, dir, data.getCalibre());
               this.report(event, Component.literal("NO PENETRATION ").withStyle(ChatFormatting.YELLOW).append(prefix).append(Component.literal(detail).withStyle(ChatFormatting.GRAY)), null, new ReplayCtx(tank, armour, plateIndex, hitVec, dir, out));
            }
            break;
         }
         default: {
            this.penetrationEffects(level, hitVec, dir, data.getCalibre());
            if (tank == null) {
               // Armour data but no module model: legacy behaviour, SBW applies its damage.
               this.report(event, Component.literal("PENETRATION ").withStyle(ChatFormatting.GREEN).append(Component.literal(detail).withStyle(ChatFormatting.GRAY)), null, null);
               break;
            }

            event.setCanceled(true);
            TankDamage.markHandled(shell);
            TankDamage.Report r = TankDamage.penetrate(tank, level, armour, plateIndex, data, hitVec, dir, effective, pen - effective, source, owner);
            if (data.isContactFuzed()) {
               // Burst inside the hull: most of the blast is contained; only a muffled share
               // escapes through the hole and hatches.
               HeExplosion.detonate(level, hitVec, data.getFiller(), shell, owner, tank, 0.25, false);
            }

            shell.discard();
            this.report(
               event, Component.literal("PENETRATION ").withStyle(ChatFormatting.GREEN).append(prefix).append(Component.literal(detail).withStyle(ChatFormatting.GRAY)), r,
               new ReplayCtx(tank, armour, plateIndex, hitVec, dir, null)
            );
         }
      }
   }

   /** A WarSteel explosive round striking a person or animal: impact, then the burst. */
   private void onShellHitsSoftTarget(HitEntity event) {
      if (!(event.getProjectile() instanceof CannonShellEntity shell) || !(shell.level() instanceof ServerLevel level)) {
         return;
      }

      ShellData data = this.profileOf(shell, event.getOwner());
      if (data == null || !data.isContactFuzed() && !data.getShellClass().equals("APHE")) {
         return; // solid shot: Superb Warfare's own pass-through behaviour
      }

      event.setCanceled(true);
      Entity target = event.getTarget();
      target.invulnerableTime = 0;
      target.hurt(ModDamageTypes.causeProjectileHitDamage(level.registryAccess(), shell, event.getOwner()), (float)Math.max(20.0, data.getMass() * 4.0));
      HeExplosion.detonate(level, event.getHitVec(), data.getFiller(), shell, event.getOwner(), null, 1.0, true);
      shell.discard();
   }

   /** What the shot replay needs to know about a hit; tank null = no replay. */
   private record ReplayCtx(@Nullable WarSteelTank tank, ArmorData armour, int plate, Vec3 hit, Vec3 dir, @Nullable Vec3 out) {
   }

   /** Hit feedback for the shooter (action bar and shot replay) and, when something broke, the victim's crew. */
   private void report(HitEntity event, MutableComponent headline, @Nullable TankDamage.Report r, @Nullable ReplayCtx replay) {
      MutableComponent line = headline;
      if (r != null && !r.isEmpty()) {
         line = line.append(Component.literal("  ")).append(r.summary());
      }

      if (rev.rev.warsteel.damage.WarSteelCommands.isTestShell(event.getProjectile())) {
         org.joml.Vector4d lh = new org.joml.Matrix4d(((VehicleEntity)event.getTarget()).getTransformFromString("Vehicle")).invert()
            .transform(new org.joml.Vector4d(event.getHitVec().x, event.getHitVec().y, event.getHitVec().z, 1.0));
         RicochetHandlerKt.access$getLOG$p().info("test hit on {} at local ({}, {}, {}) shell at {}: {}", event.getTarget().getType().getDescriptionId(),
            String.format("%.2f", lh.x), String.format("%.2f", lh.y), String.format("%.2f", lh.z), event.getProjectile().position(), line.getString());
      } else {
         RicochetHandlerKt.access$getLOG$p().debug("armour hit: {}", line.getString());
      }
      if (event.getOwner() instanceof ServerPlayer shooter) {
         MinecraftUtil.sendPacketTo(shooter, new ClientIndicatorMessage(r != null && r.knockedOut() ? 2 : 3, 10));
         shooter.displayClientMessage(line, true);
         if (replay != null && replay.tank() != null) {
            rev.rev.warsteel.network.ShotReplays.send(shooter, replay.tank(), replay.armour(), replay.plate(), replay.hit(), replay.dir(), replay.out(), headline, r);
         }
      }

      if (r != null && !r.isEmpty() && event.getTarget() instanceof WarSteelTank tank) {
         for (Entity e : tank.getPassengers()) {
            if (e instanceof ServerPlayer p) {
               p.displayClientMessage(Component.literal("HIT: ").withStyle(ChatFormatting.RED).append(r.summary()), true);
            }
         }
      }
   }

   private final Vec3 deflect(
      CannonShellEntity shell, ServerLevel level, Vec3 hitVec, Vec3 dir, Vec3 normal, double speed, ShellData data, double speedRetained
   ) {
      Vec3 out = this.reflect(dir, normal).scale(speed * speedRetained);
      Intrinsics.checkNotNull(out);
      out = this.scatter(out, data.getRicochet().getScatterDegrees(), level.random.nextDouble(), level.random.nextDouble());
      shell.setDeltaMovement(out);
      Vec3 push = dir.dot(normal) > 0.0 ? normal.scale(-1.0) : normal;
      Vec3 p = hitVec.add(push.scale(0.6));
      shell.setPos(p.x, p.y, p.z);
      return out;
   }

   private final void ricochetEffects(ServerLevel level, Vec3 at, Vec3 outVec, int calibre) {
      Vec3 dir = outVec.lengthSqr() > 1.0E-9 ? outVec.normalize() : new Vec3(0.0, 1.0, 0.0);
      int sparks = this.sparkCount(calibre);

      for (int i = 0; i < sparks; i++) {
         Vec3 jitter = new Vec3(level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5).scale(0.45);
         Vec3 v = dir.add(jitter).normalize();
         Object var10001 = ModParticleTypes.FIRE_STAR.get();
         Intrinsics.checkNotNullExpressionValue(var10001, "get(...)");
         ParticleTool.sendParticle(
            level,
            (ParticleOptions)var10001,
            at.x,
            at.y,
            at.z,
            0,
            v.x,
            v.y,
            v.z,
            0.8 + level.random.nextDouble() * 0.6,
            false
         );
      }

      int i = 0;

      for (int var13 = sparks / 2; i < var13; i++) {
         Vec3 jitter = new Vec3(level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5).scale(0.22);
         Vec3 v = dir.add(jitter).normalize();
         Object var16 = ModParticleTypes.WHITE_STAR.get();
         Intrinsics.checkNotNullExpressionValue(var16, "get(...)");
         ParticleTool.sendParticle(
            level,
            (ParticleOptions)var16,
            at.x,
            at.y,
            at.z,
            0,
            v.x,
            v.y,
            v.z,
            1.3 + level.random.nextDouble() * 0.7,
            false
         );
      }

      float flareSize = 0.5F + calibre / 130.0F;
      ParticleTool.sendParticle(
         level,
         (ParticleOptions)(new CustomFlareOption(1.0F, 0.86F, 0.55F, 6, 0.72F, 1, 0.35F, flareSize)),
         at.x,
         at.y,
         at.z,
         1,
         0.0,
         0.0,
         0.0,
         0.0,
         false
      );
      SimpleParticleType var17 = ParticleTypes.FLASH;
      Intrinsics.checkNotNullExpressionValue(ParticleTypes.FLASH, "FLASH");
      ParticleTool.sendParticle(level, (ParticleOptions)var17, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0, false);
      var17 = ParticleTypes.LAVA;
      Intrinsics.checkNotNullExpressionValue(ParticleTypes.LAVA, "LAVA");
      ParticleTool.sendParticle(level, (ParticleOptions)var17, at.x, at.y, at.z, 2, 0.1, 0.1, 0.1, 0.0, false);
      var17 = ParticleTypes.SMOKE;
      Intrinsics.checkNotNullExpressionValue(ParticleTypes.SMOKE, "SMOKE");
      ParticleTool.sendParticle(level, (ParticleOptions)var17, at.x, at.y, at.z, 4, 0.08, 0.08, 0.08, 0.02, false);
      var17 = (SimpleParticleType)ModParticleTypes.RISING_SMOKE.get();
      Intrinsics.checkNotNullExpressionValue(var17, "get(...)");
      ParticleTool.sendParticle(level, (ParticleOptions)var17, at.x, at.y + 0.1, at.z, 3, 0.1, 0.05, 0.1, 0.02, false);
      float pitch = 1.06F - calibre / 900.0F + level.random.nextFloat() * 0.16F;
      Object var10003 = ModSounds.ARMOR_RICOCHET.get();
      Intrinsics.checkNotNullExpressionValue(var10003, "get(...)");
      this.playImpactSound(level, at, (SoundEvent)var10003, calibre, pitch);
   }

   private final void nonPenetrationEffects(ServerLevel level, Vec3 at, Vec3 incoming, int calibre) {
      Vec3 back = incoming.scale(-1.0).normalize();
      int bits = this.sparkCount(calibre) * 3 / 4;

      for (int i = 0; i < bits; i++) {
         Vec3 jitter = new Vec3(level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5).scale(0.9);
         Vec3 v = back.add(jitter).normalize();
         Object var10001 = ModParticleTypes.FIRE_STAR.get();
         Intrinsics.checkNotNullExpressionValue(var10001, "get(...)");
         ParticleTool.sendParticle(
            level,
            (ParticleOptions)var10001,
            at.x,
            at.y,
            at.z,
            0,
            v.x,
            v.y,
            v.z,
            0.3 + level.random.nextDouble() * 0.25,
            false
         );
      }

      ParticleTool.sendParticle(
         level,
         (ParticleOptions)(new CustomFlareOption(1.0F, 0.45F, 0.18F, 10, 0.86F, 1, 0.15F, 0.4F + calibre / 200.0F)),
         at.x,
         at.y,
         at.z,
         1,
         0.0,
         0.0,
         0.0,
         0.0,
         false
      );
      SimpleParticleType var11 = ParticleTypes.LARGE_SMOKE;
      Intrinsics.checkNotNullExpressionValue(ParticleTypes.LARGE_SMOKE, "LARGE_SMOKE");
      ParticleTool.sendParticle(level, (ParticleOptions)var11, at.x, at.y, at.z, 8, 0.16, 0.16, 0.16, 0.02, false);
      var11 = (SimpleParticleType)ModParticleTypes.RISING_SMOKE.get();
      Intrinsics.checkNotNullExpressionValue(var11, "get(...)");
      ParticleTool.sendParticle(level, (ParticleOptions)var11, at.x, at.y + 0.1, at.z, 5, 0.14, 0.06, 0.14, 0.03, false);
      var11 = ParticleTypes.LAVA;
      Intrinsics.checkNotNullExpressionValue(ParticleTypes.LAVA, "LAVA");
      ParticleTool.sendParticle(level, (ParticleOptions)var11, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0, false);
      float clangPitch = 0.78F + level.random.nextFloat() * 0.08F;
      Object var10003 = ModSounds.ARMOR_RICOCHET.get();
      Intrinsics.checkNotNullExpressionValue(var10003, "get(...)");
      this.playImpactSound(level, at, (SoundEvent)var10003, calibre, clangPitch);
   }

   private final void penetrationEffects(ServerLevel level, Vec3 at, Vec3 incoming, int calibre) {
      Vec3 dir = incoming.lengthSqr() > 1.0E-9 ? incoming.normalize() : new Vec3(0.0, -1.0, 0.0);
      int i = 0;

      for (int var7 = this.sparkCount(calibre) / 2; i < var7; i++) {
         Vec3 jitter = new Vec3(level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5).scale(0.3);
         Vec3 v = dir.add(jitter).normalize();
         Object var10001 = ModParticleTypes.WHITE_STAR.get();
         Intrinsics.checkNotNullExpressionValue(var10001, "get(...)");
         ParticleTool.sendParticle(
            level,
            (ParticleOptions)var10001,
            at.x,
            at.y,
            at.z,
            0,
            v.x,
            v.y,
            v.z,
            1.0 + level.random.nextDouble() * 0.6,
            false
         );
      }

      ParticleTool.sendParticle(
         level,
         (ParticleOptions)(new CustomFlareOption(1.0F, 0.95F, 0.8F, 5, 0.7F, 1, 0.2F, 0.45F + calibre / 200.0F)),
         at.x,
         at.y,
         at.z,
         1,
         0.0,
         0.0,
         0.0,
         0.0,
         false
      );
      SimpleParticleType var10 = ParticleTypes.FLASH;
      Intrinsics.checkNotNullExpressionValue(ParticleTypes.FLASH, "FLASH");
      ParticleTool.sendParticle(level, (ParticleOptions)var10, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0, false);
      var10 = ParticleTypes.LARGE_SMOKE;
      Intrinsics.checkNotNullExpressionValue(ParticleTypes.LARGE_SMOKE, "LARGE_SMOKE");
      ParticleTool.sendParticle(level, (ParticleOptions)var10, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.03, false);
   }

   private final void playImpactSound(ServerLevel level, Vec3 at, SoundEvent sound, int calibre, float pitch) {
      float volume = RicochetHandlerKt.access$volumeForRange(36.0 + calibre / 5.0);
      level.playSound(null, at.x, at.y, at.z, sound, RicochetHandlerKt.access$getIMPACT_SOUND_CATEGORY$p(), volume, pitch);
   }

   private final int sparkCount(int calibre) {
      return RangesKt.coerceIn(8 + calibre / 10, 8, 24);
   }

   private final Vec3 reflect(Vec3 d, Vec3 n) {
      Vec3 var10000 = d.subtract(n.scale(2.0 * d.dot(n))).normalize();
      Intrinsics.checkNotNullExpressionValue(var10000, "normalize(...)");
      return var10000;
   }

   private final Vec3 scatter(Vec3 v, double deg, double r1, double r2) {
      if (deg <= 0.0) {
         return v;
      } else {
         double len = v.length();
         double a = Math.toRadians(deg) * (r1 * 2 - 1);
         double b = Math.toRadians(deg) * (r2 * 2 - 1);
         Vec3 d = v.normalize();
         Vec3 up = Math.abs(d.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
         Vec3 t1 = d.cross(up).normalize();
         Vec3 t2 = d.cross(t1).normalize();
         Vec3 var10000 = d.add(t1.scale(Math.sin(a))).add(t2.scale(Math.sin(b))).normalize().scale(len);
         Intrinsics.checkNotNullExpressionValue(var10000, "scale(...)");
         return var10000;
      }
   }

   private final double flightDistance(Entity owner, CannonShellEntity shell, Vec3 hitVec) {
      if (owner != null) {
         Vec3 var10000 = owner.position();
         if (var10000 != null) {
            return var10000.distanceTo(hitVec);
         }
      }

      return shell.position().distanceTo(hitVec);
   }

   private final String firingVehicleId(Entity owner) {
      Entity var3 = owner != null ? owner.getVehicle() : null;
      VehicleEntity var10000 = var3 instanceof VehicleEntity ? (VehicleEntity)var3 : null;
      if ((var3 instanceof VehicleEntity ? (VehicleEntity)var3 : null) == null) {
         return null;
      } else {
         VehicleEntity vehicle = var10000;
         ResourceLocation var4 = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
         return var4 != null ? var4.toString() : null;
      }
   }

   private final Vec3 plateNormal(ArmorPlate plate, VehicleEntity v, Vec3 incoming) {
      Quaterniond rot = v.getRotationFromString(plate.getTransform());
      Vector3d n = rot.transform(new Vector3d((Vector3dc)plate.getLocalNormal()));
      Vec3 normal = new Vec3(n.x, n.y, n.z);
      if (normal.lengthSqr() < 1.0E-9) {
         return new Vec3(0.0, 1.0, 0.0);
      } else {
         normal = normal.normalize();
         if (normal.dot(incoming) > 0.0) {
            normal = normal.scale(-1.0);
         }

         return normal;
      }
   }

   private int pickPlateIndex(List<ArmorPlate> plates, VehicleEntity v, Vec3 hit) {
      int best = -1;
      double bestDistSqr = Double.MAX_VALUE;

      for (int i = 0; i < plates.size(); i++) {
         double d = this.distanceToPlateSqr(plates.get(i), v, hit);
         if (d < bestDistSqr) {
            bestDistSqr = d;
            best = i;
         }
      }

      return bestDistSqr > 1.5625 ? -1 : best;
   }

   private final double distanceToPlateSqr(ArmorPlate plate, VehicleEntity v, Vec3 hit) {
      Matrix4d transform = v.getTransformFromString(plate.getTransform());
      Vector4d centre = v.transformPosition(transform, plate.getPosition()[0], plate.getPosition()[1], plate.getPosition()[2]);
      Vector3d rel = new Vector3d(hit.x - centre.x, hit.y - centre.y, hit.z - centre.z);
      new Quaterniond((Quaterniondc)v.getRotationFromString(plate.getTransform())).conjugate().transform(rel);
      double dx = Math.max(0.0, Math.abs(rel.x) - plate.getSize()[0]);
      double dy = Math.max(0.0, Math.abs(rel.y) - plate.getSize()[1]);
      double dz = Math.max(0.0, Math.abs(rel.z) - plate.getSize()[2]);
      return dx * dx + dy * dy + dz * dz;
   }

   private final void debug(HitEntity event, String msg) {
      RicochetHandlerKt.access$getLOG$p().debug("armour hit: {}", msg);
   }

   // $VF: synthetic class
   public class WhenMappings {
      // $VF: synthetic field
      public static final int[] $EnumSwitchMapping$0;

      static {
         int[] var0 = new int[HitOutcome.values().length];

         try {
            var0[HitOutcome.PENETRATION.ordinal()] = 1;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[HitOutcome.RICOCHET.ordinal()] = 2;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[HitOutcome.NON_PENETRATION.ordinal()] = 3;
         } catch (NoSuchFieldError var2) {
         }

         $EnumSwitchMapping$0 = var0;
      }
   }
}
