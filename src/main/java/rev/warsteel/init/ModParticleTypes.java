package rev.rev.warsteel.init;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticleTypes {
   public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, "warsteel");
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AFTERBURNER_FLAME = PARTICLES.register("afterburner_flame", () -> new SimpleParticleType(true));

   public static void register(IEventBus bus) {
      PARTICLES.register(bus);
   }
}
