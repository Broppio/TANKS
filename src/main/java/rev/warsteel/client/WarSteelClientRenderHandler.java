package rev.rev.warsteel.client;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.slf4j.Logger;
import rev.rev.warsteel.client.sight.SightRegistry;
import rev.rev.warsteel.client.sight.WarSteelSightOverlay;

@EventBusSubscriber(modid = "warsteel", bus = Bus.MOD, value = Dist.CLIENT)
public final class WarSteelClientRenderHandler {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation SBW_VEHICLE_CROSSHAIR = ResourceLocation.fromNamespaceAndPath("superbwarfare", "vehicle_crosshair");

   private WarSteelClientRenderHandler() {
   }

   @SubscribeEvent
   public static void registerGuiLayers(RegisterGuiLayersEvent event) {
      ResourceLocation id = WarSteelSightOverlay.INSTANCE.getID();

      try {
         event.registerBelow(SBW_VEHICLE_CROSSHAIR, id, WarSteelSightOverlay.INSTANCE);
      } catch (RuntimeException e) {
         LOGGER.warn("could not anchor the WarSteel sight below {}, falling back to below-all", SBW_VEHICLE_CROSSHAIR, e);
         event.registerBelowAll(id, WarSteelSightOverlay.INSTANCE);
      }
   }

   @SubscribeEvent
   public static void registerDamageHud(RegisterGuiLayersEvent event) {
      event.registerAboveAll(rev.rev.warsteel.client.hud.DamageModelHud.ID, rev.rev.warsteel.client.hud.DamageModelHud.INSTANCE);
      event.registerAboveAll(rev.rev.warsteel.client.replay.ShotReplayHud.ID, rev.rev.warsteel.client.replay.ShotReplayHud.INSTANCE);
   }

   @SubscribeEvent
   public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
      event.registerReloadListener(SightRegistry.INSTANCE);
   }
}
