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

   private static void registerPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
      event.registrar("1")
         .optional()
         .playToClient(
            rev.rev.warsteel.network.ShotReplayPayload.TYPE,
            rev.rev.warsteel.network.ShotReplayPayload.CODEC,
            // The handler class is only resolved when a packet arrives, i.e. on a client.
            (payload, context) -> context.enqueueWork(() -> rev.rev.warsteel.client.replay.ClientReplayHandler.handle(payload))
         )
         .playToServer(
            rev.rev.warsteel.network.ArtilleryPayloads.ArcControl.TYPE,
            rev.rev.warsteel.network.ArtilleryPayloads.ArcControl.CODEC,
            (payload, context) -> context.enqueueWork(() -> rev.rev.warsteel.combat.ArtilleryMode.handleControl(payload, context))
         )
         .playToClient(
            rev.rev.warsteel.network.ArtilleryPayloads.ShellLaunched.TYPE,
            rev.rev.warsteel.network.ArtilleryPayloads.ShellLaunched.CODEC,
            (payload, context) -> context.enqueueWork(() -> rev.rev.warsteel.client.artillery.ShellCamera.launched(payload))
         )
         .playToClient(
            rev.rev.warsteel.network.ArtilleryPayloads.ShellLanded.TYPE,
            rev.rev.warsteel.network.ArtilleryPayloads.ShellLanded.CODEC,
            (payload, context) -> context.enqueueWork(() -> rev.rev.warsteel.client.artillery.ShellCamera.landed(payload))
         );
   }

   public WarSteel(IEventBus bus) {
      ModEntities.REGISTRY.register(bus);
      ModTabs.TABS.register(bus);
      ModSounds.REGISTRY.register(bus);
      ModItem.ITEMS.register(bus);
      ModParticleTypes.register(bus);
      NeoForge.EVENT_BUS.register(RicochetHandler.INSTANCE);
      NeoForge.EVENT_BUS.register(MuzzleParticleHandler.INSTANCE);
      NeoForge.EVENT_BUS.register(rev.rev.warsteel.combat.FlakFuze.INSTANCE);
      NeoForge.EVENT_BUS.register(rev.rev.warsteel.combat.ArtilleryMode.INSTANCE);
      NeoForge.EVENT_BUS.register(rev.rev.warsteel.damage.WarSteelCommands.INSTANCE);
      bus.addListener(WarSteel::registerPayloads);
      // Second World War setting: Superb Warfare's firearms, own vehicles and modern gear are disabled.
      NeoForge.EVENT_BUS.register(rev.rev.warsteel.ww2.Ww2Filter.INSTANCE);
      bus.addListener(rev.rev.warsteel.ww2.Ww2Filter::onBuildTabs);
      if (net.neoforged.fml.ModList.get().isLoaded("nationsatwar")) {
         // Vehicles are built in Nations at War factories, each on its own production time.
         NeoForge.EVENT_BUS.register(rev.rev.warsteel.compat.NationsAtWarFactories.INSTANCE);
         LOGGER.info("WarSteel: Nations at War found, vehicle factories enabled");
      }
      LOGGER.info("WarSteel: registered ricochet and muzzle particle handlers");
   }
}
