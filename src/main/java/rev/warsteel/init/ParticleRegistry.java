package rev.rev.warsteel.init;

import net.minecraft.core.particles.ParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import rev.rev.warsteel.client.particle.AfterburnerFlameParticleProvider;

@EventBusSubscriber(
   modid = "warsteel",
   value = {Dist.CLIENT},
   bus = Bus.MOD
)
public class ParticleRegistry {
   @SubscribeEvent
   public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
      event.registerSpriteSet(ModParticleTypes.AFTERBURNER_FLAME.get(), AfterburnerFlameParticleProvider::new);
   }
}
