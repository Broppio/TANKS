package rev.rev.warsteel.client.renderer.entity;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public final class KV2Renderer extends WarSteelTankRenderer {
   @NotNull
   public static final KV2Renderer.Companion Companion = new KV2Renderer.Companion(null);
   private static final int LINKS = 46;
   private static final int LENGTH = 100;
   private static final float TRACK_DISTANCE = 2.173913F;
   @NotNull
   private static final float[] MOVE_Y;
   @NotNull
   private static final float[] MOVE_Z;
   @NotNull
   private static final float[] ROT_X;

   public KV2Renderer(@NotNull Context manager) {
      super(manager);
   }

   public boolean hideForTurretControllerWhileZooming() {
      return true;
   }

   public float getTrackDistance() {
      return TRACK_DISTANCE;
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
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         -0.5262F,
         -3.1417F,
         -7.9094F,
         -12.2148F,
         -15.2636F,
         -17.7353F,
         -18.8605F,
         -18.8605F,
         -18.8605F,
         -18.8605F,
         -18.8605F,
         -18.8605F,
         -18.8605F,
         -18.8605F,
         -18.8605F,
         -18.8605F,
         -18.8605F,
         -18.8605F,
         -18.8605F,
         -18.8605F,
         -18.8605F,
         -18.1305F,
         -16.0831F,
         -13.5049F,
         -10.3134F,
         -6.0F,
         -1.7574F,
         0.0F
      };
      MOVE_Y = var0;
      var0 = new float[]{
         0.0F,
         5.0F,
         10.0F,
         15.0F,
         20.0F,
         25.0F,
         30.0F,
         35.0F,
         40.0F,
         45.0F,
         50.0F,
         55.0F,
         60.0F,
         65.0F,
         70.0F,
         75.0F,
         80.0F,
         85.0F,
         90.0F,
         95.0062F,
         99.2744F,
         100.3751F,
         97.7881F,
         93.8149F,
         89.4461F,
         84.4705F,
         79.4705F,
         74.4705F,
         69.4705F,
         64.4705F,
         59.4705F,
         54.4705F,
         49.4705F,
         44.4705F,
         39.4705F,
         34.4705F,
         29.4705F,
         24.4705F,
         19.4705F,
         14.4705F,
         9.3762F,
         4.553F,
         -0.0126F,
         -4.1719F,
         -6.0F,
         -4.2426F,
         0.0F
      };
      MOVE_Z = var0;
      var0 = new float[]{
         10.7655F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         -3.0101F,
         -18.7141F,
         -53.9764F,
         -99.3027F,
         -131.7341F,
         -146.5045F,
         -158.9469F,
         -173.5647F,
         -180.0F,
         -180.0F,
         -180.0F,
         -180.0F,
         -180.0F,
         -180.0F,
         -180.0F,
         -180.0F,
         -180.0F,
         -180.0F,
         -180.0F,
         -180.0F,
         -180.0F,
         -184.1364F,
         -195.6446F,
         -206.2278F,
         -213.4766F,
         -231.4172F,
         -269.5265F,
         -315.0F,
         -349.2345F
      };
      ROT_X = var0;
   }

   public static final class Companion {
      private Companion() {
      }

      private final float sample(float[] table, float t) {
         float p = (t % 100 + 100) % 100 / KV2Renderer.TRACK_DISTANCE;
         int i = RangesKt.coerceIn((int)p, 0, 45);
         return Mth.lerp(p - i, table[i], table[i + 1]);
      }

      // $VF: synthetic method
      public Companion(DefaultConstructorMarker $constructor_marker) {
         this();
      }
   }
}
