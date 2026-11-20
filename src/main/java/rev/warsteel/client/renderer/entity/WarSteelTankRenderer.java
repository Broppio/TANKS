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

      super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
      this.emitExhaustSmoke(entity, poseStack, yaw, partialTick);
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
