package rev.rev.warsteel.init.client;

import com.atsuishio.superbwarfare.init.ModEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

public class ClientEntityHighlighter {
   private static final float BULLET_SPEED = 35.0F;
   private static final double DETECTION_RANGE = 1028.0;
   private static final double MIN_Z_THRESHOLD = 0.01;
   private static final double EPSILON = 1.0E-6;
   private static final int STEALTH_SIZE = 6;
   private static final int NORMAL_SIZE = 10;
   private static final int LOCKED_COLOR = -256;
   private static final int UNLOCKED_COLOR = -16711936;
   private static final int INTERCEPT_COLOR = -65536;
   private static final float INTERCEPT_ROTATION = 35.0F;
   private static final int MAX_TARGETS = 4;
   private static Set<EntityType<?>> excludedTypes = null;
   private static Set<EntityType<?>> stealthTypes = null;
   private static final Set<String> ALLOWED_NAMESPACES = Set.of("superbwarfare", "warsteel", "vvp");
   private static Vec3 cachedShooterPos = Vec3.ZERO;
   private static double cachedFovScale = 0.0;
   private static int cachedScreenWidth = 0;
   private static int cachedScreenHeight = 0;
   private static final List<ClientEntityHighlighter.TargetCache> targetCaches = new ArrayList<>(4);

   private static void ensureInitialized() {
      if (excludedTypes == null) {
         excludedTypes = Set.of(
            (EntityType)ModEntities.SMALL_CANNON_SHELL.get(),
            (EntityType)ModEntities.SMALL_ROCKET.get(),
            (EntityType)ModEntities.CANNON_SHELL.get(),
            (EntityType)ModEntities.GUN_GRENADE.get(),
            (EntityType)ModEntities.PROJECTILE.get(),
            (EntityType)ModEntities.AGM_65.get(),
            (EntityType)ModEntities.JAVELIN_MISSILE.get(),
            (EntityType)ModEntities.HAND_GRENADE.get(),
            (EntityType)ModEntities.RGO_GRENADE.get(),
            (EntityType)ModEntities.MELON_BOMB.get(),
            (EntityType)ModEntities.MORTAR_SHELL.get(),
            (EntityType)ModEntities.MORTAR.get(),
            (EntityType)ModEntities.FLARE_DECOY.get(),
            (EntityType)ModEntities.SMOKE_DECOY.get(),
            (EntityType)ModEntities.CLAYMORE.get(),
            (EntityType)ModEntities.BLU_43.get(),
            (EntityType)ModEntities.TM_62.get(),
            (EntityType)ModEntities.TASER_BULLET.get(),
            (EntityType)ModEntities.MK_82.get(),
            (EntityType)ModEntities.MK_42.get()
         );
      }
   }

   static {
      for (int i = 0; i < 4; i++) {
         targetCaches.add(new ClientEntityHighlighter.TargetCache());
      }
   }

   private static class TargetCache {
      Entity entity;
      double distanceSq;
      Vec3 screenPos;
      Vec3 interceptScreenPos;
      boolean isStealth;
      boolean isLocked;
      int size;

      void reset() {
         this.entity = null;
         this.screenPos = null;
         this.interceptScreenPos = null;
      }
   }
}
