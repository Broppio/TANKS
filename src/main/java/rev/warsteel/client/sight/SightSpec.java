package rev.rev.warsteel.client.sight;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.RangesKt;
import kotlin.text.StringsKt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SourceDebugExtension({"SMAP\nSightData.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SightData.kt\nrev/rev/warsteel/client/sight/SightSpec\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,191:1\n295#2,2:192\n*S KotlinDebug\n*F\n+ 1 SightData.kt\nrev/rev/warsteel/client/sight/SightSpec\n*L\n140#1:192,2\n*E\n"})
public final class SightSpec {
   @NotNull
   public static final SightSpec.Companion Companion = new SightSpec.Companion(null);
   @NotNull
   private final ResourceLocation id;
   @NotNull
   private final Set<String> usedBy;
   @NotNull
   private final String template;
   @NotNull
   private final String weapon;
   @NotNull
   private final String mgTemplate;
   @NotNull
   private final String commanderTemplate;
   @Nullable
   private final ResourceLocation mask;
   @Nullable
   private final ResourceLocation commanderMask;
   @Nullable
   private final ResourceLocation furniture;
   @NotNull
   private final FurnitureAnchor furnitureAnchor;
   @NotNull
   private final String designation;
   private final int reticleColor;
   private final float apertureScale;
   private final float viewportWidth;
   private final float viewportAspect;
   private final float viewportCorner;
   @NotNull
   private final List<LadderSpec> ladders;
   private final float arcRadius;
   private final float arcStartDeg;
   private final float arcEndDeg;
   private final boolean targetMarker;
   @NotNull
   public static final String DEFAULT_COMMANDER = "binocular";
   @NotNull
   public static final String DEFAULT_MG = "mg_cross";

   public SightSpec(
      @NotNull ResourceLocation id,
      @NotNull Set<String> usedBy,
      @NotNull String template,
      @NotNull String weapon,
      @NotNull String mgTemplate,
      @NotNull String commanderTemplate,
      @Nullable ResourceLocation mask,
      @Nullable ResourceLocation commanderMask,
      @Nullable ResourceLocation furniture,
      @NotNull FurnitureAnchor furnitureAnchor,
      @NotNull String designation,
      int reticleColor,
      float apertureScale,
      float viewportWidth,
      float viewportAspect,
      float viewportCorner,
      @NotNull List<LadderSpec> ladders,
      float arcRadius,
      float arcStartDeg,
      float arcEndDeg,
      boolean targetMarker
   ) {
      super();
      this.id = id;
      this.usedBy = usedBy;
      this.template = template;
      this.weapon = weapon;
      this.mgTemplate = mgTemplate;
      this.commanderTemplate = commanderTemplate;
      this.mask = mask;
      this.commanderMask = commanderMask;
      this.furniture = furniture;
      this.furnitureAnchor = furnitureAnchor;
      this.designation = designation;
      this.reticleColor = reticleColor;
      this.apertureScale = apertureScale;
      this.viewportWidth = viewportWidth;
      this.viewportAspect = viewportAspect;
      this.viewportCorner = viewportCorner;
      this.ladders = ladders;
      this.arcRadius = arcRadius;
      this.arcStartDeg = arcStartDeg;
      this.arcEndDeg = arcEndDeg;
      this.targetMarker = targetMarker;
   }

   @NotNull
   public final ResourceLocation getId() {
      return this.id;
   }

   @NotNull
   public final Set<String> getUsedBy() {
      return this.usedBy;
   }

   @NotNull
   public final String getTemplate() {
      return this.template;
   }

   @NotNull
   public final String getWeapon() {
      return this.weapon;
   }

   @NotNull
   public final String getMgTemplate() {
      return this.mgTemplate;
   }

   @NotNull
   public final String getCommanderTemplate() {
      return this.commanderTemplate;
   }

   @Nullable
   public final ResourceLocation getMask() {
      return this.mask;
   }

   @Nullable
   public final ResourceLocation getCommanderMask() {
      return this.commanderMask;
   }

   @Nullable
   public final ResourceLocation getFurniture() {
      return this.furniture;
   }

   @NotNull
   public final FurnitureAnchor getFurnitureAnchor() {
      return this.furnitureAnchor;
   }

