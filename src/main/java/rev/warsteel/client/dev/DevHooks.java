package rev.rev.warsteel.client.dev;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Development only (inactive unless the JVM runs with -Dwarsteel.dev=true): lets automated test
 * worlds toggle the vehicle gunsight zoom from a chat line, since they can't press the mouse.
 */
@EventBusSubscriber(modid = "warsteel", value = Dist.CLIENT)
public final class DevHooks {
   private static final boolean ENABLED = Boolean.getBoolean("warsteel.dev");

   private DevHooks() {
   }

   private static int ticks;

   @SubscribeEvent
   public static void onTick(ClientTickEvent.Post event) {
      if (ENABLED && ++ticks % 100 == 0) {
         org.slf4j.LoggerFactory.getLogger("warsteel/dev").info("client fps {}", net.minecraft.client.Minecraft.getInstance().getFps());
      }
   }

   @SubscribeEvent
   public static void onChat(ClientChatReceivedEvent event) {
      if (!ENABLED) {
         return;
      }

      String text = event.getMessage().getString();
      if (text.contains("WSZOOM on")) {
         ClientEventHandler.zoomVehicle = true;
      } else if (text.contains("WSZOOM off")) {
         ClientEventHandler.zoomVehicle = false;
      }
   }
}
