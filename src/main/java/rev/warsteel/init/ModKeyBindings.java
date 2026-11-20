package rev.rev.warsteel.init;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(
   modid = "warsteel",
   value = {Dist.CLIENT},
   bus = Bus.MOD
)
public class ModKeyBindings {
   public static final String CATEGORY = "key.categories.warsteel";
   public static final KeyMapping TARGETING_CAMERA = new KeyMapping(
      "key.warsteel.targeting_camera", KeyConflictContext.IN_GAME, Type.KEYSYM, 84, "key.categories.warsteel"
   );
   public static final KeyMapping THERMAL_TOGGLE = new KeyMapping(
      "key.warsteel.thermal_toggle", KeyConflictContext.IN_GAME, Type.KEYSYM, 78, "key.categories.warsteel"
   );
   public static final KeyMapping LOCK_TARGET = new KeyMapping(
      "key.warsteel.lock_target", KeyConflictContext.IN_GAME, Type.KEYSYM, 88, "key.categories.warsteel"
   );
   public static final KeyMapping TOGGLE_GEAR = new KeyMapping(
      "key.warsteel.toggle_gear", KeyConflictContext.IN_GAME, Type.KEYSYM, 71, "key.categories.warsteel"
   );
   public static final KeyMapping EXIT_DRONE = new KeyMapping("key.warsteel.exit_drone", KeyConflictContext.IN_GAME, Type.KEYSYM, 82, "key.categories.warsteel");
   public static final KeyMapping ZOOM_IN = new KeyMapping("key.warsteel.zoom_in", KeyConflictContext.IN_GAME, Type.KEYSYM, 61, "key.categories.warsteel");
   public static final KeyMapping ZOOM_OUT = new KeyMapping("key.warsteel.zoom_out", KeyConflictContext.IN_GAME, Type.KEYSYM, 45, "key.categories.warsteel");

   @SubscribeEvent
   public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
      event.register(TARGETING_CAMERA);
      event.register(THERMAL_TOGGLE);
      event.register(LOCK_TARGET);
      event.register(TOGGLE_GEAR);
      event.register(EXIT_DRONE);
      event.register(ZOOM_IN);
      event.register(ZOOM_OUT);
   }
}
