package rev.rev.warsteel.init.client;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public class ClientKeyMappings {
   public static KeyMapping OPEN_COORDINATE_SCREEN;
   public static KeyMapping TOGGLE_POD;
   public static KeyMapping THERMAL_VISION;
   public static KeyMapping VTOL_TOGGLE;

   public static void register(RegisterKeyMappingsEvent event) {
      OPEN_COORDINATE_SCREEN = new KeyMapping("key.warsteel.open_coordinate", 71, "key.categories.warsteel");
      event.register(OPEN_COORDINATE_SCREEN);
      TOGGLE_POD = new KeyMapping("key.warsteel.toggle_pod", 341, "key.categories.warsteel");
      event.register(TOGGLE_POD);
      THERMAL_VISION = new KeyMapping("key.warsteel.thermalvision", 70, "key.categories.warsteel");
      event.register(THERMAL_VISION);
      VTOL_TOGGLE = new KeyMapping("key.warsteel.vtol_toggle", 86, "key.categories.warsteel");
      event.register(VTOL_TOGGLE);
   }
}
