package rev.rev.warsteel.naval;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.jetbrains.annotations.Nullable;
import rev.rev.warsteel.damage.TankDamage;
import rev.rev.warsteel.damage.WarSteelTank;

/**
 * Any explosion near a ship - vanilla TNT, creepers, Superb Warfare's rockets and bombs, other
 * mods' weapons - holes the hull and wrecks the parts close to it. A ship is far longer than its
 * entity box, so explosions are measured against its hull boxes, not left to vanilla's damage.
 * WarSteel's own HE shells go through {@link #blast} directly from HeExplosion.
 */
public final class ShipExplosions {
   public static final ShipExplosions INSTANCE = new ShipExplosions();
   /** Set while HeExplosion makes its crater with a vanilla explosion, so it isn't counted twice. */
   public static final ThreadLocal<Boolean> CRATER = ThreadLocal.withInitial(() -> Boolean.FALSE);

   private ShipExplosions() {
   }

   @SubscribeEvent
   public void onDetonate(ExplosionEvent.Detonate event) {
      if (CRATER.get() || !(event.getLevel() instanceof ServerLevel level)) {
         return;
      }

      Explosion e = event.getExplosion();
      double r = e.radius();
      if (r <= 0.1) {
         return;
      }

      // TNT-equivalent from the blast radius (vanilla TNT, radius 4: about 4.6 kg).
      double kg = Math.min(500.0, Math.pow(r / 2.4, 3.0));
      blast(level, e.center(), kg, e.getIndirectSourceEntity(), null, null, true);
   }

   /**
    * @param modules whether to damage the ships' parts too (false when the caller already did)
    * @param skip    a ship whose parts were already handled (an internal burst), or null
    */
   public static void blast(ServerLevel level, Vec3 at, double kg, @Nullable Entity attacker, @Nullable DamageSource source, @Nullable Entity skip, boolean modules) {
      double reach = 0.9 * Math.cbrt(kg) + 0.6 + 60.0;
      for (Entity e : level.getEntities((Entity)null, new AABB(at, at).inflate(reach), x -> x instanceof NavalHull && x.isAlive())) {
         ((NavalHull)e).hullBlast(at, kg, attacker);
         if (modules && e != skip && e instanceof WarSteelTank ship && ship.usesModuleDamage()) {
            DamageSource src = source != null ? source : level.damageSources().explosion(null, attacker);
            TankDamage.blastFromOutside(ship, level, at, kg * 1000.0, src, attacker);
         }
      }
   }

   static boolean isShip(Entity e) {
      return e instanceof VehicleEntity && e instanceof NavalHull;
   }
}
