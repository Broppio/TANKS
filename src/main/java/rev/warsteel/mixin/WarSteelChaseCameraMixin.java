package rev.rev.warsteel.mixin;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import rev.rev.warsteel.entity.vehicle.ChaseCameraOnly;

@Mixin({Camera.class})
public abstract class WarSteelChaseCameraMixin {
   @ModifyVariable(
      method = {"setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private boolean warsteel$forceChaseCamera(boolean detached, BlockGetter level, Entity entity, boolean detachedArg, boolean mirrored, float partialTicks) {
      if (detached) {
         return true;
      } else if (entity != null && entity.getVehicle() instanceof ChaseCameraOnly) {
         return Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON ? false : !ClientEventHandler.zoomVehicle;
      } else {
         return false;
      }
   }
}
