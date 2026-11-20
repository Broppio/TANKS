package rev.rev.warsteel.client.sight;

import java.util.HashMap;
import java.util.Map;
import kotlin.jvm.internal.SourceDebugExtension;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
@SourceDebugExtension({"SMAP\nSightBallistics.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SightBallistics.kt\nrev/rev/warsteel/client/sight/SightBallistics\n+ 2 Maps.kt\nkotlin/collections/MapsKt__MapsKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,112:1\n381#2,7:113\n1#3:120\n*S KotlinDebug\n*F\n+ 1 SightBallistics.kt\nrev/rev/warsteel/client/sight/SightBallistics\n*L\n63#1:113,7\n*E\n"})
public final class SightBallistics {
   @NotNull
   public static final SightBallistics INSTANCE = new SightBallistics();
   private static final double MAX_ELEVATION_RAD = 0.785398;
   private static final int BISECTION_STEPS = 48;
   @NotNull
   private static final HashMap<Long, Double> cache = new HashMap<>();

   private SightBallistics() {
   }

   public final void clearCache() {
      cache.clear();
   }

   private final long key(double range, double v, double g) {
      long h = Double.doubleToRawLongBits(range);
      h = h * 31 + Double.doubleToRawLongBits(v);
      return h * 31 + Double.doubleToRawLongBits(g);
   }

   @Nullable
   public final Double elevation(double range, double velocity, double gravity) {
      if (range <= 0.0 || velocity <= 0.0) {
         return null;
      } else if (gravity <= 0.0) {
         return 0.0;
      } else {
         Map $this$getOrPut$iv = cache;
         Object key$iv = this.key(range, velocity, gravity);
         Object value$iv = $this$getOrPut$iv.get(key$iv);
         Object var10000;
         if (value$iv == null) {
            double lo = 0.0;
            double hi = 0.0;
            hi = 0.785398;
            byte var12 = 48;

            for (int var13 = 0; var13 < var12; var13++) {
               double mid = (lo + hi) * 0.5;
               if (INSTANCE.heightAtRange(range, velocity, gravity, mid) < 0.0) {
                  lo = mid;
               } else {
                  hi = mid;
               }
            }

            double theta = (lo + hi) * 0.5;
            Object answer$iv = theta >= 0.785397 ? Double.NaN : theta;
            $this$getOrPut$iv.put(key$iv, answer$iv);
            var10000 = answer$iv;
         } else {
            var10000 = value$iv;
         }

         double it = ((Number)var10000).doubleValue();
         return Double.isNaN(it) ? null : it;
      }
   }

   private final double heightAtRange(double range, double v, double g, double theta) {
      double vx = v * Math.cos(theta);
      if (vx <= 1.0E-9) {
         return -Double.MAX_VALUE;
      } else {
         double n = range / vx;
         return n * v * Math.sin(theta) - g * n * (n - 1.0) * 0.5;
      }
   }

   public final float pixelsBelowBoresight(double elevationRad, double fovVerticalDeg, int screenHeight) {
      double halfFov = Math.toRadians(fovVerticalDeg * 0.5);
      return halfFov <= 1.0E-6 ? 0.0F : (float)(screenHeight * 0.5 * Math.tan(elevationRad) / Math.tan(halfFov));
   }

   @Nullable
   public final Float markOffset(double range, double velocity, double gravity, double fovVerticalDeg, int screenHeight) {
      Double var10 = this.elevation(range, velocity, gravity);
      Float var10000;
      if (var10 != null) {
         double it = var10.doubleValue();
         var10000 = INSTANCE.pixelsBelowBoresight(it, fovVerticalDeg, screenHeight);
      } else {
         var10000 = null;
      }

      return var10000;
   }

   public final boolean sameShell(double a, double b) {
      return Math.abs(a - b) < 0.05;
   }
}
