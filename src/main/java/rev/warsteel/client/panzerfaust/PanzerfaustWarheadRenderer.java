package rev.rev.warsteel.client.panzerfaust;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import rev.rev.warsteel.entity.projectile.PanzerfaustWarheadEntity;

/** Draws the flying warhead, nose along its flight path. */
public class PanzerfaustWarheadRenderer extends EntityRenderer<PanzerfaustWarheadEntity> {
   public PanzerfaustWarheadRenderer(EntityRendererProvider.Context ctx) {
      super(ctx);
   }

   @Override
   public void render(@NotNull PanzerfaustWarheadEntity e, float yaw, float partialTick, PoseStack ps, @NotNull MultiBufferSource buffers, int light) {
      Vec3 v = e.getDeltaMovement();
      if (v.lengthSqr() < 1.0E-8) {
         v = Vec3.directionFromRotation(e.getXRot(), e.getYRot());
      }

      double h = Math.sqrt(v.x * v.x + v.z * v.z);
      float[] c = PanzerfaustMesh.warheadCentre();
      ps.pushPose();
      // The mesh's nose points along -z: turn it to the heading, then tilt it to the climb angle.
      ps.mulPose(Axis.YP.rotation((float)Mth.atan2(-v.x, -v.z)));
      ps.mulPose(Axis.XP.rotation((float)Mth.atan2(v.y, h)));
      ps.translate(-c[0], -c[1], -c[2]);
      PanzerfaustMesh.draw("warhead", ps.last(), buffers.getBuffer(RenderType.entityCutoutNoCull(PanzerfaustMesh.TEXTURE)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
      ps.popPose();
      super.render(e, yaw, partialTick, ps, buffers, light);
   }

   @NotNull
   @Override
   public ResourceLocation getTextureLocation(@NotNull PanzerfaustWarheadEntity e) {
      return PanzerfaustMesh.TEXTURE;
   }
}