   @NotNull
   public final String getDesignation() {
      return this.designation;
   }

   public final int getReticleColor() {
      return this.reticleColor;
   }

   public final float getApertureScale() {
      return this.apertureScale;
   }

   public final float getViewportWidth() {
      return this.viewportWidth;
   }

   public final float getViewportAspect() {
      return this.viewportAspect;
   }

   public final float getViewportCorner() {
      return this.viewportCorner;
   }

   @NotNull
   public final List<LadderSpec> getLadders() {
      return this.ladders;
   }

   public final float getArcRadius() {
      return this.arcRadius;
   }

   public final float getArcStartDeg() {
      return this.arcStartDeg;
   }

   public final float getArcEndDeg() {
      return this.arcEndDeg;
   }

   public final boolean getTargetMarker() {
      return this.targetMarker;
   }

   @Nullable
   public final LadderSpec ladderFor(@NotNull LadderSide side) {
      Iterable $this$firstOrNull$iv = this.ladders;
      Iterator var4 = $this$firstOrNull$iv.iterator();

      Object var10000;
      while (true) {
         if (var4.hasNext()) {
            Object element$iv = var4.next();
            LadderSpec it = (LadderSpec)element$iv;
            if (it.getSide() != side) {
               continue;
            }

            var10000 = element$iv;
            break;
         }

         var10000 = null;
         break;
      }

      return (LadderSpec)var10000;
   }

   @NotNull
   public final ResourceLocation component1() {
      return this.id;
   }

   @NotNull
   public final Set<String> component2() {
      return this.usedBy;
   }

   @NotNull
   public final String component3() {
      return this.template;
   }

   @NotNull
   public final String component4() {
      return this.weapon;
   }

   @NotNull
   public final String component5() {
      return this.mgTemplate;
   }

   @NotNull
   public final String component6() {
      return this.commanderTemplate;
   }

   @Nullable
   public final ResourceLocation component7() {
      return this.mask;
   }

   @Nullable
   public final ResourceLocation component8() {
      return this.commanderMask;
   }

   @Nullable
   public final ResourceLocation component9() {
      return this.furniture;
   }

   @NotNull
   public final FurnitureAnchor component10() {
      return this.furnitureAnchor;
   }

   @NotNull
   public final String component11() {
      return this.designation;
   }

   public final int component12() {
      return this.reticleColor;
   }

   public final float component13() {
      return this.apertureScale;
   }

   public final float component14() {
      return this.viewportWidth;
   }

   public final float component15() {
      return this.viewportAspect;
   }

   public final float component16() {
      return this.viewportCorner;
   }

   @NotNull
   public final List<LadderSpec> component17() {
      return this.ladders;
   }

   public final float component18() {
      return this.arcRadius;
   }

   public final float component19() {
      return this.arcStartDeg;
   }

   public final float component20() {
      return this.arcEndDeg;
   }

   public final boolean component21() {
      return this.targetMarker;
   }

   @NotNull
   public final SightSpec copy(
      @NotNull ResourceLocation id,
      @NotNull Set<String> usedBy,
      @NotNull String template,
      @NotNull String weapon,
      @NotNull String mgTemplate,
      @NotNull String commanderTemplate,
      @Nullable ResourceLocation mask,
      @Nullable ResourceLocation commanderMask,
      @Nullable ResourceLocation furniture,
      @NotNull FurnitureAnchor furnitureAnchor,
      @NotNull String designation,
      int reticleColor,
      float apertureScale,
      float viewportWidth,
      float viewportAspect,
      float viewportCorner,
      @NotNull List<LadderSpec> ladders,
      float arcRadius,
      float arcStartDeg,
      float arcEndDeg,
      boolean targetMarker
   ) {
      return new SightSpec(
         id,
         usedBy,
         template,
         weapon,
         mgTemplate,
         commanderTemplate,
         mask,
         commanderMask,
         furniture,
         furnitureAnchor,
         designation,
         reticleColor,
         apertureScale,
         viewportWidth,
         viewportAspect,
         viewportCorner,
         ladders,
         arcRadius,
         arcStartDeg,
         arcEndDeg,
         targetMarker
      );
   }

