package rev.rev.warsteel.combat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.text.StringsKt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SourceDebugExtension({"SMAP\nRicochetHandler.kt\nKotlin\n*S Kotlin\n*F\n+ 1 RicochetHandler.kt\nrev/rev/warsteel/combat/RicochetHandlerKt\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,975:1\n1863#2,2:976\n1863#2:978\n1557#2:979\n1628#2,3:980\n1557#2:983\n1628#2,3:984\n1864#2:987\n*S KotlinDebug\n*F\n+ 1 RicochetHandler.kt\nrev/rev/warsteel/combat/RicochetHandlerKt\n*L\n231#1:976,2\n298#1:978\n304#1:979\n304#1:980,3\n305#1:983\n305#1:984,3\n298#1:987\n*E\n"})
public final class RicochetHandlerKt {
   @NotNull
   private static final Gson GSON = new Gson();
   private static final Logger LOG = LoggerFactory.getLogger("warsteel/ricochet");
   private static final double PLATE_ATTRIBUTION_RANGE = 1.25;
   private static final double MIN_COS_INCIDENCE = 0.087;
   private static final double SOUND_RANGE_BLOCKS = 112.0;
   @NotNull
   private static final SoundSource IMPACT_SOUND_CATEGORY = SoundSource.PLAYERS;
   private static final long REHIT_IMMUNITY_TICKS = 6L;
   private static final double SPENT_SPEED_RETAINED = 0.28;

   private static final float volumeForRange(double blocks) {
      return (float)(blocks / 16.0);
   }

   private static final String str(JsonObject o, String key, String fallback) {
      String var10000 = GsonHelper.getAsString(o, key, fallback);
      if (var10000 == null) {
         var10000 = fallback;
      }

      return var10000;
   }

   static ShellData parseShell(ResourceLocation id, JsonObject o) {
      JsonObject pen = o.getAsJsonObject("Penetration");
      JsonObject ric = o.getAsJsonObject("Ricochet");
      HashSet used = new HashSet();
      if (o.has("UsedBy")) {
         JsonArray var10000 = o.getAsJsonArray("UsedBy");
         Intrinsics.checkNotNullExpressionValue(var10000, "getAsJsonArray(...)");
         Iterable $this$forEach$iv = (Iterable)var10000;

         for (Object element$iv : $this$forEach$iv) {
            JsonElement it = (JsonElement)element$iv;
            used.add(it.getAsString());
         }
      }

      String shellClass = str(o, "Class", "AP");
      String var10005 = id.getPath();
      Intrinsics.checkNotNullExpressionValue(var10005, "getPath(...)");
      return new ShellData(
         id,
         str(o, "Name", var10005),
         shellClass,
         GsonHelper.getAsInt(o, "Calibre", 0),
         GsonHelper.getAsDouble(o, "Velocity", 20.0),
         GsonHelper.getAsDouble(o, "Damage", 0.0),
         new Penetration(GsonHelper.getAsDouble(pen, "Muzzle", 0.0), GsonHelper.getAsDouble(pen, "DropoffPerBlock", 0.0), GsonHelper.getAsDouble(pen, "Minimum", 0.0)),
         new RicochetProfile(
            GsonHelper.getAsBoolean(ric, "Enabled", true),
            GsonHelper.getAsDouble(ric, "MinAngle", 70.0),
            GsonHelper.getAsDouble(ric, "AlwaysAngle", 84.0),
            GsonHelper.getAsDouble(ric, "OvermatchRatio", 2.0),
            GsonHelper.getAsDouble(ric, "SpeedRetained", 0.55),
            GsonHelper.getAsDouble(ric, "ScatterDegrees", 6.0)
         ),
         used,
         GsonHelper.getAsBoolean(o, "DefeatedByArmour", !shellClass.equalsIgnoreCase("HE") && !shellClass.equalsIgnoreCase("HESH") && !shellClass.equalsIgnoreCase("FLAK")),
         GsonHelper.getAsDouble(o, "Filler", 0.0),
         GsonHelper.getAsDouble(o, "FuzeDelay", 1.2),
         GsonHelper.getAsDouble(o, "FuzeSensitivity", 15.0),
         GsonHelper.getAsDouble(o, "Mass", 0.0)
      ).withFlakFuze(GsonHelper.getAsInt(o, "FuzeTicks", 0), GsonHelper.getAsDouble(o, "ProximityRange", 0.0));
   }

