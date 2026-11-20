package rev.rev.warsteel.init.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;

public class ClientTargetingData {
   private static final List<Entity> lockedTargets = new ArrayList<>();

   public static void setLockedTargets(List<Entity> entities) {
      lockedTargets.clear();
      if (entities != null) {
         lockedTargets.addAll(entities);
      }
   }

   public static void setLockedTarget(@Nullable Entity entity) {
      if (entity != null) {
         setLockedTargets(List.of(entity));
      } else {
         clearLockedTargets();
      }
   }

   public static void clearLockedTargets() {
      lockedTargets.clear();
   }

   @Nullable
   public static Entity getLockedTarget() {
      return lockedTargets.isEmpty() ? null : lockedTargets.get(0);
   }

   public static List<Entity> getLockedTargets() {
      return Collections.unmodifiableList(lockedTargets);
   }

   public static void clearLockedTarget() {
      clearLockedTargets();
   }
}
