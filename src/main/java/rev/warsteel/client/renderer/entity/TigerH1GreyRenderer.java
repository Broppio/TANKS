package rev.rev.warsteel.client.renderer.entity;

import com.atsuishio.superbwarfare.client.renderer.entity.BasicVehicleRenderer;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public final class TigerH1GreyRenderer extends BasicVehicleRenderer {
   @NotNull
   public static final TigerH1GreyRenderer.Companion Companion = new TigerH1GreyRenderer.Companion(null);
   private static final float LENGTH = 100.0F;
   private static final int LINKS = 40;
   @NotNull
   private static final float[] MOVE_Y;
   @NotNull
   private static final float[] MOVE_Z;
   @NotNull
   private static final float[] ROT_X;

   public TigerH1GreyRenderer(@NotNull Context manager) {
      super(manager);
   }

   public boolean hideForTurretControllerWhileZooming() {
      return true;
   }

   public float getTrackDistance() {
      return 2.5F;
   }

   public float wrap(float value, @NotNull VehicleEntity entity) {
      return this.wrap(value, 100);
   }

   public float getBoneMoveY(float t) {
      return Companion.sample(MOVE_Y, t);
   }

   public float getBoneMoveZ(float t) {
      return Companion.sample(MOVE_Z, t);
   }

   public float getBoneRotX(float t) {
      return Companion.sample(ROT_X, t);
   }

   static {
      float[] var0 = new float[]{
         0.0F,
         -0.1173F,
         -0.2411F,
         -0.3649F,
         -0.4887F,
         -0.6125F,
         -0.7363F,
         -0.8601F,
         -0.9839F,
         -1.1077F,
         -1.2315F,
         -1.3554F,
         -1.4792F,
         -1.603F,
         -1.7268F,
         -1.8585F,
         -3.6361F,
         -7.6733F,
         -12.4843F,
         -16.2986F,
         -17.7569F,
         -18.2251F,
         -18.6328F,
         -18.6328F,
         -18.6328F,
         -18.6328F,
         -18.6328F,
         -18.6328F,
         -18.6328F,
         -18.6328F,
         -18.6328F,
         -18.6328F,
         -18.6328F,
         -18.6015F,
         -17.4049F,
         -16.0727F,
         -14.2017F,
         -10.2326F,
         -5.4182F,
         -1.5304F,
         0.0F
      };
      MOVE_Y = var0;
      var0 = new float[]{
         0.0F,
         4.8905F,
         9.7809F,
         14.6713F,
         19.5617F,
         24.4521F,
         29.3425F,
         34.2329F,
         39.1234F,
         44.0138F,
         48.9042F,
         53.7946F,
         58.685F,
         63.5754F,
         68.4658F,
         73.3559F,
         77.8307F,
         80.4545F,
         80.2616F,
         77.3232F,
         72.7232F,
         67.8537F,
         62.9806F,
         58.0887F,
         53.1967F,
         48.3047F,
         43.4127F,
         38.5208F,
         33.6288F,
         28.7368F,
         23.8448F,
         18.9528F,
         14.0609F,
         9.1698F,
         4.4333F,
         -0.2737F,
         -4.7504F,
         -7.4761F,
         -7.4057F,
         -4.5652F,
         0.0F
      };
      MOVE_Z = var0;
      var0 = new float[]{
         0.0F,
         -0.0765F,
         -0.0765F,
         -0.0765F,
         -0.0765F,
         -0.0765F,
         -0.0765F,
         -0.0765F,
         -0.0765F,
         -0.0765F,
         -0.0765F,
         -0.0765F,
         -0.0765F,
         -0.0765F,
         -0.1691F,
         -20.2915F,
         -55.6065F,
         -90.9216F,
         -126.2365F,
         -161.0369F,
         -173.134F,
         -173.8441F,
         -178.6263F,
         -178.6263F,
         -178.6263F,
         -178.6263F,
         -178.6263F,
         -178.6263F,
         -178.6263F,
         -178.6263F,
         -178.6263F,
         -178.6263F,
         -178.9922F,
         -192.8046F,
         -194.4288F,
         -201.3082F,
         -234.1487F,
         -269.4637F,
         -304.7788F,
         -340.0937F,
         -360.0F
      };
      ROT_X = var0;
   }

   public static final class Companion {
      private Companion() {
      }

      private final float sample(float[] table, float t) {
         float p = (t % 100.0F + 100.0F) % 100.0F / 2.5F;
         int i = RangesKt.coerceIn((int)p, 0, 39);
         return Mth.lerp(p - i, table[i], table[i + 1]);
      }

      // $VF: synthetic method
      public Companion(DefaultConstructorMarker $constructor_marker) {
         this();
      }
   }
}
