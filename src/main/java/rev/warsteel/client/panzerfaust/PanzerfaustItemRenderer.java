package rev.rev.warsteel.client.panzerfaust;

import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.ItemModelHelper;
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import rev.rev.warsteel.item.gun.PanzerfaustItem;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * Superb Warfare's gun renderer (arms, camera, recoil, muzzle flash) with the Panzerfaust's own
 * triangle mesh drawn at the "panzerfaust" bone - GeckoLib itself only draws cubes.
 */
public class PanzerfaustItemRenderer extends CustomGunRenderer<PanzerfaustItem> {
   public PanzerfaustItemRenderer() {
      super(new PanzerfaustItemModel());
   }

   @Override
   public void renderRecursively(
      PoseStack stack, PanzerfaustItem animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn,
      boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, int color
   ) {
      Minecraft mc = Minecraft.getInstance();
      String name = bone.getName();
      boolean arms = name.equals("Lefthand") || name.equals("Righthand");
      bone.setHidden(arms);
      LocalPlayer player = mc.player;
      if (player == null) {
         return;
      }

      ItemStack held = player.getMainHandItem();
      if (!(held.getItem() instanceof GunItem) || GeoItem.getId(held) != this.getInstanceId(animatable)) {
         ItemModelHelper.hideAllAttachments(bone, name);
      } else if (this.renderPerspective != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && this.renderPerspective != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
         ItemModelHelper.hideAllAttachments(bone, name);
      } else {
         AnimationHelper.handleShootFlare(name, stack, held, bone, buffer, packedLightIn);
      }

      if (arms) {
         AnimationHelper.renderArms(player, this.renderPerspective, stack, name, bone, buffer, type, packedLightIn, false);
      }

      super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, color);
   }

   @Override
   public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, int colour) {
      super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
      if (bone.isHidden() || !bone.getName().equals("panzerfaust")) {
         return;
      }

      PoseStack.Pose pose = poseStack.last();
      PanzerfaustMesh.draw("tube", pose, buffer, packedLight, packedOverlay, colour);
      PanzerfaustMesh.draw("sights", pose, buffer, packedLight, packedOverlay, colour);
      ItemStack shown = this.currentItemStack;
      if (shown == null || !(shown.getItem() instanceof PanzerfaustItem) || PanzerfaustItem.loaded(shown)) {
         PanzerfaustMesh.draw("warhead", pose, buffer, packedLight, packedOverlay, colour);
      }
   }
}
