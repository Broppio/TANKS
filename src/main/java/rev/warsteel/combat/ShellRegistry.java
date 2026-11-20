package rev.rev.warsteel.combat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

@SourceDebugExtension({"SMAP\nRicochetHandler.kt\nKotlin\n*S Kotlin\n*F\n+ 1 RicochetHandler.kt\nrev/rev/warsteel/combat/ShellRegistry\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 3 _Maps.kt\nkotlin/collections/MapsKt___MapsKt\n*L\n1#1,975:1\n774#2:976\n865#2,2:977\n2341#2,14:979\n774#2:993\n865#2,2:994\n2341#2,14:996\n216#3,2:1010\n*S KotlinDebug\n*F\n+ 1 RicochetHandler.kt\nrev/rev/warsteel/combat/ShellRegistry\n*L\n341#1:976\n341#1:977,2\n342#1:979,14\n361#1:993\n361#1:994,2\n362#1:996,14\n367#1:1010,2\n*E\n"})
public final class ShellRegistry extends SimpleJsonResourceReloadListener {
   @NotNull
   public static final ShellRegistry INSTANCE = new ShellRegistry();
   @NotNull
   private static Map<ResourceLocation, ShellData> shells = MapsKt.emptyMap();

   private ShellRegistry() {
      super(RicochetHandlerKt.access$getGSON$p(), "warsteel/shells");
   }

   /**
    * Picks the profile for a shell: first the firing vehicle's own shells of the same family
    * (AP-type vs HE-type, which SBW tells us from the shell entity), then any of that vehicle's
    * shells, nearest Damage breaking ties. Returns null for vehicles with no WarSteel shells.
    */
   public java.util.Set<ResourceLocation> ids() {
      return shells.keySet();
   }

   @Nullable
   public ShellData byId(ResourceLocation id) {
      return shells.get(id);
   }

   @Nullable
   public final ShellData resolve(@Nullable String vehicleId, double damage, boolean armourPiercing) {
      if (vehicleId == null) {
         return null;
      }

      ShellData best = null;
      double bestScore = Double.POSITIVE_INFINITY;

      for (ShellData shell : shells.values()) {
         if (shell.getUsedBy().contains(vehicleId)) {
            double score = Math.abs(shell.getDamage() - damage) + (shell.isArmourPiercing() == armourPiercing ? 0.0 : 1.0E6);
            if (score < bestScore) {
               best = shell;
               bestScore = score;
            }
         }
      }

      return best;
   }

   protected void apply(@NotNull Map<ResourceLocation, JsonElement> map, @NotNull ResourceManager rm, @NotNull ProfilerFiller p) {
      HashMap out = new HashMap();

      for (Entry element$iv : map.entrySet()) {
         ResourceLocation id = (ResourceLocation)element$iv.getKey();
         JsonElement el = (JsonElement)element$iv.getValue();

         try {
            Map t = out;
            JsonObject var10001 = el.getAsJsonObject();
            Intrinsics.checkNotNullExpressionValue(var10001, "getAsJsonObject(...)");
            ShellData var14 = RicochetHandlerKt.access$parseShell(id, var10001);
            t.put(id, var14);
         } catch (Throwable var15) {
            RicochetHandlerKt.access$getLOG$p().error("failed to read shell {}", id, var15);
         }
      }

      shells = out;
      RicochetHandlerKt.access$getLOG$p().info("loaded {} warsteel shells: {}", out.size(), out.keySet());
   }
}
