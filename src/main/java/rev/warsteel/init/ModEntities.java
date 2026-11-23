package rev.rev.warsteel.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.EntityType.EntityFactory;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rev.rev.warsteel.entity.vehicle.KV1Entity;
import rev.rev.warsteel.entity.vehicle.KV2Entity;
import rev.rev.warsteel.entity.vehicle.M4A3ShermanEntity;
import rev.rev.warsteel.entity.vehicle.T26Entity;
import rev.rev.warsteel.entity.vehicle.TigerH1Entity;

public class ModEntities {
   public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(Registries.ENTITY_TYPE, "warsteel");
   public static final DeferredHolder<EntityType<?>, EntityType<TigerH1Entity>> TigerH1 = ifv("tiger-h1", TigerH1Entity::new);
   public static final DeferredHolder<EntityType<?>, EntityType<KV1Entity>> KV_1 = ifv("kv-1", KV1Entity::new);
   public static final DeferredHolder<EntityType<?>, EntityType<KV2Entity>> KV_2 = ifv("kv-2", KV2Entity::new);
   public static final DeferredHolder<EntityType<?>, EntityType<M4A3ShermanEntity>> M4A3_SHERMAN = ifv("m4a3-sherman", M4A3ShermanEntity::new);
   public static final DeferredHolder<EntityType<?>, EntityType<T26Entity>> T_26 = ifv("t-26-1931", T26Entity::new);
   public static final DeferredHolder<EntityType<?>, EntityType<rev.rev.warsteel.entity.vehicle.Flak88Entity>> FLAK_88 =
      reg("flak-88", rev.rev.warsteel.entity.vehicle.Flak88Entity::new, 3.0F, 2.2F);
   public static final DeferredHolder<EntityType<?>, EntityType<rev.rev.warsteel.entity.vehicle.Pak38Entity>> PAK_38 =
      reg("pak-38", rev.rev.warsteel.entity.vehicle.Pak38Entity::new, 2.0F, 1.4F);

   public static final DeferredHolder<EntityType<?>, EntityType<rev.rev.warsteel.entity.vehicle.T22Entity>> T_22 =
      reg("t-22", rev.rev.warsteel.entity.vehicle.T22Entity::new, 9.6F, 6.7F);
   public static final DeferredHolder<EntityType<?>, EntityType<rev.rev.warsteel.naval.TorpedoEntity>> TORPEDO =
      REGISTRY.register("torpedo_g7a", () -> Builder.<rev.rev.warsteel.naval.TorpedoEntity>of(rev.rev.warsteel.naval.TorpedoEntity::new, MobCategory.MISC)
         .sized(0.6F, 0.6F).clientTrackingRange(16).updateInterval(1).build("torpedo_g7a"));
   public static final DeferredHolder<EntityType<?>, EntityType<rev.rev.warsteel.entity.projectile.PanzerfaustWarheadEntity>> PANZERFAUST_WARHEAD =
      REGISTRY.register("panzerfaust_warhead", () -> Builder.<rev.rev.warsteel.entity.projectile.PanzerfaustWarheadEntity>of(
            rev.rev.warsteel.entity.projectile.PanzerfaustWarheadEntity::new, MobCategory.MISC)
         .sized(0.3F, 0.3F).clientTrackingRange(16).updateInterval(1).build("panzerfaust_warhead"));

   private static <T extends Entity> Builder<T> vehicle(EntityFactory<T> factory, float w, float h) {
      return Builder.of(factory, MobCategory.MISC).setTrackingRange(1028).setUpdateInterval(1).fireImmune().sized(w, h);
   }

   private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> reg(String name, EntityFactory<T> factory, float w, float h) {
      return REGISTRY.register(name, () -> vehicle(factory, w, h).build(name));
   }

   private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> ifv(String name, EntityFactory<T> factory) {
      return reg(name, factory, 3.0F, 2.0F);
   }
}
