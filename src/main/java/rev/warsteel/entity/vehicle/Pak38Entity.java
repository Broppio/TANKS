package rev.rev.warsteel.entity.vehicle;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import rev.rev.warsteel.damage.WarSteelTank;

/**
 * 5 cm PaK 38: a towed anti-tank gun. The gun and shield traverse on the split-trail carriage,
 * but only 32 degrees either side; the crew kneel behind a thin spaced shield.
 */
public class Pak38Entity extends WarSteelTank {
   /** Traverse either side of the trails, degrees. */
   public static final float TRAVERSE = 32.0F;

   public Pak38Entity(EntityType<?> type, Level level) {
      super(type, level);
   }

   @Override
   public void adjustTurretAngle() {
      super.adjustTurretAngle();
      float yaw = Mth.wrapDegrees(this.getTurretYRot());
      if (Math.abs(yaw) > TRAVERSE) {
         this.setTurretYRot(Mth.clamp(yaw, -TRAVERSE, TRAVERSE));
      }
   }
}
