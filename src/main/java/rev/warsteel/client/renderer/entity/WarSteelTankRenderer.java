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

      if (rev.rev.warsteel.damage.HitDecals.ENABLED && entity instanceof rev.rev.warsteel.damage.WarSteelTank tank) {
         this.renderDecals(tank, yaw, partialTick, poseStack, buffer, packedLight);
      }
      this.emitExhaustSmoke(entity, poseStack, yaw, partialTick);
   }

   /** Set while a WarSteel vehicle is being drawn (read by WreckTextureMixin). */
   public static final ThreadLocal<Boolean> RENDERING_WARSTEEL = ThreadLocal.withInitial(() -> Boolean.FALSE);

   private static final net.minecraft.resources.ResourceLocation DECALS =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("warsteel", "textures/misc/hit_decals.png");

   /**
    * Hit marks, placed in the frame of the plate that was struck so turret marks turn with it.
    * The server only knows the hit box, which can stand off the visible hull, so the first time a
    * mark is drawn it is dropped onto the model itself by tracing the model's cubes along the mark's
    * normal; the result is cached in the mark's own frame. Poly-mesh models (which the tracer
    * can't see) keep the hit-box position.
    */
   private void renderDecals(
      rev.rev.warsteel.damage.WarSteelTank tank, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight
   ) {
      java.util.List<rev.rev.warsteel.damage.HitDecals.Decal> decals = tank.decals();
      if (decals.isEmpty()) {
         tank.decalSnap.clear();
         return;
      }

      double ox = net.minecraft.util.Mth.lerp(partialTick, tank.xo, tank.getX());
      double oy = net.minecraft.util.Mth.lerp(partialTick, tank.yo, tank.getY());
      double oz = net.minecraft.util.Mth.lerp(partialTick, tank.zo, tank.getZ());
      com.mojang.blaze3d.vertex.VertexConsumer vc = buffer.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(DECALS));
      PoseStack.Pose pose = poseStack.last();
      java.util.Map<String, org.joml.Matrix4d> frames = new java.util.HashMap<>();
      java.util.Map<String, org.joml.Quaterniond> rots = new java.util.HashMap<>();
      if (tank.decalSnap.size() > decals.size() * 2 + 8) {
         tank.decalSnap.keySet().retainAll(new java.util.HashSet<>(decals));
      }

      BoneTreeInstance model = null;
      Matrix4f modelPose = null;

      for (rev.rev.warsteel.damage.HitDecals.Decal d : decals) {
         org.joml.Matrix4d m = frames.computeIfAbsent(d.frameName(), f -> tank.getTransformFromString(f, partialTick));
         org.joml.Quaterniond q = rots.computeIfAbsent(d.frameName(), f -> tank.getRotationFromString(f, partialTick));
         float[] snap = tank.decalSnap.get(d);
         if (snap == null) {
            if (model == null) {
               VehicleModelEntry entry = this.getCurrentModelEntry(poseStack, tank);
               model = entry != null ? entry.getInstance() : null;
               PoseStack ps = new PoseStack();
               this.rotateVehicleAxis(tank, ps, yaw, partialTick);
               ps.scale(this.renderScale(), this.renderScale(), this.renderScale());
               modelPose = new Matrix4f(ps.last().pose());
            }

            snap = snapToModel(d, m, q, model, modelPose, ox, oy, oz);
            tank.decalSnap.put(d, snap);
         }

         org.joml.Vector4d c = m.transform(snap.length == 6
            ? new org.joml.Vector4d(snap[0], snap[1], snap[2], 1.0)
            : new org.joml.Vector4d(d.px(), d.py(), d.pz(), 1.0));
         org.joml.Vector3d n = q.transform(snap.length == 6
            ? new org.joml.Vector3d(snap[3], snap[4], snap[5])
            : new org.joml.Vector3d(d.nx(), d.ny(), d.nz())).normalize();
         org.joml.Vector3d u = q.transform(new org.joml.Vector3d(d.tx(), d.ty(), d.tz()));
         u.sub(new org.joml.Vector3d(n).mul(u.dot(n)));
         if (u.lengthSquared() < 1.0E-6) {
            u = Math.abs(n.y) < 0.9 ? new org.joml.Vector3d(0, 1, 0).cross(n) : new org.joml.Vector3d(1, 0, 0).cross(n);
         }

         u.normalize();
         org.joml.Vector3d v = new org.joml.Vector3d(n).cross(u).normalize();
         // Just off the surface: enough to beat z-fighting, too little to see a gap.
         double lift = snap.length == 6 ? 0.008 : 0.02;
         double cx = c.x - ox + n.x * lift;
         double cy = c.y - oy + n.y * lift;
         double cz = c.z - oz + n.z * lift;
         double hu = d.size() * 0.5 * d.aspect();
         double hv = d.size() * 0.5;
         // 4 x 2 atlas, cell = kind.
         float u0 = (d.kind() % 4) * 0.25F;
         float v0 = (d.kind() / 4) * 0.5F;
         double[][] corners = {
            {cx - u.x * hu - v.x * hv, cy - u.y * hu - v.y * hv, cz - u.z * hu - v.z * hv},
            {cx + u.x * hu - v.x * hv, cy + u.y * hu - v.y * hv, cz + u.z * hu - v.z * hv},
            {cx + u.x * hu + v.x * hv, cy + u.y * hu + v.y * hv, cz + u.z * hu + v.z * hv},
            {cx - u.x * hu + v.x * hv, cy - u.y * hu + v.y * hv, cz - u.z * hu + v.z * hv}
         };
         float[][] uv = {{u0, v0 + 0.5F}, {u0 + 0.25F, v0 + 0.5F}, {u0 + 0.25F, v0}, {u0, v0}};
         // u x v = n, so 0-1-2-3 is counter-clockwise seen from outside: one face, outward.
         for (int i = 0; i < 4; i++) {
            vc.addVertex(pose, (float)corners[i][0], (float)corners[i][1], (float)corners[i][2])
               .setColor(255, 255, 255, 255)
               .setUv(uv[i][0], uv[i][1])
               .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
               .setLight(packedLight)
               .setNormal(pose, (float)n.x, (float)n.y, (float)n.z);
         }
      }
   }

   /**
    * Traces the model's cubes along the mark's normal through its hit-box position. Returns the
    * surface point and face normal in the mark's frame (6 floats), or an empty array if the model
    * has no cube there.
    */
   private static float[] snapToModel(
      rev.rev.warsteel.damage.HitDecals.Decal d, org.joml.Matrix4d m, org.joml.Quaterniond q, BoneTreeInstance model, Matrix4f modelPose,
      double ox, double oy, double oz
   ) {
      if (model == null) {
         return new float[0];
      }

      try {
         org.joml.Vector4d c = m.transform(new org.joml.Vector4d(d.px(), d.py(), d.pz(), 1.0));
         org.joml.Vector3d n = q.transform(new org.joml.Vector3d(d.nx(), d.ny(), d.nz())).normalize();
         Vec3 centre = new Vec3(c.x - ox, c.y - oy, c.z - oz);
         Vec3 nv = new Vec3(n.x, n.y, n.z);
         com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.ModelRayTraceResult hit =
            model.rayTrace(modelPose, Vec3.ZERO, centre.add(nv.scale(0.9)), centre.subtract(nv.scale(1.4)));
         if (hit == null || hit.location().distanceTo(centre) > 1.0) {
            return new float[0];
         }

         Vec3 fn = hit.normal() != null && hit.normal().lengthSqr() > 1.0E-6 ? hit.normal().normalize() : nv;
         if (fn.dot(nv) < 0.5) {
            // Struck the inside or the edge of some other cube: keep the plate's normal.
            fn = nv;
         }

         org.joml.Vector4d local = new org.joml.Matrix4d(m).invert()
            .transform(new org.joml.Vector4d(hit.location().x + ox, hit.location().y + oy, hit.location().z + oz, 1.0));
         org.joml.Vector3d ln = new org.joml.Quaterniond(q).conjugate().transform(new org.joml.Vector3d(fn.x, fn.y, fn.z)).normalize();
         return new float[]{(float)local.x, (float)local.y, (float)local.z, (float)ln.x, (float)ln.y, (float)ln.z};
      } catch (RuntimeException e) {
         return new float[0];
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
