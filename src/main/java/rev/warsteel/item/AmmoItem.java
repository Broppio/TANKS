package rev.rev.warsteel.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A tank round. Loaded into a WarSteel tank's inventory like Superb Warfare's own shells. */
public class AmmoItem extends Item {
   private final AmmoSpec spec;

   public AmmoItem(AmmoSpec spec) {
      super(new Item.Properties().stacksTo(spec.stackSize()));
      this.spec = spec;
   }

   public AmmoSpec spec() {
      return this.spec;
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.warsteel.ammo.class", this.spec.shellClass()).withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.warsteel.ammo.pen", fmt(this.spec.penetration())).withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.warsteel.ammo.velocity", fmt(this.spec.velocity())).withStyle(ChatFormatting.GRAY));
      if (this.spec.fillerGrams() > 0.0) {
         tooltip.add(Component.translatable("tooltip.warsteel.ammo.filler", fmt(this.spec.fillerGrams())).withStyle(ChatFormatting.GRAY));
      }

      tooltip.add(Component.translatable("tooltip.warsteel.ammo.mass", fmt(this.spec.massKg())).withStyle(ChatFormatting.GRAY));
      Component[] names = this.spec
         .vehicles()
         .stream()
         .filter(v -> !v.endsWith("-grey") && !v.endsWith("-sand"))
         .map(v -> Component.translatable("entity." + ResourceLocation.parse(v).getNamespace() + "." + ResourceLocation.parse(v).getPath()))
         .toArray(Component[]::new);
      if (names.length > 0) {
         tooltip.add(Component.translatable("tooltip.warsteel.ammo.used_by", names[0]).withStyle(ChatFormatting.DARK_GRAY));
      }
   }

   private static String fmt(double v) {
      return v == Math.rint(v) ? Long.toString((long)v) : String.format(java.util.Locale.ROOT, "%.2f", v);
   }
}
