package rev.rev.warsteel.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.BasicVehicleRenderer;
import com.atsuishio.superbwarfare.entity.vehicle.VehicleModelEntry;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneTreeInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public abstract class WarSteelTankRenderer extends BasicVehicleRenderer {
   @NotNull
   public static final WarSteelTankRenderer.Companion Companion = new WarSteelTankRenderer.Companion(null);
   @NotNull
   private static final List<String> SMOKE_BONES;
   private static final int SMOKE_INTERVAL = 6;

   public WarSteelTankRenderer(@NotNull Context manager) {
      super(manager);
   }

   public void render(
      @NotNull VehicleEntity entity, float yaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight
   ) {
      if (entity.isInvisible()) {
         // Hidden by /warsteel xray so the module outlines inside are visible.
         return;
      }

      RENDERING_WARSTEEL.set(Boolean.TRUE);
      try {
         super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
      } finally {
         RENDERING_WARSTEEL.set(Boolean.FALSE);
      }

      if (entity instanceof rev.rev.warsteel.damage.WarSteelTank tank) {
         renderDecals(tank, partialTick, poseStack, buffer, packedLight);
      }
      this.emitExhaustSmoke(entity, poseStack, yaw, partialTick);
   }

   /** Set while a WarSteel vehicle is being drawn (read by WreckTextureMixin). */
   public static final ThreadLocal<Boolean> RENDERING_WARSTEEL = ThreadLocal.withInitial(() -> Boolean.FALSE);

   private static final net.minecraft.resources.ResourceLocation DECALS =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("warsteel", "textures/misc/hit_decals.png");

   /** Hit marks, placed in the frame of the plate that was struck so turret marks turn with it. */
   private static void renderDecals(
      rev.rev.warsteel.damage.WarSteelTank tank, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight
   ) {
      java.util.List<rev.rev.warsteel.damage.HitDecals.Decal> decals = tank.decals();
      if (decals.isEmpty()) {
         return;
      }

      double ox = net.minecraft.util.Mth.lerp(partialTick, tank.xo, tank.getX());
      double oy = net.minecraft.util.Mth.lerp(partialTick, tank.yo, tank.getY());
      double oz = net.minecraft.util.Mth.lerp(partialTick, tank.zo, tank.getZ());
      com.mojang.blaze3d.vertex.VertexConsumer vc = buffer.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(DECALS));
      PoseStack.Pose pose = poseStack.last();
      java.util.Map<String, org.joml.Matrix4d> frames = new java.util.HashMap<>();
      java.util.Map<String, org.joml.Quaterniond> rots = new java.util.HashMap<>();

      for (rev.rev.warsteel.damage.HitDecals.Decal d : decals) {
         org.joml.Matrix4d m = frames.computeIfAbsent(d.frameName(), f -> tank.getTransformFromString(f, partialTick));
         org.joml.Quaterniond q = rots.computeIfAbsent(d.frameName(), f -> tank.getRotationFromString(f, partialTick));
         org.joml.Vector4d c = m.transform(new org.joml.Vector4d(d.px(), d.py(), d.pz(), 1.0));
         org.joml.Vector3d n = q.transform(new org.joml.Vector3d(d.nx(), d.ny(), d.nz())).normalize();
         org.joml.Vector3d u = q.transform(new org.joml.Vector3d(d.tx(), d.ty(), d.tz())).normalize();
         org.joml.Vector3d v = new org.joml.Vector3d(n).cross(u).normalize();
         // Lift off the plate a little to avoid z-fighting.
         double cx = c.x - ox + n.x * 0.02;
         double cy = c.y - oy + n.y * 0.02;
         double cz = c.z - oz + n.z * 0.02;
         double hu = d.size() * 0.5 * d.aspect();
         double hv = d.size() * 0.5;
         float u0 = (d.kind() % 2) * 0.5F;
         float v0 = (d.kind() / 2) * 0.5F;
         double[][] corners = {
            {cx - u.x * hu - v.x * hv, cy - u.y * hu - v.y * hv, cz - u.z * hu - v.z * hv},
            {cx + u.x * hu - v.x * hv, cy + u.y * hu - v.y * hv, cz + u.z * hu - v.z * hv},
            {cx + u.x * hu + v.x * hv, cy + u.y * hu + v.y * hv, cz + u.z * hu + v.z * hv},
            {cx - u.x * hu + v.x * hv, cy - u.y * hu + v.y * hv, cz - u.z * hu + v.z * hv}
         };
         float[][] uv = {{u0, v0 + 0.5F}, {u0 + 0.5F, v0 + 0.5F}, {u0 + 0.5F, v0}, {u0, v0}};
         // Both windings, so the mark shows whichever way the quad ends up facing.
         int[][] orders = {{0, 1, 2, 3}, {3, 2, 1, 0}};

         for (int[] order : orders) {
            for (int i : order) {
               vc.addVertex(pose, (float)corners[i][0], (float)corners[i][1], (float)corners[i][2])
                  .setColor(255, 255, 255, 255)
                  .setUv(uv[i][0], uv[i][1])
                  .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                  .setLight(packedLight)
                  .setNormal(pose, (float)n.x, (float)n.y, (float)n.z);
            }
         }
      }
   }

   private final void emitExhaustSmoke(VehicleEntity entity, PoseStack poseStack, float yaw, float partialTick) {
      Level level = entity.level();
      if (level.isClientSide) {
         if (entity.engineRunning()) {
            VehicleModelEntry var10000 = this.getCurrentModelEntry(poseStack, entity);
            if (var10000 != null) {
               VehicleModelInstance var14 = var10000.getInstance();
               if (var14 != null) {
                  VehicleModelInstance instance = var14;
                  Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

                  for (String boneName : SMOKE_BONES) {
                     BoneState var15 = instance.getBone(boneName);
                     if (var15 != null) {
                        BoneState bone = var15;
                        if ((int)(Math.random() * 6) == 0) {
                           poseStack.pushPose();
                           this.rotateVehicleAxis(entity, poseStack, yaw, partialTick);
                           poseStack.scale(this.renderScale(), this.renderScale(), this.renderScale());
                           poseStack.mulPose(bone.getGlobalTransform((BoneTreeInstance)instance));
                           BoneDefinition def = bone.definition();
                           poseStack.translate(def.pivotX(), def.pivotY(), def.pivotZ());
                           Matrix4f pose = poseStack.last().pose();
                           poseStack.popPose();
                           Vec3 worldPos = new Vec3(pose.m30() + camera.x, pose.m31() + camera.y, pose.m32() + camera.z);
                           SimpleParticleType var10001 = ParticleTypes.SMOKE;
                           Intrinsics.checkNotNullExpressionValue(ParticleTypes.SMOKE, "SMOKE");
                           ParticleOptions var16 = (ParticleOptions)var10001;
                           Intrinsics.checkNotNull(level);
                           entity.addRandomParticle(var16, worldPos, 0.01F, level, 0.03F, 1);
                        }
                     }
                  }

                  return;
               }
            }
         }
      }
   }

   static {
      String[] var0 = new String[]{"POS_Smoking_trail", "POS_Smoking_trail2"};
      SMOKE_BONES = CollectionsKt.listOf(var0);
   }

   public static final class Companion {
      private Companion() {
      }

      // $VF: synthetic method
      public Companion(DefaultConstructorMarker $constructor_marker) {
         this();
      }
   }
}
