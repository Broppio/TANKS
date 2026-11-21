package rev.rev.warsteel.item;

import com.atsuishio.superbwarfare.item.container.ContainerBlockItem;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * A factory-built vehicle, crated for the stockpile. WarSteel vehicles can't be crafted: they come
 * out of Nations at War factories as these crates (one per production period, see
 * {@link #productionDays()}). Right-click to unpack it into Superb Warfare's deployable vehicle
 * container, then place that to roll the vehicle out.
 */
public class VehicleCrateItem extends Item {
   private final Supplier<? extends EntityType<?>> vehicle;
   private final double productionDays;

   public VehicleCrateItem(Supplier<? extends EntityType<?>> vehicle, double productionDays) {
      super(new Item.Properties().stacksTo(1));
      this.vehicle = vehicle;
      this.productionDays = productionDays;
   }

   /** Real days a factory takes to build one. */
   public double productionDays() {
      return this.productionDays;
   }

   public EntityType<?> vehicle() {
      return this.vehicle.get();
   }

   @NotNull
   @Override
   public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
      ItemStack crate = player.getItemInHand(hand);
      if (!level.isClientSide) {
         ItemStack container = ContainerBlockItem.createInstance(this.vehicle.get());
         level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
         crate.shrink(1);
         if (crate.isEmpty()) {
            return InteractionResultHolder.sidedSuccess(container, false);
         }

         if (!player.getInventory().add(container)) {
            player.drop(container, false);
         }
      }

      return InteractionResultHolder.sidedSuccess(crate, level.isClientSide);
   }

   @Override
   public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext ctx, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.warsteel.crate.vehicle", this.vehicle.get().getDescription()).withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.warsteel.crate.production", formatDays(this.productionDays)).withStyle(ChatFormatting.DARK_AQUA));
      tooltip.add(Component.translatable("tooltip.warsteel.crate.unpack").withStyle(ChatFormatting.DARK_GRAY));
   }

   public static String formatDays(double days) {
      return days == Math.rint(days) ? String.valueOf((long)days) : String.format(java.util.Locale.ROOT, "%.1f", days);
   }
}