   // $VF: synthetic method
   public static SightSpec copy$default(
      SightSpec var0,
      ResourceLocation var1,
      Set var2,
      String var3,
      String var4,
      String var5,
      String var6,
      ResourceLocation var7,
      ResourceLocation var8,
      ResourceLocation var9,
      FurnitureAnchor var10,
      String var11,
      int var12,
      float var13,
      float var14,
      float var15,
      float var16,
      List var17,
      float var18,
      float var19,
      float var20,
      boolean var21,
      int var22,
      Object var23
   ) {
      if ((var22 & 1) != 0) {
         var1 = var0.id;
      }

      if ((var22 & 2) != 0) {
         var2 = var0.usedBy;
      }

      if ((var22 & 4) != 0) {
         var3 = var0.template;
      }

      if ((var22 & 8) != 0) {
         var4 = var0.weapon;
      }

      if ((var22 & 16) != 0) {
         var5 = var0.mgTemplate;
      }

      if ((var22 & 32) != 0) {
         var6 = var0.commanderTemplate;
      }

      if ((var22 & 64) != 0) {
         var7 = var0.mask;
      }

      if ((var22 & 128) != 0) {
         var8 = var0.commanderMask;
      }

      if ((var22 & 256) != 0) {
         var9 = var0.furniture;
      }

      if ((var22 & 512) != 0) {
         var10 = var0.furnitureAnchor;
      }

      if ((var22 & 1024) != 0) {
         var11 = var0.designation;
      }

      if ((var22 & 2048) != 0) {
         var12 = var0.reticleColor;
      }

      if ((var22 & 4096) != 0) {
         var13 = var0.apertureScale;
      }

      if ((var22 & 8192) != 0) {
         var14 = var0.viewportWidth;
      }

      if ((var22 & 16384) != 0) {
         var15 = var0.viewportAspect;
      }

      if ((var22 & 32768) != 0) {
         var16 = var0.viewportCorner;
      }

      if ((var22 & 65536) != 0) {
         var17 = var0.ladders;
      }

      if ((var22 & 131072) != 0) {
         var18 = var0.arcRadius;
      }

      if ((var22 & 262144) != 0) {
         var19 = var0.arcStartDeg;
      }

      if ((var22 & 524288) != 0) {
         var20 = var0.arcEndDeg;
      }

      if ((var22 & 1048576) != 0) {
         var21 = var0.targetMarker;
      }

      return var0.copy(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, var13, var14, var15, var16, var17, var18, var19, var20, var21);
   }

   @NotNull
   @Override
   public String toString() {
      return "SightSpec(id="
         + this.id
         + ", usedBy="
         + this.usedBy
         + ", template="
         + this.template
         + ", weapon="
         + this.weapon
         + ", mgTemplate="
         + this.mgTemplate
         + ", commanderTemplate="
         + this.commanderTemplate
         + ", mask="
         + this.mask
         + ", commanderMask="
         + this.commanderMask
         + ", furniture="
         + this.furniture
         + ", furnitureAnchor="
         + this.furnitureAnchor
         + ", designation="
         + this.designation
         + ", reticleColor="
         + this.reticleColor
         + ", apertureScale="
         + this.apertureScale
         + ", viewportWidth="
         + this.viewportWidth
         + ", viewportAspect="
         + this.viewportAspect
         + ", viewportCorner="
         + this.viewportCorner
         + ", ladders="
         + this.ladders
         + ", arcRadius="
         + this.arcRadius
         + ", arcStartDeg="
         + this.arcStartDeg
         + ", arcEndDeg="
         + this.arcEndDeg
         + ", targetMarker="
         + this.targetMarker
         + ")";
   }

