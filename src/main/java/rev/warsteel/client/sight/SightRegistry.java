package rev.rev.warsteel.client.sight;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import kotlin.collections.MapsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
@SourceDebugExtension({"SMAP\nSightRegistry.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SightRegistry.kt\nrev/rev/warsteel/client/sight/SightRegistry\n+ 2 _Maps.kt\nkotlin/collections/MapsKt___MapsKt\n+ 3 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,81:1\n216#2:82\n217#2:85\n1863#3,2:83\n*S KotlinDebug\n*F\n+ 1 SightRegistry.kt\nrev/rev/warsteel/client/sight/SightRegistry\n*L\n53#1:82\n53#1:85\n60#1:83,2\n*E\n"})
public final class SightRegistry extends SimpleJsonResourceReloadListener {
   @NotNull
   public static final SightRegistry INSTANCE = new SightRegistry();
   private static final Logger LOG = LogUtils.getLogger();
   @NotNull
   private static Map<String, SightSpec> byVehicle = MapsKt.emptyMap();

   private SightRegistry() {
      super(SightRegistryKt.access$getSIGHT_GSON$p(), "sights");
   }

   @Nullable
   public final SightSpec get(@Nullable String vehicleId) {
      return vehicleId == null ? null : byVehicle.get(vehicleId);
   }

   @Nullable
   public final SightSpec forEntity(@Nullable Entity entity) {
      return entity == null ? null : byVehicle.get(EntityType.getKey(entity.getType()).toString());
   }

   protected void apply(@NotNull Map<ResourceLocation, JsonElement> map, @NotNull ResourceManager rm, @NotNull ProfilerFiller profiler) {
      HashMap<String, SightSpec> out = new HashMap<>();
      int loaded = 0;

      for (Entry element$iv : map.entrySet()) {
         ResourceLocation id = (ResourceLocation)element$iv.getKey();
         JsonElement el = (JsonElement)element$iv.getValue();

         try {
            SightSpec.Companion var10000 = SightSpec.Companion;
            JsonObject var10002 = el.getAsJsonObject();
            Intrinsics.checkNotNullExpressionValue(var10002, "getAsJsonObject(...)");
            SightSpec spec = var10000.parse(id, var10002);
            loaded++;
            if (spec.getUsedBy().isEmpty()) {
               LOG.warn("warsteel sight {} has no UsedBy entries, it will never be used", id);
            }

            Iterable $this$forEach$iv = spec.getUsedBy();

            for (Object element$ivx : $this$forEach$iv) {
               String vehicle = (String)element$ivx;
               SightSpec previous = out.put(vehicle, spec);
               if (previous != null) {
                  Logger var24 = LOG;
                  Object[] var22 = new Object[]{id, previous.getId(), vehicle};
                  var24.warn("warsteel sight {} overrides {} for vehicle {} - only one sight per vehicle is used", var22);
               }
            }
         } catch (Throwable var23) {
            LOG.error("failed to read warsteel sight {}", id, var23);
         }
      }

      byVehicle = out;
      SightBallistics.INSTANCE.clearCache();
      SightRender.INSTANCE.clearTextureCache();
      LOG.info("loaded {} warsteel sights covering {} vehicles", loaded, out.size());
   }
}
