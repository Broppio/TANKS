package rev.rev.warsteel.client.renderer.entity;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public final class TigerH1Renderer extends WarSteelTankRenderer {
   @NotNull
   public static final TigerH1Renderer.Companion Companion = new TigerH1Renderer.Companion(null);
   private static final int LINKS = 39;
   private static final int LENGTH = 100;
   private static final float TRACK_DISTANCE = 2.5641026F;
   @NotNull
   private static final float[] MOVE_Y;
   @NotNull
   private static final float[] MOVE_Z;
   @NotNull
   private static final float[] ROT_X;

   public TigerH1Renderer(@NotNull Context manager) {
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
         0.0549F,
         -0.0847F,
         -0.2243F,
         -0.364F,
         -0.5036F,
         -0.6432F,
         -0.7829F,
         -0.9225F,
         -1.0621F,
         -1.2017F,
         -1.3414F,
         -1.481F,
         -1.6206F,
         -1.7602F,
         -1.8999F,
         -3.1844F,
         -7.4597F,
         -12.8664F,
         -16.9463F,
         -18.1424F,
         -18.6704F,
         -19.0092F,
         -19.0092F,
         -19.0092F,
         -19.0092F,
         -19.0092F,
         -19.0092F,
         -19.0092F,
         -19.0092F,
         -19.0092F,
         -19.0092F,
         -19.0092F,
         -18.765F,
         -17.2657F,
         -15.7633F,
         -12.9613F,
         -7.869F,
         -2.7768F,
         0.0F
      };
      MOVE_Y = var0;
      var0 = new float[]{
         0.0F,
         5.5059F,
         11.0212F,
         16.5365F,
         22.0518F,
         27.5671F,
         33.0824F,
         38.5977F,
         44.113F,
         49.6283F,
         55.1436F,
         60.6589F,
         66.1742F,
         71.6895F,
         77.2048F,
         82.7201F,
         87.9963F,
         91.3099F,
         91.156F,
         87.6047F,
         82.2723F,
         76.7805F,
         71.2791F,
         65.762F,
         60.245F,
         54.7279F,
         49.2108F,
         43.6938F,
         38.1767F,
         32.6596F,
         27.1426F,
         21.6255F,
         16.1084F,
         10.6115F,
         5.3021F,
         -0.0064F,
         -4.6418F,
         -6.4655F,
         -4.6418F,
         0.0F
      };
      MOVE_Z = var0;
      var0 = new float[]{
         15.5916F,
         -0.4404F,
         -1.4502F,
         -1.4502F,
         -1.4502F,
         -1.4502F,
         -1.4502F,
         -1.4502F,
         -1.4502F,
         -1.4502F,
         -1.4502F,
         -1.4502F,
         -1.4502F,
         -1.4502F,
         -1.4502F,
         -7.5181F,
         -32.9133F,
         -71.9261F,
         -111.3338F,
         -149.2944F,
         -170.9496F,
         -175.4914F,
         -178.2389F,
         -180.0F,
         -180.0F,
         -180.0F,
         -180.0F,
         -180.0F,
         -180.0F,
         -180.0F,
         -180.0F,
         -180.0F,
         -181.2701F,
         -189.1651F,
         -195.7857F,
         -203.4064F,
         -230.7101F,
         -270.0F,
         -309.4078F,
         -344.4084F
      };
      ROT_X = var0;
   }

   public static final class Companion {
      private Companion() {
      }

      private final float sample(float[] table, float t) {
         float p = (t % 100 + 100) % 100 / TigerH1Renderer.TRACK_DISTANCE;
         int i = RangesKt.coerceIn((int)p, 0, 38);
         return Mth.lerp(p - i, table[i], table[i + 1]);
      }

      // $VF: synthetic method
      public Companion(DefaultConstructorMarker $constructor_marker) {
         this();
      }
   }
}