   @Override
   public int hashCode() {
      int result = this.id.hashCode();
      result = result * 31 + this.usedBy.hashCode();
      result = result * 31 + this.template.hashCode();
      result = result * 31 + this.weapon.hashCode();
      result = result * 31 + this.mgTemplate.hashCode();
      result = result * 31 + this.commanderTemplate.hashCode();
      result = result * 31 + (this.mask == null ? 0 : this.mask.hashCode());
      result = result * 31 + (this.commanderMask == null ? 0 : this.commanderMask.hashCode());
      result = result * 31 + (this.furniture == null ? 0 : this.furniture.hashCode());
      result = result * 31 + this.furnitureAnchor.hashCode();
      result = result * 31 + this.designation.hashCode();
      result = result * 31 + Integer.hashCode(this.reticleColor);
      result = result * 31 + Float.hashCode(this.apertureScale);
      result = result * 31 + Float.hashCode(this.viewportWidth);
      result = result * 31 + Float.hashCode(this.viewportAspect);
      result = result * 31 + Float.hashCode(this.viewportCorner);
      result = result * 31 + this.ladders.hashCode();
      result = result * 31 + Float.hashCode(this.arcRadius);
      result = result * 31 + Float.hashCode(this.arcStartDeg);
      result = result * 31 + Float.hashCode(this.arcEndDeg);
      return result * 31 + Boolean.hashCode(this.targetMarker);
   }

   @Override
   public boolean equals(@Nullable Object other) {
      if (this == other) {
         return true;
      } else if (!(other instanceof SightSpec var2)) {
         return false;
      } else if (!Intrinsics.areEqual(this.id, var2.id)) {
         return false;
      } else if (!Intrinsics.areEqual(this.usedBy, var2.usedBy)) {
         return false;
      } else if (!Intrinsics.areEqual(this.template, var2.template)) {
         return false;
      } else if (!Intrinsics.areEqual(this.weapon, var2.weapon)) {
         return false;
      } else if (!Intrinsics.areEqual(this.mgTemplate, var2.mgTemplate)) {
         return false;
      } else if (!Intrinsics.areEqual(this.commanderTemplate, var2.commanderTemplate)) {
         return false;
      } else if (!Intrinsics.areEqual(this.mask, var2.mask)) {
         return false;
      } else if (!Intrinsics.areEqual(this.commanderMask, var2.commanderMask)) {
         return false;
      } else if (!Intrinsics.areEqual(this.furniture, var2.furniture)) {
         return false;
      } else if (this.furnitureAnchor != var2.furnitureAnchor) {
         return false;
      } else if (!Intrinsics.areEqual(this.designation, var2.designation)) {
         return false;
      } else if (this.reticleColor != var2.reticleColor) {
         return false;
      } else if (Float.compare(this.apertureScale, var2.apertureScale) != 0) {
         return false;
      } else if (Float.compare(this.viewportWidth, var2.viewportWidth) != 0) {
         return false;
      } else if (Float.compare(this.viewportAspect, var2.viewportAspect) != 0) {
         return false;
      } else if (Float.compare(this.viewportCorner, var2.viewportCorner) != 0) {
         return false;
      } else if (!Intrinsics.areEqual(this.ladders, var2.ladders)) {
         return false;
      } else if (Float.compare(this.arcRadius, var2.arcRadius) != 0) {
         return false;
      } else if (Float.compare(this.arcStartDeg, var2.arcStartDeg) != 0) {
         return false;
      } else {
         return Float.compare(this.arcEndDeg, var2.arcEndDeg) != 0 ? false : this.targetMarker == var2.targetMarker;
      }
   }

   @SourceDebugExtension({"SMAP\nSightData.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SightData.kt\nrev/rev/warsteel/client/sight/SightSpec$Companion\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,191:1\n1863#2,2:192\n1863#2,2:194\n*S KotlinDebug\n*F\n+ 1 SightData.kt\nrev/rev/warsteel/client/sight/SightSpec$Companion\n*L\n153#1:192,2\n157#1:194,2\n*E\n"})
   public static final class Companion {
      private Companion() {
      }

      private final ResourceLocation loc(JsonObject o, String key) {
         return o.has(key) && !o.get(key).isJsonNull() ? ResourceLocation.tryParse(GsonHelper.getAsString(o, key)) : null;
      }

