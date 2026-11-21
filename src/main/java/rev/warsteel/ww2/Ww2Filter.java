package rev.rev.warsteel.ww2;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.material.BlueprintItem;
import com.atsuishio.superbwarfare.item.misc.PerkItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps the game to WarSteel's Second World War setting. With WarSteel installed:
 * <ul>
 * <li>every Superb Warfare firearm (and its perks and blueprints) is disabled;</li>
 * <li>Superb Warfare's own vehicles can't be built, deployed or spawned (WarSteel's can);</li>
 * <li>Superb Warfare items that don't fit the period (drones, missiles, modern armour, C4...) are
 *     disabled - the list is the item tag {@code warsteel:disabled}, editable with a data pack.</li>
 * </ul>
 * Disabled items lose their recipes, vanish from the creative tabs and are removed from players'
 * inventories, the ground and mobs' hands. Vehicles can be allowed back with the entity type tag
 * {@code warsteel:allowed_vehicles}. Start the game with {@code -Dwarsteel.allowModern=true} to
 * switch all of this off.
 */
public final class Ww2Filter {
   public static final Ww2Filter INSTANCE = new Ww2Filter();
   public static final boolean ENABLED = !Boolean.getBoolean("warsteel.allowModern");
   public static final TagKey<Item> DISABLED_ITEMS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("warsteel", "disabled"));
   public static final TagKey<EntityType<?>> ALLOWED_VEHICLES =
      TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("warsteel", "allowed_vehicles"));
   private static final String SBW = "superbwarfare";
   private static final Logger LOG = LoggerFactory.getLogger("warsteel/ww2");

   private Ww2Filter() {
   }

   // ------------------------------------------------------------------------------------------ rules

   public static boolean isDisabled(@Nullable ItemStack stack) {
      if (!ENABLED || stack == null || stack.isEmpty()) {
         return false;
      }

      Item item = stack.getItem();
      if (stack.is(DISABLED_ITEMS)) {
         return true;
      }

      ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
      if (SBW.equals(id.getNamespace()) && (item instanceof GunItem || item instanceof PerkItem || item instanceof BlueprintItem)) {
         return true;
      }

      // A vehicle container holding one of Superb Warfare's own vehicles.
      EntityType<?> inside = containedVehicle(stack);
      return inside != null && isDisabledVehicle(inside);
   }

   public static boolean isDisabledVehicle(EntityType<?> type) {
      if (!ENABLED) {
         return false;
      }

      String ns = BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace();
      return !"warsteel".equals(ns) && !type.is(ALLOWED_VEHICLES);
   }

   @Nullable
   private static EntityType<?> containedVehicle(ItemStack stack) {
      CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
      if (data == null || !SBW.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) {
         return null;
      }

      String type = data.copyTag().getString("EntityType");
      ResourceLocation id = type.isEmpty() ? null : ResourceLocation.tryParse(type);
      return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id) ? BuiltInRegistries.ENTITY_TYPE.get(id) : null;
   }

   // ------------------------------------------------------------------------------------------ recipes

   @SubscribeEvent
   public void onServerStarted(ServerStartedEvent event) {
      this.filterRecipes(event.getServer());
   }

   /** Fired after every /reload, before recipes are sent to players. */
   @SubscribeEvent
   public void onDatapackSync(OnDatapackSyncEvent event) {
      if (event.getPlayer() == null) {
         this.filterRecipes(event.getPlayerList().getServer());
      }
   }

   private void filterRecipes(MinecraftServer server) {
      if (!ENABLED) {
         return;
      }

      RecipeManager recipes = server.getRecipeManager();
      List<RecipeHolder<?>> keep = new ArrayList<>();
      int removed = 0;
      for (RecipeHolder<?> holder : recipes.getRecipes()) {
         ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
         boolean drop;
         if (type != null && SBW.equals(type.getNamespace()) && type.getPath().equals("vehicle_assembling")) {
            // Superb Warfare's vehicle assembly table builds its own vehicles; WarSteel's come from factories.
            drop = true;
         } else {
            ItemStack result;
            try {
               result = holder.value().getResultItem(server.registryAccess());
            } catch (RuntimeException e) {
               result = ItemStack.EMPTY;
            }
            drop = isDisabled(result);
         }

         if (drop) {
            removed++;
         } else {
            keep.add(holder);
         }
      }

      if (removed > 0) {
         recipes.replaceRecipes(keep);
         LOG.info("WW2 setting: removed {} recipes for disabled items and vehicles", removed);
      }
   }

   // ------------------------------------------------------------------------------------------ creative tabs

   /** Mod-bus event: hide disabled items and vehicle containers from every creative tab. */
   public static void onBuildTabs(BuildCreativeModeTabContentsEvent event) {
      if (!ENABLED) {
         return;
      }

      List<ItemStack> hidden = new ArrayList<>();
      for (ItemStack s : event.getParentEntries()) {
         if (isDisabled(s)) {
            hidden.add(s);
         }
      }
      for (ItemStack s : event.getSearchEntries()) {
         if (isDisabled(s) && !hidden.contains(s)) {
            hidden.add(s);
         }
      }

      for (ItemStack s : hidden) {
         try {
            event.remove(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
         } catch (RuntimeException ignored) {
            // Already gone from one of the two lists.
         }
      }
   }

   // ------------------------------------------------------------------------------------------ world

   /** Disabled vehicles can't be spawned, deployed or loaded; disabled items can't lie on the ground; mobs drop their guns. */
   @SubscribeEvent(priority = EventPriority.HIGH)
   public void onJoin(EntityJoinLevelEvent event) {
      if (!ENABLED || event.getLevel().isClientSide()) {
         return;
      }

      var e = event.getEntity();
      if (e instanceof VehicleEntity && isDisabledVehicle(e.getType())) {
         event.setCanceled(true);
      } else if (e instanceof ItemEntity item && isDisabled(item.getItem())) {
         event.setCanceled(true);
      } else if (e instanceof LivingEntity living && !(e instanceof Player)) {
         for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (isDisabled(living.getItemBySlot(slot))) {
               living.setItemSlot(slot, ItemStack.EMPTY);
            }
         }
      }
   }

   @SubscribeEvent
   public void onUseItem(PlayerInteractEvent.RightClickItem event) {
      if (isDisabled(event.getItemStack())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public void onUseOnBlock(PlayerInteractEvent.RightClickBlock event) {
      if (isDisabled(event.getItemStack())) {
         event.setUseItem(net.neoforged.neoforge.common.util.TriState.FALSE);
      }
   }

   /** Anything disabled that a player still has (old inventories, loot, other mods) is removed. */
   @SubscribeEvent
   public void onPlayerTick(PlayerTickEvent.Post event) {
      if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
         return;
      }

      int removed = 0;
      var inv = player.getInventory();
      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack s = inv.getItem(i);
         if (isDisabled(s)) {
            removed += s.getCount();
            inv.setItem(i, ItemStack.EMPTY);
         }
      }

      if (isDisabled(player.containerMenu.getCarried())) {
         removed += player.containerMenu.getCarried().getCount();
         player.containerMenu.setCarried(ItemStack.EMPTY);
      }

      if (removed > 0) {
         player.displayClientMessage(Component.translatable("message.warsteel.ww2_removed", removed).withStyle(ChatFormatting.GRAY), true);
      }
   }
}
