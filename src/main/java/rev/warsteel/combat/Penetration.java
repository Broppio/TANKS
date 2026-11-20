package rev.rev.warsteel.combat;


public final class Penetration {
   private final double muzzle;
   private final double dropoffPerBlock;
   private final double minimum;

   public Penetration(double muzzle, double dropoffPerBlock, double minimum) {
      this.muzzle = muzzle;
      this.dropoffPerBlock = dropoffPerBlock;
      this.minimum = minimum;
   }

   public final double getMuzzle() {
      return this.muzzle;
   }

   public final double getDropoffPerBlock() {
      return this.dropoffPerBlock;
   }

   public final double getMinimum() {
      return this.minimum;
   }

   public final double at(double blocks) {
      return Math.max(this.minimum, this.muzzle - this.dropoffPerBlock * blocks);
   }
}
