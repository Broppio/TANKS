package rev.rev.warsteel.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import rev.rev.warsteel.naval.NavalWeapons;

/**
 * T-22: Superb Warfare turns mount A for the captain; this turns everything else.
 * <ul>
 * <li>Mounts B-D follow A's bearing and elevation when they bear (C and D rest facing astern).</li>
 * <li>The AA mounts follow their gunners; unmanned ones rest.</li>
 * <li>The torpedo mounts train to the torpedo officer's bearing, out over the beam.</li>
 * <li>Propellers turn with the engine, rudders with the helm, the radar sweeps.</li>
 * </ul>
 * Every mount uses Superb Warfare's own convention (bone yaw = bearing - rest bearing; elevation
 * about the axis rest-direction x up), so they agree with mount A.
 */
public final class T22Renderer extends WarSteelTankRenderer {
   /** Gunner seat, bone, rest bearing (degrees; -90 = to port), rest direction in model space. */
   private record Aa(int seat, String bone, float rest, float rx, float ry, float rz) {
   }

   private static final Aa[] AA = {
      new Aa(1, "flakz_p", -90.0F, 1, 0, 0),
      new Aa(2, "flakz_s", 90.0F, -1, 0, 0),
      new Aa(3, "flakv", 180.0F, 0, 0, 1),
      new Aa(4, "flak_3", -90.0F, 1, 0, 0),
      new Aa(5, "flak_2", 90.0F, -1, 0, 0),
      new Aa(-1, "flak_1", 90.0F, -1, 0, 0),
      new Aa(-1, "flak_4", -90.0F, 1, 0, 0),
   };

   public T22Renderer(Context manager) {
      super(manager);
   }

   /** Trim and list from flooding, about the waterline amidships, on top of Superb Warfare's own pitch and roll. */
   @Override
   public void rotateVehicleAxis(VehicleEntity entity, PoseStack poseStack, float entityYaw, float partialTicks) {
      super.rotateVehicleAxis(entity, poseStack, entityYaw, partialTicks);
      if (entity instanceof rev.rev.warsteel.entity.vehicle.T22Entity ship) {
         float pivot = (float)entity.getRotateOffsetHeight();
         poseStack.rotateAround(com.mojang.math.Axis.XP.rotationDegrees(-ship.visualTrim(partialTicks)), 0.0F, pivot, 0.0F);
         poseStack.rotateAround(com.mojang.math.Axis.ZP.rotationDegrees(-ship.visualList(partialTicks)), 0.0F, pivot, 0.0F);
      }
   }

   @Override
   public void transformCustomModelPart(VehicleEntity entity, VehicleModelInstance instance, PoseStack poseStack, float yaw, float partialTicks) {
      super.transformCustomModelPart(entity, instance, poseStack, yaw, partialTicks);
      float bearing = this.getTurretYRot();
      float pitch = this.getTurretXRot();

      // Main battery: B, C, D slaved to A.
      for (NavalWeapons.Mount m : NavalWeapons.MAIN) {
         BoneState turret = instance.getBone("turret_" + m.bone());
         BoneState barrel = instance.getBone("barrel_" + m.bone());
         if (turret == null || barrel == null) {
            continue;
         }

         boolean bears = m.bears(bearing);
         float yawDeg = bears ? Mth.wrapDegrees(bearing - m.rest()) : 0.0F;
         turret.rotation.rotationY(yawDeg * Mth.DEG_TO_RAD);
         float elev = bears ? Mth.clamp(-pitch, -10.0F, 40.0F) : 0.0F;
         // Aft-facing barrels elevate about -x.
         barrel.rotation.rotationX((m.rest() == 0.0F ? elev : -elev) * Mth.DEG_TO_RAD);
      }

      // AA guns follow their gunners.
      for (Aa a : AA) {
         BoneState mount = instance.getBone(a.bone);
         BoneState gun = instance.getBone(a.bone + "_gun");
         if (mount == null || gun == null) {
            continue;
         }

         Entity gunner = a.seat >= 0 ? entity.getNthEntity(a.seat) : null;
         float b = a.rest;
         float e = 0.0F;
         if (gunner != null) {
            b = Mth.wrapDegrees(Mth.rotLerp(partialTicks, gunner.yRotO, gunner.getYRot()) - Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot()));
            e = Mth.clamp(-Mth.lerp(partialTicks, gunner.xRotO, gunner.getXRot()), -5.0F, 85.0F);
         }

         mount.rotation.rotationY(Mth.wrapDegrees(b - a.rest) * Mth.DEG_TO_RAD);
         // Elevation about (rest direction x up) = (-rz, 0, rx).
         gun.rotation.rotationAxis(e * Mth.DEG_TO_RAD, -a.rz, 0.0F, a.rx);
      }

      // Torpedo mounts train out over the beam towards the torpedo officer's bearing.
      Entity officer = entity.getNthEntity(NavalWeapons.TORPEDO_SEAT);
      float torp = 90.0F;
      if (officer != null) {
         torp = Mth.wrapDegrees(officer.getYHeadRot() - entity.getYRot());
         float a = Math.abs(torp);
         torp = Math.copySign(Mth.clamp(a, 35.0F, 145.0F), torp == 0 ? 1 : torp);
      }

      for (String t : new String[]{"torp_1", "torp_2"}) {
         BoneState b = instance.getBone(t);
         if (b != null) {
            b.rotation.rotationY(torp * Mth.DEG_TO_RAD);
         }
      }

      float prop = Mth.lerp(partialTicks, entity.getPropellerRotO(), entity.getPropellerRot());
      BoneState pl = instance.getBone("prop_l");
      BoneState pr = instance.getBone("prop_r");
      if (pl != null) {
         pl.rotation.rotationZ(prop);
      }
      if (pr != null) {
         pr.rotation.rotationZ(-prop);
      }

      float rudder = Mth.lerp(partialTicks, entity.getRudderRotO(), entity.getRudderRot());
      for (String r : new String[]{"rudder_l", "rudder_r"}) {
         BoneState b = instance.getBone(r);
         if (b != null) {
            b.rotation.rotationY(rudder * 0.6F);
         }
      }

      BoneState radar = instance.getBone("radar");
      if (radar != null && !entity.isWreck()) {
         radar.rotation.rotationY((entity.tickCount + partialTicks) * 0.04F);
      }
   }
}
