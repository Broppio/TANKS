package rev.rev.warsteel.client.panzerfaust;

import com.atsuishio.superbwarfare.client.model.item.CustomGunModel;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import rev.rev.warsteel.item.gun.PanzerfaustItem;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

/** Model, texture and animations come from assets/warsteel/sbw/guns/panzerfaust_60.json. */
public class PanzerfaustItemModel extends CustomGunModel<PanzerfaustItem> {
   @Override
   public void setCustomAnimations(PanzerfaustItem animatable, long instanceId, AnimationState<PanzerfaustItem> animationState) {
      Player player = Minecraft.getInstance().player;
      if (player == null) {
         return;
      }

      ItemStack stack = player.getMainHandItem();
      if (this.shouldCancelRender(stack, animationState)) {
         return;
      }

      GeoBone gun = this.getAnimationProcessor().getBone("bone");
      GeoBone launcher = this.getAnimationProcessor().getBone("rpg");
      if (gun == null || launcher == null) {
         return;
      }

      // Same handling as Superb Warfare's RPG-7: aiming brings the leaf sight to the eye.
      double zp = ClientEventHandler.zoomPos;
      double zpz = ClientEventHandler.zoomPosZ;
      ClientEventHandler.handleShootAnimation(launcher, 1.0F, -0.4F, 1.2F, 1.3F, 1.0F, 1.0F, 0.5F, 0.7F);
      gun.setPosX(0.91F * (float)zp);
      gun.setPosY(-0.3F * (float)zp - (float)(0.2F * zpz));
      gun.setPosZ(2.0F * (float)zp + (float)(0.15F * zpz));
      gun.setRotZ(0.45F * (float)zp + (float)(0.02F * zpz));
      gun.setScaleZ(1.0F - 0.5F * (float)zp);
      ClientEventHandler.gunRootMove(this.getAnimationProcessor(), 0.0F, 0.0F, 0.0F, true);
   }
}