      @NotNull
      public final SightSpec parse(@NotNull ResourceLocation id, @NotNull JsonObject o) {
         HashSet used = new HashSet();
         if (o.has("UsedBy")) {
            JsonArray var10000 = GsonHelper.getAsJsonArray(o, "UsedBy");
            Intrinsics.checkNotNullExpressionValue(var10000, "getAsJsonArray(...)");
            Iterable $this$forEach$iv = (Iterable)var10000;

            for (Object element$iv : $this$forEach$iv) {
               JsonElement it = (JsonElement)element$iv;
               used.add(it.getAsString());
            }
         }

         ArrayList ladders = new ArrayList();
         if (o.has("Ladders")) {
            JsonArray var57 = GsonHelper.getAsJsonArray(o, "Ladders");
            Intrinsics.checkNotNullExpressionValue(var57, "getAsJsonArray(...)");
            Iterable var44 = (Iterable)var57;

            for (Object element$iv : var44) {
               JsonElement it = (JsonElement)element$iv;
               LadderSpec.Companion var10001 = LadderSpec.Companion;
               JsonObject var10002 = it.getAsJsonObject();
               Intrinsics.checkNotNullExpressionValue(var10002, "getAsJsonObject(...)");
               ladders.add(var10001.parse(var10002));
            }
         }

         String colorStr = StringsKt.removePrefix(
            StringsKt.removePrefix(StringsKt.removePrefix(SightDataKt.access$optString(o, "ReticleColor", "0x000000"), "0x"), "0X"), "#"
         );
         Integer var58 = StringsKt.toIntOrNull(colorStr, 16);
         int color = var58 != null ? var58 : 0;
         Set var60 = used;
         String var61 = SightDataKt.access$optString(o, "Template", "binocular");
         String var10003 = SightDataKt.access$optString(o, "Weapon", "Cannon");
         String var10004 = SightDataKt.access$optString(o, "MgTemplate", "mg_cross");
         String var10005 = SightDataKt.access$optString(o, "CommanderTemplate", "binocular");
         ResourceLocation var10006 = this.loc(o, "Mask");
         ResourceLocation var10007 = this.loc(o, "CommanderMask");
         ResourceLocation var10008 = this.loc(o, "Furniture");
         SightSpec.Companion var49 = this;
         ResourceLocation var19 = var10008;
         ResourceLocation var18 = var10007;
         ResourceLocation var17 = var10006;
         String var16 = var10005;
         String var15 = var10004;
         String var14 = var10003;
         String var13 = var61;
         Set var12 = var60;

         FurnitureAnchor var10009;
         try {
            var10009 = FurnitureAnchor.valueOf(SightDataKt.access$optString(o, "FurnitureAnchor", "APERTURE").toUpperCase(Locale.ROOT));
         } catch (Throwable var42) {
            var10009 = FurnitureAnchor.APERTURE;
         }
         String var10010 = SightDataKt.access$optString(o, "Designation", "");
         float var10012 = RangesKt.coerceIn(GsonHelper.getAsFloat(o, "ApertureScale", 0.72F), 0.2F, 1.0F);
         float var10013 = RangesKt.coerceIn(GsonHelper.getAsFloat(o, "ViewportWidth", 0.78F), 0.2F, 1.0F);
         float var10014 = RangesKt.coerceIn(GsonHelper.getAsFloat(o, "ViewportAspect", 3.4F), 1.0F, 8.0F);
         float var10015 = RangesKt.coerceIn(GsonHelper.getAsFloat(o, "ViewportCorner", 0.22F), 0.0F, 0.5F);
         List var10016 = ladders;
         float var10017 = GsonHelper.getAsFloat(o, "ArcRadius", 0.74F);
         float var10018 = GsonHelper.getAsFloat(o, "ArcStartDeg", 208.0F);
         float var10019 = GsonHelper.getAsFloat(o, "ArcEndDeg", 332.0F);
         boolean var21 = GsonHelper.getAsBoolean(o, "TargetMarker", true);
         float var22 = var10019;
         float var23 = var10018;
         float var24 = var10017;
         List var25 = var10016;
         float var26 = var10015;
         float var27 = var10014;
         float var28 = var10013;
         float var29 = var10012;
         String var31 = var10010;
         FurnitureAnchor var32 = var10009;
         return new SightSpec(
            id, var12, var13, var14, var15, var16, var17, var18, var19, var32, var31, color, var29, var28, var27, var26, var25, var24, var23, var22, var21
         );
      }

      // $VF: synthetic method
      public Companion(DefaultConstructorMarker $constructor_marker) {
         this();
      }
   }
}
