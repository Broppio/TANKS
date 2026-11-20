package rev.rev.warsteel.init.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "warsteel",
   value = {Dist.CLIENT},
   bus = Bus.MOD
)
public class ClientKeyRegister {
   @SubscribeEvent
   public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
      ClientKeyMappings.register(event);
   }
}