   static ArmorData parseArmor(JsonObject o) {
      ArrayList plates = new ArrayList();
      JsonArray var10000 = o.getAsJsonArray("Plates");
      Intrinsics.checkNotNullExpressionValue(var10000, "getAsJsonArray(...)");
      Iterable $this$forEach$iv = (Iterable)var10000;

      for (Object element$iv : $this$forEach$iv) {
         JsonElement e = (JsonElement)element$iv;
         JsonObject p = e.getAsJsonObject();
         Intrinsics.checkNotNull(p);
         String var10001 = str(p, "Name", "?");
         String var10002 = str(p, "Transform", "Vehicle");
         JsonArray var10003 = p.getAsJsonArray("Size");
         Intrinsics.checkNotNullExpressionValue(var10003, "getAsJsonArray(...)");
         Iterable $this$map$iv = (Iterable)var10003;
         String var10 = var10002;
         String var11 = var10001;
         var destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv, 10));

         for (Object item$iv$iv : $this$map$iv) {
            JsonElement it = (JsonElement)item$iv$iv;
            destination$iv$iv.add(it.getAsDouble());
         }

         List var20 = (List)destination$iv$iv;
         double[] var42 = CollectionsKt.toDoubleArray(var20);
         JsonArray var10004 = p.getAsJsonArray("Position");
         Intrinsics.checkNotNullExpressionValue(var10004, "getAsJsonArray(...)");
         $this$map$iv = (Iterable)var10004;
         double[] var39 = var42;
         destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv, 10));

         for (Object item$iv$iv : $this$map$iv) {
            JsonElement var38 = (JsonElement)item$iv$iv;
            destination$iv$iv.add(var38.getAsDouble());
         }

         List var22 = (List)destination$iv$iv;
         double[] var43 = CollectionsKt.toDoubleArray(var22);
         double var10005 = GsonHelper.getAsDouble(p, "Thickness", 50.0);
         double var23 = GsonHelper.getAsDouble(p, "RicochetChance", 1.0);
         double var25 = var10005;
         double[] var27 = var43;
         double[] normal = null;
         if (p.has("Normal")) {
            JsonArray n = p.getAsJsonArray("Normal");
            normal = new double[]{n.get(0).getAsDouble(), n.get(1).getAsDouble(), n.get(2).getAsDouble()};
         }

         plates.add(new ArmorPlate(var11, var10, var39, var27, var25, var23, normal));
      }

      String var41 = GsonHelper.getAsString(o, "Vehicle");
      Intrinsics.checkNotNullExpressionValue(var41, "getAsString(...)");
      String vehicleId = var41;
      return new ArmorData(ResourceLocation.parse(vehicleId), GsonHelper.getAsDouble(o, "GeneralRicochetChance", 1.0), plates);
   }

   // $VF: synthetic method
   public static final Gson access$getGSON$p() {
      return GSON;
   }

   // $VF: synthetic method
   public static final Logger access$getLOG$p() {
      return LOG;
   }

   // $VF: synthetic method
   public static final ShellData access$parseShell(ResourceLocation id, JsonObject o) {
      return parseShell(id, o);
   }

   // $VF: synthetic method
   public static final ArmorData access$parseArmor(JsonObject o) {
      return parseArmor(o);
   }

   // $VF: synthetic method
   public static final float access$volumeForRange(double blocks) {
      return volumeForRange(blocks);
   }

   // $VF: synthetic method
   public static final SoundSource access$getIMPACT_SOUND_CATEGORY$p() {
      return IMPACT_SOUND_CATEGORY;
   }
}
