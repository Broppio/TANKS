package rev.rev.warsteel.mixin;

import com.atsuishio.superbwarfare.client.renderer.TextureBrightnessHandler;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rev.rev.warsteel.client.renderer.entity.WarSteelTankRenderer;

/**
 * Superb Warfare renders wrecked ground vehicles with their texture darkened to 30%, which turns
 * them black. WarSteel vehicles keep their paint (the fire, smoke and hit marks show the damage).
 */
@Mixin(value = GeoVehicleRenderer.class, remap = false)
public class WreckTextureMixin {
   @Redirect(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lcom/atsuishio/superbwarfare/client/renderer/TextureBrightnessHandler;getBrightenedTexture(Lnet/minecraft/resources/ResourceLocation;F)Lnet/minecraft/resources/ResourceLocation;"
      )
   )
   private ResourceLocation warsteel$keepWreckPaint(TextureBrightnessHandler handler, ResourceLocation texture, float brightness) {
      return WarSteelTankRenderer.RENDERING_WARSTEEL.get() ? texture : handler.getBrightenedTexture(texture, brightness);
   }
}
