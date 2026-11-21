package rev.rev.warsteel.event;

import com.atsuishio.superbwarfare.api.event.ShootEvent.Post;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import java.util.Set;
import kotlin.Lazy;
import kotlin.LazyKt;
import kotlin.collections.SetsKt;
import kotlin.jvm.internal.Intrinsics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import rev.rev.warsteel.init.ModEntities;

public final class MuzzleParticleHandler {
   @NotNull
   public static final MuzzleParticleHandler INSTANCE = new MuzzleParticleHandler();
   @NotNull
   private static final Lazy TANKS$delegate = LazyKt.lazy(MuzzleParticleHandler::TANKS_delegate$lambda$0);
   private static final double CANNON_DAMAGE_THRESHOLD = 100.0;

   private MuzzleParticleHandler() {
   }

   private final Set<EntityType<?>> getTANKS() {
      Lazy var1 = TANKS$delegate;
      return (Set<EntityType<?>>)var1.getValue();
   }

   @SubscribeEvent
   public final void onVehicleShoot(@NotNull Post event) {
      Entity var3 = event.getParameters().ammoSupplier;
      VehicleEntity var10000 = var3 instanceof VehicleEntity ? (VehicleEntity)var3 : null;
      if ((var3 instanceof VehicleEntity ? (VehicleEntity)var3 : null) != null) {
         VehicleEntity vehicle = var10000;
         if (this.getTANKS().contains(vehicle.getType())) {
            if (!(((Number)event.getData().get(GunProp.DAMAGE)).doubleValue() < 100.0)) {
               ParticleTool.spawnBigCannonMuzzleParticles(
                  event.getParameters().shootDirection, event.getParameters().shootPosition, event.getLevel(), (Entity)vehicle
               );
            }
         }
      }
   }

   private static final Set TANKS_delegate$lambda$0() {
      EntityType[] var0 = new EntityType[]{
         (EntityType)ModEntities.TigerH1.get(),
         (EntityType)ModEntities.KV_1.get(),
         (EntityType)ModEntities.KV_2.get(),
         (EntityType)ModEntities.M4A3_SHERMAN.get(),
         (EntityType)ModEntities.T_26.get(),
         (EntityType)ModEntities.FLAK_88.get(),
         (EntityType)ModEntities.PAK_38.get()
      };
      return SetsKt.setOf(var0);
   }

   /**
    * Superb Warfare doesn't raise ShootEvent for vehicle weapons, so the muzzle blast is made when
    * a main-gun shell appears: any cannon shell whose owner is crewing a WarSteel vehicle.
    */
   @SubscribeEvent
   public final void onShellSpawn(@NotNull net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
      if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
         || !(event.getEntity() instanceof com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity shell)
         || shell instanceof rev.rev.warsteel.entity.projectile.PanzerfaustWarheadEntity
         || !(shell.getOwner() instanceof net.minecraft.world.entity.LivingEntity gunner)
         || !(gunner.getVehicle() instanceof rev.rev.warsteel.damage.WarSteelTank vehicle)
         || shell.getDamage() < CANNON_DAMAGE_THRESHOLD) {
         return;
      }

      net.minecraft.world.phys.Vec3 dir = shell.getDeltaMovement().lengthSqr() > 1.0E-6 ? shell.getDeltaMovement().normalize() : vehicle.getShootVec(gunner, 1.0F);
      ParticleTool.spawnBigCannonMuzzleParticles(dir, shell.position(), level, (Entity)vehicle);
   }
}
