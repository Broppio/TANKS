package rev.rev.warsteel.combat;

import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public final class ArmorPlate {
   @NotNull
   private final String name;
   @NotNull
   private final String transform;
   @NotNull
   private final double[] size;
   @NotNull
   private final double[] position;
   private final double thickness;
   private final double ricochetChance;
   private final int normalAxis;
   @NotNull
   private final Vector3d localNormal;

   public ArmorPlate(
      @NotNull String name, @NotNull String transform, @NotNull double[] size, @NotNull double[] position, double thickness, double ricochetChance, @Nullable double[] normal
   ) {
      super();
      this.name = name;
      this.transform = transform;
      this.size = size;
      this.position = position;
      this.thickness = thickness;
      this.ricochetChance = ricochetChance;
      int var10001;
      if (this.size.length >= 3) {
         int best = 0;

         for (int i = 0; i < 3; i++) {
            if (this.size[i] < this.size[best]) {
               best = i;
            }
         }

         var10001 = best;
      } else {
         var10001 = 2;
      }

      this.normalAxis = var10001;

      if (normal != null && normal.length == 3 && normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2] > 1.0E-9) {
         // Explicit outward normal for sloped plates (the box stays axis-aligned for hit attribution,
         // but the angle of incidence uses the real slope).
         this.localNormal = new Vector3d(normal[0], normal[1], normal[2]).normalize();
      } else {
         this.localNormal = switch (this.normalAxis) {
            case 0 -> new Vector3d(this.position[0] < 0.0 ? -1.0 : 1.0, 0.0, 0.0);
            case 1 -> new Vector3d(0.0, this.position[1] < 0.0 ? -1.0 : 1.0, 0.0);
            default -> new Vector3d(0.0, 0.0, this.position[2] < 0.0 ? -1.0 : 1.0);
         };
      }
   }

   @NotNull
   public final String getName() {
      return this.name;
   }

   @NotNull
   public final String getTransform() {
      return this.transform;
   }

   @NotNull
   public final double[] getSize() {
      return this.size;
   }

   @NotNull
   public final double[] getPosition() {
      return this.position;
   }

   public final double getThickness() {
      return this.thickness;
   }

   public final double getRicochetChance() {
      return this.ricochetChance;
   }

   public final int getNormalAxis() {
      return this.normalAxis;
   }

   @NotNull
   public final Vector3d getLocalNormal() {
      return this.localNormal;
   }
}
