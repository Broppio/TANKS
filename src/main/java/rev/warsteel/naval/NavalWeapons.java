package rev.rev.warsteel.naval;

import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import rev.rev.warsteel.entity.vehicle.T22Entity;
import rev.rev.warsteel.init.ModEntities;

/**
 * The T-22's main battery and torpedoes.
 *
 * <p>Main battery: the captain aims mount A (Superb Warfare's turret) and fires it; every other
 * mount whose arc covers the same bearing fires with it, a salvo from up to four guns. Mounts C
 * and D sit aft facing astern, so they bear on targets abaft the beam; B covers the forward arc.
 *
 * <p>Torpedoes: the torpedo officer's weapon launches a spread of three G7a from the mounts in
 * turn, along the bearing he's looking at.
 */
public final class NavalWeapons {
   public static final NavalWeapons INSTANCE = new NavalWeapons();
   public static final String SALVO_TAG = "warsteel_salvo";
   /** Torpedo officer's seat and the marker damage his weapon fires with (t-22.json). */
   public static final int TORPEDO_SEAT = 6;
   public static final float TORPEDO_MARKER = 4242.0F;

   /** Mount: barrel pivot (Vehicle frame), muzzle length, rest bearing (0 = ahead, 180 = astern), arc either side. */
   public record Mount(String bone, double x, double y, double z, double muzzle, float rest, float arc) {
      public boolean bears(float relBearing) {
         return Math.abs(Mth.wrapDegrees(relBearing - this.rest)) <= this.arc;
      }
   }

   public static final Mount[] MAIN = {
      new Mount("b", 0.0, 8.644, -1.747, 3.42, 0.0F, 135.0F),
      new Mount("c", 0.0, 8.67, -24.396, 3.42, 180.0F, 140.0F),
      new Mount("d", 0.0, 7.453, -33.747, 3.42, 180.0F, 150.0F),
   };
   public static final double[][] TORPEDO_MOUNTS = {{0.0, 6.58, 7.42}, {0.0, 6.43, -16.24}};

   private NavalWeapons() {
   }

   @SubscribeEvent
   public void onJoin(EntityJoinLevelEvent event) {
      if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof CannonShellEntity shell)
         || shell.getTags().contains(SALVO_TAG)
         || !(shell.getOwner() instanceof LivingEntity gunner) || !(gunner.getVehicle() instanceof T22Entity ship)) {
         return;
      }

      int seat = ship.getSeatIndex(gunner);
      if (seat == TORPEDO_SEAT || shell.getDamage() == TORPEDO_MARKER) {
         event.setCanceled(true);
         this.launchTorpedoes(level, ship, gunner);
      } else if (seat == 0) {
         this.salvo(level, ship, shell);
      }
   }

   private void salvo(ServerLevel level, T22Entity ship, CannonShellEntity lead) {
      Vec3 v = lead.getDeltaMovement();
      if (v.lengthSqr() < 1.0E-6) {
         return;
      }

      Vec3 dir = v.normalize();
      float bearing = Mth.wrapDegrees((float)(Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG) - ship.getYRot());
      CompoundTag template = new CompoundTag();
      lead.saveWithoutId(template);
      template.remove("UUID");
      int fired = 0;
      for (Mount m : MAIN) {
         if (!m.bears(bearing)) {
            continue;
         }

         Entity copy = lead.getType().create(level);
         if (!(copy instanceof CannonShellEntity shell)) {
            continue;
         }

         shell.load(template);
         shell.addTag(SALVO_TAG);
         Vec3 muzzle = ship.toWorld(m.x(), m.y(), m.z()).add(dir.scale(m.muzzle()));
         shell.setPos(muzzle);
         // A little dispersion between the guns of a salvo.
         Vec3 jitter = new Vec3(level.random.nextGaussian(), level.random.nextGaussian(), level.random.nextGaussian()).scale(0.004);
         shell.setDeltaMovement(dir.add(jitter).normalize().scale(v.length()));
         shell.setOwner(lead.getOwner());
         level.addFreshEntity(shell);
         fired++;
      }

      org.slf4j.LoggerFactory.getLogger("warsteel/naval").debug("salvo at bearing {}: mount A + {} more", (int)bearing, fired);
   }

   private void launchTorpedoes(ServerLevel level, T22Entity ship, LivingEntity officer) {
      int mount = ship.nextTorpedoMount();
      double[] p = TORPEDO_MOUNTS[mount];
      float heading = officer.getYHeadRot();
      Vec3 base = ship.toWorld(p[0], p[1], p[2]);
      Vec3 dir = Vec3.directionFromRotation(0.0F, heading);
      for (int i = -1; i <= 1; i++) {
         TorpedoEntity t = ModEntities.TORPEDO.get().create(level);
         if (t == null) {
            continue;
         }

         // Fired over the side, a few degrees apart.
         Vec3 at = base.add(dir.scale(4.5 + i * 0.6)).add(0, -0.5, 0);
         t.launch(officer, ship, at, heading + i * 3.0F);
         level.addFreshEntity(t);
      }
   }
}
