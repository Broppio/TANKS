package rev.rev.warsteel.combat;

import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import rev.rev.warsteel.damage.WarSteelCommands;

/**
 * Time and proximity fuzing for anti-aircraft rounds (shell class FLAK). A round bursts when it
 * passes within ProximityRange of an aircraft - standing in for the predictor setting the time
 * fuze to the target's range - or when its longest fuze setting (FuzeTicks) runs out.
 */
public final class FlakFuze {
   public static final FlakFuze INSTANCE = new FlakFuze();
   private final Map<CannonShellEntity, Armed> live = new WeakHashMap<>();

   private record Armed(ShellData data, long firedAt) {
   }

   private FlakFuze() {
   }

   @SubscribeEvent
   public void onJoin(EntityJoinLevelEvent event) {
      if (event.getLevel().isClientSide() || !(event.getEntity() instanceof CannonShellEntity shell)) {
         return;
      }

      ShellData data = WarSteelCommands.forcedProfile(shell);
      if (data == null) {
         Entity owner = shell.getOwner();
         Entity vehicle = owner != null ? owner.getVehicle() : null;
         String id = vehicle != null ? BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).toString() : null;
         data = ShellRegistry.INSTANCE.resolve(id, shell.getDamage(), shell.canPassThroughFluid());
      }

      if (data != null && data.isFlak()) {
         this.live.put(shell, new Armed(data, event.getLevel().getGameTime()));
      }
   }

   @SubscribeEvent
   public void onTick(ServerTickEvent.Post event) {
      if (this.live.isEmpty()) {
         return;
      }

      Iterator<Map.Entry<CannonShellEntity, Armed>> it = this.live.entrySet().iterator();

      while (it.hasNext()) {
         Map.Entry<CannonShellEntity, Armed> e = it.next();
         CannonShellEntity shell = e.getKey();
         if (shell == null || shell.isRemoved() || !(shell.level() instanceof ServerLevel level)) {
            it.remove();
            continue;
         }

         ShellData data = e.getValue().data();
         long age = level.getGameTime() - e.getValue().firedAt();
         Entity target = age >= 2 ? this.nearestAircraft(level, shell, data.getProximityRange()) : null;
         if (target != null || age >= Math.max(10, data.getFuzeTicks())) {
            it.remove();
            Vec3 at = shell.position();
            if (target != null) {
               // Burst at the closest point of the shell's path this tick, not where it ended up.
               Vec3 v = shell.getDeltaMovement();
               Vec3 start = at.subtract(v);
               Vec3 to = target.getBoundingBox().getCenter().subtract(start);
               double k = v.lengthSqr() > 1.0E-6 ? Math.max(0.0, Math.min(1.0, to.dot(v) / v.lengthSqr())) : 1.0;
               at = start.add(v.scale(k));
            }

            if (WarSteelCommands.isTestShell(shell)) {
               org.slf4j.LoggerFactory.getLogger("warsteel/flak").info("flak burst after {} ticks: {}", age, target != null ? "proximity to " + target.getType().getDescriptionId() : "time fuze");
            }

            HeExplosion.detonate(level, at, data.getFiller(), shell, shell.getOwner(), null, 1.0, false);
            shell.discard();
         }
      }
   }

   @Nullable
   private Entity nearestAircraft(ServerLevel level, CannonShellEntity shell, double range) {
      Vec3 v = shell.getDeltaMovement();
      // Search around the whole path flown this tick (shells move 40 blocks a tick).
      AABB swept = shell.getBoundingBox().expandTowards(v.scale(-1.0)).inflate(range);
      Entity owner = shell.getOwner();
      Entity ownVehicle = owner != null ? owner.getVehicle() : null;
      Entity best = null;
      double bestD = Double.MAX_VALUE;

      for (Entity e : level.getEntities(shell, swept, x -> x != owner && x != ownVehicle && isAircraft(x))) {
         double d = distanceToSegment(e.getBoundingBox().getCenter(), shell.position().subtract(v), shell.position());
         double reach = range + Math.max(e.getBbWidth(), e.getBbHeight()) * 0.5;
         if (d < reach && d < bestD) {
            best = e;
            bestD = d;
         }
      }

      return best;
   }

   public static boolean isAircraft(Entity e) {
      if (e instanceof VehicleEntity v) {
         VehicleType t = v.getVehicleType();
         return t == VehicleType.AIRPLANE || t == VehicleType.HELICOPTER || t == VehicleType.AIRSHIP || !v.onGround() && v.getY() - v.level().getHeight(
            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, v.getBlockX(), v.getBlockZ()) > 6;
      }

      return e instanceof FlyingMob || e instanceof EnderDragon || e instanceof Player p && p.isFallFlying();
   }

   private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
      Vec3 ab = b.subtract(a);
      double len2 = ab.lengthSqr();
      double t = len2 < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, p.subtract(a).dot(ab) / len2));
      return p.distanceTo(a.add(ab.scale(t)));
   }
}
