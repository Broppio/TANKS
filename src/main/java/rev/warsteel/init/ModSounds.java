package rev.rev.warsteel.init;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
   public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(Registries.SOUND_EVENT, "warsteel");
   public static final DeferredHolder<SoundEvent, SoundEvent> TIGER_FIRE_1P = register("tiger1_1p_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> TIGER_FIRE_3P = register("tiger1_3p_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> KV1_FIRE_1P = register("kv1_1p_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> KV1_FIRE_3P = register("kv1_3p_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> KV2_FIRE_1P = register("kv2_1p_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> KV2_FIRE_3P = register("kv2_3p_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> KV_ENGINE = register("kv_engine");
   public static final DeferredHolder<SoundEvent, SoundEvent> TIGER_H1_ENGINE = register("tiger_engine");
   public static final DeferredHolder<SoundEvent, SoundEvent> TIGER_2_ENGINE = register("tiger2_engine");
   public static final DeferredHolder<SoundEvent, SoundEvent> DEFAULT_RELOAD = register("default_reload");
   public static final DeferredHolder<SoundEvent, SoundEvent> SHERMAN_FIRE_1P = register("sherman_1p_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> SHERMAN_FIRE_3P = register("sherman_3p_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> SHERMAN_ENGINE = register("sherman_engine");
   public static final DeferredHolder<SoundEvent, SoundEvent> T26_FIRE_1P = register("t26_1p_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> T26_FIRE_3P = register("t26_3p_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> T26_ENGINE = register("t26_engine");
   public static final DeferredHolder<SoundEvent, SoundEvent> WHEEL_STEP = register("wheel_step");
   public static final DeferredHolder<SoundEvent, SoundEvent> YX_100_VERY_FAR = register("yx_100_veryfar");
   public static final DeferredHolder<SoundEvent, SoundEvent> YX_100_RELOAD = register("yx_100_reload");
   public static final DeferredHolder<SoundEvent, SoundEvent> ARMOR_RICOCHET = register("armor_ricochet");
   public static final DeferredHolder<SoundEvent, SoundEvent> SHELL_NONPEN = register("shell_nonpen");
   @Deprecated
   public static final DeferredHolder<SoundEvent, SoundEvent> RICOCHET_1 = register("ricochet1");
   @Deprecated
   public static final DeferredHolder<SoundEvent, SoundEvent> RICOCHET_2 = register("ricochet2");
   @Deprecated
   public static final DeferredHolder<SoundEvent, SoundEvent> RICOCHET_3 = register("ricochet3");
   @Deprecated
   public static final List<DeferredHolder<SoundEvent, SoundEvent>> RICOCHET = List.of(RICOCHET_1, RICOCHET_2, RICOCHET_3);

   private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
      return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("warsteel", name)));
   }
}
