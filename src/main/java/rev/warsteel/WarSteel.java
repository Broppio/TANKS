package rev.rev.warsteel;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import rev.rev.warsteel.combat.RicochetHandler;
import rev.rev.warsteel.event.MuzzleParticleHandler;
import rev.rev.warsteel.init.ModEntities;
import rev.rev.warsteel.init.ModItem;
import rev.rev.warsteel.init.ModParticleTypes;
import rev.rev.warsteel.init.ModSounds;
import rev.rev.warsteel.init.ModTabs;

@Mod(WarSteel.MODID)
public class WarSteel {
   public static final String MODID = "warsteel";
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Logger LOG_OBBS = org.slf4j.LoggerFactory.getLogger("warsteel/obbs");

   public WarSteel(IEventBus bus) {
      ModEntities.REGISTRY.register(bus);
      ModTabs.TABS.register(bus);
      ModSounds.REGISTRY.register(bus);
      ModItem.ITEMS.register(bus);
      ModParticleTypes.register(bus);
      NeoForge.EVENT_BUS.register(RicochetHandler.INSTANCE);
      NeoForge.EVENT_BUS.register(MuzzleParticleHandler.INSTANCE);
      NeoForge.EVENT_BUS.register(rev.rev.warsteel.damage.WarSteelCommands.INSTANCE);
      LOGGER.info("WarSteel: registered ricochet and muzzle particle handlers");
   }
}
