package rev.rev.warsteel.naval;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A ship whose hull can be holed and flooded. */
public interface NavalHull {
   /**
    * An explosion of {@code kgTnt} at {@code pos} (world): holes the hull plating near it, more so
    * under water. Damage to the parts inside is applied separately (TankDamage.blastFromOutside).
    */
   void hullBlast(Vec3 pos, double kgTnt, @Nullable Entity attacker);

   /** A clean hole of {@code areaM2} at {@code pos} (a shell going through the plating). */
   void hullHole(Vec3 pos, double areaM2);
}
