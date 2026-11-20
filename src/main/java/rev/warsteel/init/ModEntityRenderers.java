package rev.rev.warsteel.init;

import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import rev.rev.warsteel.client.renderer.entity.KV1Renderer;
import rev.rev.warsteel.client.renderer.entity.KV2Renderer;
import rev.rev.warsteel.client.renderer.entity.M4A3ShermanRenderer;
import rev.rev.warsteel.client.renderer.entity.T26Renderer;
import rev.rev.warsteel.client.renderer.entity.TigerH1Renderer;

@EventBusSubscriber(
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public class ModEntityRenderers {
   @SubscribeEvent
   public static void registerEntityRenderers(RegisterRenderers event) {
      event.registerEntityRenderer(ModEntities.TigerH1.get(), TigerH1Renderer::new);
      event.registerEntityRenderer(ModEntities.KV_1.get(), KV1Renderer::new);
      event.registerEntityRenderer(ModEntities.KV_2.get(), KV2Renderer::new);
      event.registerEntityRenderer(ModEntities.M4A3_SHERMAN.get(), M4A3ShermanRenderer::new);
      event.registerEntityRenderer(ModEntities.T_26.get(), T26Renderer::new);
      event.registerEntityRenderer(ModEntities.FLAK_88.get(), rev.rev.warsteel.client.renderer.entity.Flak88Renderer::new);
   }
}
