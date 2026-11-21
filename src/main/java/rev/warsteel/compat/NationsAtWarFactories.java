package rev.rev.warsteel.compat;

import com.nationsatwar.data.DataStore;
import com.nationsatwar.data.Factory;
import com.nationsatwar.discord.Discord;
import com.nationsatwar.factory.FactoryManager;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rev.rev.warsteel.item.VehicleCrateItem;

/**
 * Nations at War integration: WarSteel vehicles are built in its factories instead of crafted.
 *
 * <p>A nation requests a factory for a vehicle crate (e.g. {@code warsteel:t26_crate}) like any
 * other item. Nations at War pays every factory out on one global cycle (30 days by default);
 * for crate factories WarSteel takes over the timing so each vehicle has its own build time
 * ({@link VehicleCrateItem#productionDays()}: a PaK 38 in 8 days, a Tiger in 42). It keeps the
 * factory's own payout clock current so Nations at War never pays a crate factory itself, and
 * delivers {@code amountPerCycle} crates to the owning nation's stockpile each production period.
 *
 * <p>Only loaded when Nations at War is installed (see WarSteel's constructor).
 */
public final class NationsAtWarFactories {
   public static final NationsAtWarFactories INSTANCE = new NationsAtWarFactories();
   private static final Logger LOG = LoggerFactory.getLogger("warsteel/factories");
   /**
    * Length of a production "day". Real days by default; start the server with
    * -Dwarsteel.factoryDayMillis=60000 to make every day one minute when testing.
    */
   private static final long DAY_MS = Long.getLong("warsteel.factoryDayMillis", 24L * 3600L * 1000L);
   /** Same cadence as Nations at War's own factory check (once a minute). */
   private static final int CHECK_TICKS = 1200;
   private int ticks;
   @Nullable
   private Method notifyNation;
   private boolean notifyLookedUp;

   private NationsAtWarFactories() {
   }

   @Nullable
   public static VehicleCrateItem crateFor(@Nullable String itemId) {
      if (itemId == null) {
         return null;
      }

      ResourceLocation id = ResourceLocation.tryParse(itemId);
      Item item = id != null ? BuiltInRegistries.ITEM.get(id) : null;
      return item instanceof VehicleCrateItem crate ? crate : null;
   }

   @SubscribeEvent
   public void onServerTick(ServerTickEvent.Post event) {
      if (++this.ticks % CHECK_TICKS != 0) {
         return;
      }

      try {
         this.check(event.getServer());
      } catch (Throwable t) {
         // Never take the server down over a version mismatch with the other mod.
         LOG.error("factory production check failed", t);
      }
   }

   private void check(MinecraftServer server) {
      Timers timers = Timers.get(server);
      long now = System.currentTimeMillis();
      boolean changed = false;

      for (Factory f : DataStore.get().factories) {
         VehicleCrateItem crate = crateFor(f.item);
         if (crate == null) {
            continue;
         }

         if (f.status != Factory.Status.ACTIVE) {
            // Pending, denied, revoked or stopped (land lost): the line isn't running.
            if (f.status != Factory.Status.INACTIVE) {
               changed |= timers.last.remove(f.id) != null;
            }
            continue;
         }

         UUID owner = FactoryManager.currentOwner(f);
         if (owner == null) {
            continue;
         }

         long period = Math.max(60_000L, (long)(crate.productionDays() * DAY_MS));
         Long started = timers.last.get(f.id);
         if (started == null) {
            // First time we see this line running: production starts from its approval.
            started = f.lastPayout > 0 ? f.lastPayout : now;
            timers.last.put(f.id, started);
            changed = true;
            this.notify(owner, "Factory " + f.name + " builds " + f.amountPerCycle + "x " + crate.vehicle().getDescription().getString()
               + " every " + VehicleCrateItem.formatDays(crate.productionDays()) + " days.", ChatFormatting.AQUA);
         }

         int guard = 0;
         while (now - started >= period && guard++ < 24) {
            DataStore.get().nation(owner).addStock(f.item, f.amountPerCycle);
            f.totalProduced += f.amountPerCycle;
            started += period;
            String what = f.amountPerCycle + "x " + crate.vehicle().getDescription().getString();
            this.notify(owner, "Factory " + f.name + " delivered " + what + " to the stockpile.", ChatFormatting.GREEN);
            Discord.send("factory", Discord.Channel.MAIN, "🏭 " + f.name + " delivered",
               "**" + what + "** added to the stockpile of **" + nationName(owner) + "**.", 2719929);
         }

         if (guard > 0) {
            timers.last.put(f.id, started);
            changed = true;
         }

         // Keep Nations at War's own (global-cycle) payout from firing for this line.
         f.lastPayout = now;
         DataStore.markDirty();
      }

      // Forget factories that no longer exist.
      changed |= timers.last.keySet().removeIf(id -> DataStore.get().factories.stream().noneMatch(f -> f.id == id));
      if (changed) {
         timers.setDirty();
      }
   }

   private static String nationName(UUID nation) {
      String n = DataStore.get().nation(nation).name;
      return n != null && !n.isBlank() ? n : nation.toString();
   }

   /** Nations at War's own "message the nation's online members" (private there). */
   private void notify(UUID nation, String msg, ChatFormatting colour) {
      if (!this.notifyLookedUp) {
         this.notifyLookedUp = true;
         try {
            this.notifyNation = FactoryManager.class.getDeclaredMethod("notifyNation", UUID.class, String.class, ChatFormatting.class);
            this.notifyNation.setAccessible(true);
         } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("can't message nations directly ({}); deliveries still go to the stockpile and Discord", e.toString());
         }
      }

      if (this.notifyNation != null) {
         try {
            this.notifyNation.invoke(null, nation, msg, colour);
         } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.debug("nation message failed", e);
         }
      }
   }

   /** When each crate factory last delivered (or started), by factory id. Saved with the world. */
   public static final class Timers extends SavedData {
      final Map<Integer, Long> last = new HashMap<>();

      static Timers get(MinecraftServer server) {
         return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Timers::new, Timers::load, null), "warsteel_factories");
      }

      static Timers load(CompoundTag tag, HolderLookup.Provider registries) {
         Timers t = new Timers();
         for (String k : tag.getAllKeys()) {
            try {
               t.last.put(Integer.parseInt(k), tag.getLong(k));
            } catch (NumberFormatException ignored) {
            }
         }
         return t;
      }

      @NotNull
      @Override
      public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
         this.last.forEach((id, time) -> tag.putLong(String.valueOf(id), time));
         return tag;
      }
   }
}
