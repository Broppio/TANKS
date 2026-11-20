package rev.rev.warsteel.damage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loads data/<namespace>/warsteel/modules/*.json. */
public final class DamageModelRegistry extends SimpleJsonResourceReloadListener {
   public static final DamageModelRegistry INSTANCE = new DamageModelRegistry();
   private static final Logger LOG = LoggerFactory.getLogger("warsteel/modules");
   private static Map<ResourceLocation, DamageModel> models = Map.of();

   private DamageModelRegistry() {
      super(new Gson(), "warsteel/modules");
   }

   @Nullable
   public static DamageModel get(ResourceLocation vehicle) {
      return models.get(vehicle);
   }

   @Override
   protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
      Map<ResourceLocation, DamageModel> out = new HashMap<>();

      for (Map.Entry<ResourceLocation, JsonElement> e : map.entrySet()) {
         try {
            DamageModel model = parse(e.getValue().getAsJsonObject());
            out.put(model.vehicle(), model);
         } catch (Exception ex) {
            LOG.error("failed to read damage model {}", e.getKey(), ex);
         }
      }

      models = out;
      LOG.info("loaded damage models for {} vehicles: {}", out.size(), out.keySet());
   }

   static DamageModel parse(JsonObject o) {
      List<ModuleDef> modules = new ArrayList<>();
      JsonArray arr = GsonHelper.getAsJsonArray(o, "Modules");

      for (JsonElement el : arr) {
         JsonObject m = el.getAsJsonObject();
         ModuleType type = ModuleType.valueOf(GsonHelper.getAsString(m, "Type").toUpperCase(Locale.ROOT));
         CrewRole role = m.has("Role") ? CrewRole.valueOf(GsonHelper.getAsString(m, "Role").toUpperCase(Locale.ROOT)) : null;
         if (type == ModuleType.CREW && role == null) {
            throw new IllegalArgumentException("crew module " + GsonHelper.getAsString(m, "Name", "?") + " needs a Role");
         }

         String side = m.has("Side") ? GsonHelper.getAsString(m, "Side").toUpperCase(Locale.ROOT) : null;
         String name = GsonHelper.getAsString(m, "Name", role != null ? role.name() : type.name() + "_" + modules.size());
         modules.add(
            new ModuleDef(
               modules.size(),
               name,
               type,
               role,
               side,
               GsonHelper.getAsString(m, "Transform", "Vehicle"),
               vec(m, "Position"),
               vec(m, "Size"),
               GsonHelper.getAsFloat(m, "Health", type.defaultHealth),
               GsonHelper.getAsBoolean(m, "External", type.defaultExternal),
               GsonHelper.getAsDouble(m, "Armour", type.defaultArmour),
               GsonHelper.getAsDouble(m, "FireChance", type.defaultFireChance),
               GsonHelper.getAsDouble(m, "DetonationChance", type.defaultDetonationChance),
               GsonHelper.getAsInt(m, "Seat", -1)
            )
         );
      }

      return new DamageModel(
         ResourceLocation.parse(GsonHelper.getAsString(o, "Vehicle")),
         List.copyOf(modules),
         GsonHelper.getAsInt(o, "MinCrew", 2),
         GsonHelper.getAsInt(o, "MainGunSeat", 0),
         GsonHelper.getAsFloat(o, "RepairSeconds", 25.0F),
         GsonHelper.getAsFloat(o, "CrewReplaceSeconds", 60.0F),
         GsonHelper.getAsFloat(o, "CrewSwapSeconds", 5.0F),
         GsonHelper.getAsFloat(o, "FireSeconds", 14.0F),
         GsonHelper.getAsFloat(o, "FallbackDamageMultiplier", 0.25F),
         GsonHelper.getAsFloat(o, "AmmoExplosionRadius", 6.0F)
      );
   }

   private static double[] vec(JsonObject o, String key) {
      JsonArray a = GsonHelper.getAsJsonArray(o, key);
      return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
   }
}
