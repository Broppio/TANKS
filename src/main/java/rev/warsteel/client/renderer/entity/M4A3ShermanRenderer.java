package rev.rev.warsteel.client.renderer.entity;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public final class M4A3ShermanRenderer extends WarSteelTankRenderer {
   @NotNull
   public static final M4A3ShermanRenderer.Companion Companion = new M4A3ShermanRenderer.Companion(null);
   private static final int LINKS = 48;
   private static final int LENGTH = 100;
   private static final float TRACK_DISTANCE = 2.0833333F;
   @NotNull
   private static final float[] MOVE_Y;
   @NotNull
   private static final float[] MOVE_Z;
   @NotNull
   private static final float[] ROT_X;

   public M4A3ShermanRenderer(@NotNull Context manager) {
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
         -0.1382F,
         -0.8345F,
         -1.1655F,
         -1.2399F,
         -1.2399F,
         -1.2399F,
         -1.2399F,
         -1.2399F,
         -1.2399F,
         -1.2399F,
         -1.2399F,
         -1.2399F,
         -1.2399F,
         -1.2399F,
         -1.2399F,
         -1.2399F,
         -1.2399F,
         -1.2399F,
         -1.6008F,
         -3.0767F,
         -5.9395F,
         -9.4168F,
         -12.2761F,
         -14.7367F,
         -16.7077F,
         -17.7567F,
         -18.0302F,
         -18.0302F,
         -18.0302F,
         -18.0302F,
         -18.0302F,
         -18.0302F,
         -18.0302F,
         -18.0302F,
         -18.0302F,
         -18.0302F,
         -18.0302F,
         -18.0302F,
         -18.0302F,
         -18.0302F,
         -18.0302F,
         -16.7781F,
         -14.0955F,
         -11.2046F,
         -7.9199F,
         -4.5835F,
         -1.5155F,
         0.0F
      };
      MOVE_Y = var0;
      var0 = new float[]{
         0.0F,
         3.9564F,
         8.1176F,
         12.3237F,
         16.5857F,
         20.8757F,
         25.1657F,
         29.4557F,
         33.7457F,
         38.0357F,
         42.3257F,
         46.6157F,
         50.9057F,
         55.1957F,
         59.4857F,
         63.7757F,
         68.0657F,
         72.3557F,
         76.6457F,
         80.7701F,
         84.3333F,
         86.2462F,
         85.7883F,
         83.2809F,
         79.7667F,
         76.1741F,
         72.2589F,
         68.0872F,
         63.7972F,
         59.5072F,
         55.2172F,
         50.9272F,
         46.6372F,
         42.3472F,
         38.0572F,
         33.7672F,
         29.4772F,
         25.1872F,
         20.8972F,
         16.6072F,
         12.3172F,
         8.0272F,
         4.2667F,
         0.954F,
         -2.0659F,
         -4.5863F,
         -5.1896F,
         -3.3644F,
         0.0F
      };
      MOVE_Z = var0;
      var0 = new float[]{
         10.6554F,
         -5.8695F,
         -7.0F,
         -2.7411F,
         -0.4983F,
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
         -2.4555F,
         -13.4375F,
         -38.3902F,
         -77.0744F,
         -115.0778F,
         -138.5407F,
         -148.0533F,
         -158.0874F,
         -170.7122F,
         -178.1492F,
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
         -188.8404F,
         -209.086F,
         -221.3519F,
         -228.1036F,
         -244.7427F,
         -280.8022F,
         -318.5487F,
         -349.3446F
      };
      ROT_X = var0;
   }

   public static final class Companion {
      private Companion() {
      }

      private final float sample(float[] table, float t) {
         float p = (t % 100 + 100) % 100 / M4A3ShermanRenderer.TRACK_DISTANCE;
         int i = RangesKt.coerceIn((int)p, 0, 47);
         return Mth.lerp(p - i, table[i], table[i + 1]);
      }

      // $VF: synthetic method
      public Companion(DefaultConstructorMarker $constructor_marker) {
         this();
      }
   }
}
