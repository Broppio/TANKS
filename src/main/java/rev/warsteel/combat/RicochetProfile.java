package rev.rev.warsteel.combat;


public final class RicochetProfile {
   private final boolean enabled;
   private final double minAngle;
   private final double alwaysAngle;
   private final double overmatchRatio;
   private final double speedRetained;
   private final double scatterDegrees;

   public RicochetProfile(boolean enabled, double minAngle, double alwaysAngle, double overmatchRatio, double speedRetained, double scatterDegrees) {
      this.enabled = enabled;
      this.minAngle = minAngle;
      this.alwaysAngle = alwaysAngle;
      this.overmatchRatio = overmatchRatio;
      this.speedRetained = speedRetained;
      this.scatterDegrees = scatterDegrees;
   }

   public final boolean getEnabled() {
      return this.enabled;
   }

   public final double getMinAngle() {
      return this.minAngle;
   }

   public final double getAlwaysAngle() {
      return this.alwaysAngle;
   }

   public final double getOvermatchRatio() {
      return this.overmatchRatio;
   }

   public final double getSpeedRetained() {
      return this.speedRetained;
   }

   public final double getScatterDegrees() {
      return this.scatterDegrees;
   }

   public final double angleFactor(double angleDeg) {
      if (!this.enabled || angleDeg < this.minAngle) {
         return 0.0;
      } else {
         return angleDeg >= this.alwaysAngle ? 1.0 : (angleDeg - this.minAngle) / (this.alwaysAngle - this.minAngle);
      }
   }
}
