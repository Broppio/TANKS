package rev.rev.warsteel.client.renderer.entity;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public final class T26Renderer extends WarSteelTankRenderer {
   @NotNull
   public static final T26Renderer.Companion Companion = new T26Renderer.Companion(null);
   private static final int LINKS = 53;
   private static final int LENGTH = 100;
   private static final float TRACK_DISTANCE = 1.8867924F;
   @NotNull
   private static final float[] MOVE_Y;
   @NotNull
   private static final float[] MOVE_Z;
   @NotNull
   private static final float[] ROT_X;

   public T26Renderer(@NotNull Context manager) {
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
         -0.061F,
         -0.6953F,
         -1.1642F,
         -1.3892F,
         -1.7686F,
         -2.148F,
         -2.3729F,
         -2.5979F,
         -2.8229F,
         -3.0479F,
         -3.2729F,
         -3.4978F,
         -3.7228F,
         -3.9478F,
         -4.1728F,
         -4.3978F,
         -4.6227F,
         -4.8477F,
         -5.0727F,
         -5.2977F,
         -6.1131F,
         -8.2746F,
         -11.209F,
         -14.2757F,
         -16.9358F,
         -18.6387F,
         -19.8385F,
         -21.0384F,
         -21.5921F,
         -21.5921F,
         -21.5921F,
         -21.5921F,
         -21.5921F,
         -21.5921F,
         -21.5921F,
         -21.5921F,
         -21.5921F,
         -21.5921F,
         -21.5921F,
         -21.5921F,
         -21.5921F,
         -21.5921F,
         -21.5921F,
         -21.0658F,
         -19.7009F,
         -17.7119F,
         -15.2209F,
         -12.6225F,
         -9.8403F,
         -6.7171F,
         -3.5521F,
         -1.1298F,
         0.0F
      };
      MOVE_Y = var0;
      var0 = new float[]{
         0.0F,
         3.4966F,
         7.0937F,
         10.6555F,
         14.3339F,
         17.9435F,
         21.553F,
         25.2314F,
         28.9098F,
         32.5882F,
         36.2666F,
         39.945F,
         43.6234F,
         47.3018F,
         50.9802F,
         54.6586F,
         58.337F,
         62.0154F,
         65.6938F,
         69.3722F,
         73.0506F,
         76.1456F,
         78.7446F,
         79.9005F,
         79.1642F,
         76.9519F,
         74.0318F,
         70.5473F,
         67.0628F,
         63.4238F,
         59.7385F,
         56.0533F,
         52.368F,
         48.6827F,
         44.9974F,
         41.3122F,
         37.6269F,
         33.9416F,
         30.2564F,
         26.5711F,
         22.8858F,
         19.2005F,
         15.5153F,
         11.83F,
         8.4104F,
         5.1153F,
         2.2213F,
         -0.45F,
         -3.0484F,
         -5.2026F,
         -6.0982F,
         -5.3237F,
         -3.1041F,
         0.0F
      };
      MOVE_Z = var0;
      var0 = new float[]{
         9.1974F,
         -5.5981F,
         -8.7603F,
         -5.4745F,
         -4.7405F,
         -6.0F,
         -4.7405F,
         -3.5F,
         -3.5F,
         -3.5F,
         -3.5F,
         -3.5F,
         -3.5F,
         -3.5F,
         -3.5001F,
         -3.5001F,
         -3.5F,
         -3.5F,
         -3.5F,
         -3.5F,
         -8.7322F,
         -27.6014F,
         -53.6161F,
         -85.9999F,
         -117.2431F,
         -139.6326F,
         -155.6185F,
         -160.9999F,
         -166.1707F,
         -175.6763F,
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
         -184.2367F,
         -195.73F,
         -208.4531F,
         -218.8338F,
         -224.003F,
         -228.5463F,
         -242.6865F,
         -268.8964F,
         -298.186F,
         -326.2879F,
         -350.8026F
      };
      ROT_X = var0;
   }

   public static final class Companion {
      private Companion() {
      }

      private final float sample(float[] table, float t) {
         float p = (t % 100 + 100) % 100 / T26Renderer.TRACK_DISTANCE;
         int i = RangesKt.coerceIn((int)p, 0, 52);
         return Mth.lerp(p - i, table[i], table[i + 1]);
      }

      // $VF: synthetic method
      public Companion(DefaultConstructorMarker $constructor_marker) {
         this();
      }
   }
}
