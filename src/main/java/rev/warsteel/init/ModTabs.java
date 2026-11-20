package rev.rev.warsteel.init;

import com.atsuishio.superbwarfare.item.container.ContainerBlockItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModTabs {
   public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "warsteel");
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLOCKTANK_TAB = TABS.register(
      "warsteel-tank",
      () -> CreativeModeTab.builder()
         .title(Component.translatable("item_group.warsteel.ash-tank"))
         .icon(() -> new ItemStack(ModItem.WARSTEEL_TANK_ICON.get()))
         .displayItems((param, output) -> {
            output.accept(ContainerBlockItem.createInstance(ModEntities.TigerH1.get()));
            output.accept(ContainerBlockItem.createInstance(ModEntities.KV_1.get()));
            output.accept(ContainerBlockItem.createInstance(ModEntities.KV_2.get()));
            output.accept(ContainerBlockItem.createInstance(ModEntities.M4A3_SHERMAN.get()));
            output.accept(ContainerBlockItem.createInstance(ModEntities.T_26.get()));
            output.accept(new ItemStack(ModItem.TIGER_HULL.get()));
            output.accept(new ItemStack(ModItem.TIGER_TURRET.get()));
            output.accept(new ItemStack(ModItem.TIGER_BARREL.get()));
            output.accept(new ItemStack(ModItem.TIGER_TRACK.get()));
            output.accept(new ItemStack(ModItem.KV1_HULL.get()));
            output.accept(new ItemStack(ModItem.KV1_TURRET.get()));
            output.accept(new ItemStack(ModItem.KV1_BARREL.get()));
            output.accept(new ItemStack(ModItem.KV1_TRACK.get()));
            output.accept(new ItemStack(ModItem.KV2_HULL.get()));
            output.accept(new ItemStack(ModItem.KV2_TURRET.get()));
            output.accept(new ItemStack(ModItem.KV2_BARREL.get()));
            output.accept(new ItemStack(ModItem.KV2_TRACK.get()));
            output.accept(new ItemStack(ModItem.SHERMAN_HULL.get()));
            output.accept(new ItemStack(ModItem.SHERMAN_TURRET.get()));
            output.accept(new ItemStack(ModItem.SHERMAN_BARREL.get()));
            output.accept(new ItemStack(ModItem.SHERMAN_TRACK.get()));
            output.accept(new ItemStack(ModItem.T26_HULL.get()));
            output.accept(new ItemStack(ModItem.T26_TURRET.get()));
            output.accept(new ItemStack(ModItem.T26_BARREL.get()));
            output.accept(new ItemStack(ModItem.T26_TRACK.get()));
            ModItem.AMMO.values().forEach(ammo -> output.accept(new ItemStack(ammo.get())));
            Item spray = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("superbwarfare", "skin_spray"));
            if (spray != Items.AIR) {
               output.accept(new ItemStack(spray));
            }
         })
         .build()
   );
}
