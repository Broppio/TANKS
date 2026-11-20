package rev.rev.warsteel.client.sight;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.RangesKt;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LadderSpec {
   @NotNull
   public static final LadderSpec.Companion Companion = new LadderSpec.Companion(null);
   @NotNull
   private final String label;
   private final double velocity;
   @NotNull
   private final LadderSide side;
   @NotNull
   private final List<Integer> stops;
   private final int majorEvery;

   public LadderSpec(@NotNull String label, double velocity, @NotNull LadderSide side, @NotNull List<Integer> stops, int majorEvery) {
      super();
      this.label = label;
      this.velocity = velocity;
      this.side = side;
      this.stops = stops;
      this.majorEvery = majorEvery;
   }

   @NotNull
   public final String getLabel() {
      return this.label;
   }

   public final double getVelocity() {
      return this.velocity;
   }

   @NotNull
   public final LadderSide getSide() {
      return this.side;
   }

   @NotNull
   public final List<Integer> getStops() {
      return this.stops;
   }

   public final int getMajorEvery() {
      return this.majorEvery;
   }

   @NotNull
   public final String component1() {
      return this.label;
   }

   public final double component2() {
      return this.velocity;
   }

   @NotNull
   public final LadderSide component3() {
      return this.side;
   }

   @NotNull
   public final List<Integer> component4() {
      return this.stops;
   }

   public final int component5() {
      return this.majorEvery;
   }

   @NotNull
   public final LadderSpec copy(@NotNull String label, double velocity, @NotNull LadderSide side, @NotNull List<Integer> stops, int majorEvery) {
      return new LadderSpec(label, velocity, side, stops, majorEvery);
   }

   // $VF: synthetic method
   public static LadderSpec copy$default(LadderSpec var0, String var1, double var2, LadderSide var4, List var5, int var6, int var7, Object var8) {
      if ((var7 & 1) != 0) {
         var1 = var0.label;
      }

      if ((var7 & 2) != 0) {
         var2 = var0.velocity;
      }

      if ((var7 & 4) != 0) {
         var4 = var0.side;
      }

      if ((var7 & 8) != 0) {
         var5 = var0.stops;
      }

      if ((var7 & 16) != 0) {
         var6 = var0.majorEvery;
      }

      return var0.copy(var1, var2, var4, var5, var6);
   }

   @NotNull
   @Override
   public String toString() {
      return "LadderSpec(label="
         + this.label
         + ", velocity="
         + this.velocity
         + ", side="
         + this.side
         + ", stops="
         + this.stops
         + ", majorEvery="
         + this.majorEvery
         + ")";
   }

   @Override
   public int hashCode() {
      int result = this.label.hashCode();
      result = result * 31 + Double.hashCode(this.velocity);
      result = result * 31 + this.side.hashCode();
      result = result * 31 + this.stops.hashCode();
      return result * 31 + Integer.hashCode(this.majorEvery);
   }

   @Override
   public boolean equals(@Nullable Object other) {
      if (this == other) {
         return true;
      } else if (!(other instanceof LadderSpec var2)) {
         return false;
      } else if (!Intrinsics.areEqual(this.label, var2.label)) {
         return false;
      } else if (Double.compare(this.velocity, var2.velocity) != 0) {
         return false;
      } else if (this.side != var2.side) {
         return false;
      } else {
         return !Intrinsics.areEqual(this.stops, var2.stops) ? false : this.majorEvery == var2.majorEvery;
      }
   }

   @SourceDebugExtension({"SMAP\nSightData.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SightData.kt\nrev/rev/warsteel/client/sight/LadderSpec$Companion\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,191:1\n1863#2,2:192\n1#3:194\n*S KotlinDebug\n*F\n+ 1 SightData.kt\nrev/rev/warsteel/client/sight/LadderSpec$Companion\n*L\n60#1:192,2\n*E\n"})
   public static final class Companion {
      private Companion() {
      }

      @NotNull
      public final LadderSpec parse(@NotNull JsonObject o) {
         ArrayList stops = new ArrayList();
         JsonArray var10000 = GsonHelper.getAsJsonArray(o, "Stops");
         Intrinsics.checkNotNullExpressionValue(var10000, "getAsJsonArray(...)");
         Iterable $this$forEach$iv = (Iterable)var10000;

         for (Object element$iv : $this$forEach$iv) {
            JsonElement it = (JsonElement)element$iv;
            stops.add(it.getAsInt());
         }

         if (stops.isEmpty()) {
            String var26 = "ladder \"" + SightDataKt.access$optString(o, "Label", "?") + "\" has no Stops";
            throw new IllegalArgumentException(var26.toString());
         } else {
            String var28 = SightDataKt.access$optString(o, "Label", "");
            double var10001 = GsonHelper.getAsDouble(o, "Velocity");
            LadderSpec.Companion var20 = this;
            double var10 = var10001;
            String var9 = var28;

            LadderSide var10002;
            try {
               var10002 = LadderSide.valueOf(SightDataKt.access$optString(o, "Side", "LEFT").toUpperCase(Locale.ROOT));
            } catch (Throwable var19) {
               var10002 = LadderSide.LEFT;
            }

            List var10003 = CollectionsKt.sorted(stops);
            int var13 = RangesKt.coerceAtLeast(GsonHelper.getAsInt(o, "MajorEvery", 1), 1);
            List var14 = var10003;
            LadderSide var15 = var10002;
            return new LadderSpec(var9, var10, var15, var14, var13);
         }
      }

      // $VF: synthetic method
      public Companion(DefaultConstructorMarker $constructor_marker) {
         this();
      }
   }
}
