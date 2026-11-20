package rev.rev.warsteel.combat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SourceDebugExtension({"SMAP\nRicochetHandler.kt\nKotlin\n*S Kotlin\n*F\n+ 1 RicochetHandler.kt\nrev/rev/warsteel/combat/ArmorRegistry\n+ 2 _Maps.kt\nkotlin/collections/MapsKt___MapsKt\n*L\n1#1,975:1\n216#2,2:976\n*S KotlinDebug\n*F\n+ 1 RicochetHandler.kt\nrev/rev/warsteel/combat/ArmorRegistry\n*L\n386#1:976,2\n*E\n"})
public final class ArmorRegistry extends SimpleJsonResourceReloadListener {
   @NotNull
   public static final ArmorRegistry INSTANCE = new ArmorRegistry();
   @NotNull
   private static Map<ResourceLocation, ArmorData> byVehicle = MapsKt.emptyMap();

   private ArmorRegistry() {
      super(RicochetHandlerKt.access$getGSON$p(), "warsteel/armor");
   }

   @Nullable
   public final ArmorData get(@NotNull ResourceLocation vehicleType) {
      return byVehicle.get(vehicleType);
   }

   protected void apply(@NotNull Map<ResourceLocation, JsonElement> map, @NotNull ResourceManager rm, @NotNull ProfilerFiller p) {
      HashMap out = new HashMap();

      for (Entry element$iv : map.entrySet()) {
         ResourceLocation id = (ResourceLocation)element$iv.getKey();
         JsonElement el = (JsonElement)element$iv.getValue();

         try {
            JsonObject var10000 = el.getAsJsonObject();
            Intrinsics.checkNotNullExpressionValue(var10000, "getAsJsonObject(...)");
            ArmorData a = RicochetHandlerKt.access$parseArmor(var10000);
            out.put(a.getVehicle(), a);
         } catch (Throwable var14) {
            RicochetHandlerKt.access$getLOG$p().error("failed to read armor {}", id, var14);
         }
      }

      byVehicle = out;
      RicochetHandlerKt.access$getLOG$p().info("loaded armour for {} vehicles: {}", out.size(), out.keySet());
   }
}
