package rev.rev.warsteel.client.artillery;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import rev.rev.warsteel.entity.vehicle.Flak88Entity;
import rev.rev.warsteel.network.ArtilleryPayloads;

/**
 * Flak 88 ARC mode, client side: the keys, and a chase camera that follows each shell fired in ARC
 * mode, holds on the impact for a moment and then returns to the gun.
 *
 * <p>The camera tracks the real shell entity while the client can see it, and the shell's own
 * ballistics (from its launch state) once it flies out of entity-tracking range.
 */
public final class ShellCamera {
   public static final KeyMapping ARC_TOGGLE = new KeyMapping(
      "key.warsteel.arc_mode", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_B, "key.categories.warsteel"
   );
   public static final KeyMapping CHARGE_UP = new KeyMapping(
      "key.warsteel.arc_charge_up", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_RBRACKET, "key.categories.warsteel"
   );
   public static final KeyMapping CHARGE_DOWN = new KeyMapping(
      "key.warsteel.arc_charge_down", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_LBRACKET, "key.categories.warsteel"
   );

   private static final double BACK = 9.0;
   private static final double UP = 2.5;
   private static final int LINGER_TICKS = 60;
   private static final int MAX_TICKS = 6200;

   private static boolean active;
   private static int shellId;
   private static Vec3 shellPos = Vec3.ZERO;
   private static Vec3 shellVel = Vec3.ZERO;
   private static float gravity;
   private static boolean landed;
   /** Game time each shell was fired, for the splash report (kept even if the camera is left early). */
   private static final java.util.Map<Integer, Long> firedAt = new java.util.HashMap<>();
   private static int age;
   private static int linger;
   private static Vec3 camPos = Vec3.ZERO;
   private static Vec3 camPrev = Vec3.ZERO;
   private static float yaw;
   private static float yawPrev;
   private static float pitch;
   private static float pitchPrev;
   private static float lockYRot;
   private static float lockXRot;
   /** Gravity actually reported by the last shell, for the trajectory predictor. */
   @Nullable
   static Float observedGravity;

   private ShellCamera() {
   }

   public static boolean active() {
      return active;
   }

   public static float flightSeconds() {
      return age / 20.0F;
   }

   public static boolean hasLanded() {
      return landed;
   }

   public static void launched(ArtilleryPayloads.ShellLaunched msg) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      observedGravity = msg.gravity();
      if (mc.level != null) {
         if (firedAt.size() > 32) {
            firedAt.clear();
         }

         firedAt.put(msg.shellId(), mc.level.getGameTime());
      }
      if (player == null || !(player.getVehicle() instanceof Flak88Entity)) {
         return;
      }

      active = true;
      shellId = msg.shellId();
      shellPos = msg.pos();
      shellVel = msg.velocity();
      gravity = msg.gravity();
      landed = false;
      age = 0;
      linger = 0;
      lockYRot = player.getYRot();
      lockXRot = player.getXRot();
      Vec3 dir = shellVel.lengthSqr() > 1.0E-6 ? shellVel.normalize() : player.getLookAngle();
      // Start just behind the muzzle, so the cut to the shell camera reads as following the shot.
      camPos = shellPos.subtract(dir.scale(3.0)).add(0.0, 0.8, 0.0);
      camPrev = camPos;
      aimAt(shellPos, true);
   }

   public static void landed(ArtilleryPayloads.ShellLanded msg) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (active && msg.shellId() == shellId) {
         if (!landed) {
            landed = true;
         }

         shellPos = msg.pos();
      }

      if (player != null && player.getVehicle() instanceof Flak88Entity gun) {
         // Where it came down, so the next shot can be corrected even when the splash is out of sight.
         double dx = msg.pos().x - gun.getX();
         double dz = msg.pos().z - gun.getZ();
         double bearing = Math.round(Math.toDegrees(Math.atan2(-dx, dz)) + 360.0) % 360;
         player.displayClientMessage(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT,
            "Splash: %,.0f m, bearing %.0f°, %+.0f m height, %.1f s", Math.sqrt(dx * dx + dz * dz), bearing, msg.pos().y - gun.getY(),
            (gun.level().getGameTime() - firedAt.getOrDefault(msg.shellId(), gun.level().getGameTime())) / 20.0F))
            .withStyle(net.minecraft.ChatFormatting.GOLD), false);
      }
   }

   public static void stop() {
      active = false;
      landed = false;
   }

   @Nullable
   public static Vec3 position(Flak88Entity gun, Entity player, float partialTicks) {
      if (!active || player != Minecraft.getInstance().player) {
         return null;
      }

      return camPrev.lerp(camPos, partialTicks);
   }

   @Nullable
   public static Vec2 rotation(Flak88Entity gun, Entity player, float partialTicks) {
      if (!active || player != Minecraft.getInstance().player) {
         return null;
      }

      return new Vec2(Mth.rotLerp(partialTicks, yawPrev, yaw), Mth.lerp(partialTicks, pitchPrev, pitch));
   }

   private static void aimAt(Vec3 target, boolean snap) {
      Vec3 d = target.subtract(camPos);
      double h = Math.sqrt(d.x * d.x + d.z * d.z);
      float ny = (float)(Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG);
      float np = (float)(-Mth.atan2(d.y, Math.max(h, 1.0E-4)) * Mth.RAD_TO_DEG);
      yawPrev = snap ? ny : yaw;
      pitchPrev = snap ? np : pitch;
      yaw = ny;
      pitch = np;
   }

   @EventBusSubscriber(modid = "warsteel", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
   public static final class Keys {
      @SubscribeEvent
      public static void register(RegisterKeyMappingsEvent event) {
         event.register(ARC_TOGGLE);
         event.register(CHARGE_UP);
         event.register(CHARGE_DOWN);
      }
   }

   @EventBusSubscriber(modid = "warsteel", value = Dist.CLIENT)
   public static final class Ticker {
      @SubscribeEvent
      public static void onTick(ClientTickEvent.Post event) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer player = mc.player;
         if (player == null || mc.level == null) {
            stop();
            return;
         }

         boolean onGun = player.getVehicle() instanceof Flak88Entity;
         while (ARC_TOGGLE.consumeClick()) {
            if (active) {
               // Leave the shell camera early.
               stop();
            } else if (onGun) {
               PacketDistributor.sendToServer(new ArtilleryPayloads.ArcControl(ArtilleryPayloads.ArcControl.TOGGLE));
            }
         }

         while (CHARGE_UP.consumeClick()) {
            if (onGun) {
               PacketDistributor.sendToServer(new ArtilleryPayloads.ArcControl(ArtilleryPayloads.ArcControl.CHARGE_UP));
            }
         }

         while (CHARGE_DOWN.consumeClick()) {
            if (onGun) {
               PacketDistributor.sendToServer(new ArtilleryPayloads.ArcControl(ArtilleryPayloads.ArcControl.CHARGE_DOWN));
            }
         }

         if (onGun && player.getVehicle() instanceof Flak88Entity gun && gun.arcMode() && !active) {
            ArcPredictor.update(gun, player);
         } else {
            ArcPredictor.clear();
         }

         if (!active) {
            return;
         }

         if (!onGun || ++age > MAX_TICKS) {
            stop();
            return;
         }

         // Keep the gun laid where it was while the camera is away.
         player.setYRot(lockYRot);
         player.setXRot(lockXRot);
         player.yRotO = lockYRot;
         player.xRotO = lockXRot;
         camPrev = camPos;
         if (landed) {
            if (++linger > LINGER_TICKS) {
               stop();
               return;
            }

            // Hold back from the impact and watch it.
            Vec3 flat = new Vec3(shellVel.x, 0.0, shellVel.z);
            flat = flat.lengthSqr() > 1.0E-6 ? flat.normalize() : new Vec3(0.0, 0.0, 1.0);
            Vec3 want = shellPos.subtract(flat.scale(14.0)).add(0.0, 7.0, 0.0);
            camPos = camPos.lerp(want, 0.15);
            aimAt(shellPos, false);
            return;
         }

         Entity shell = mc.level.getEntity(shellId);
         if (shell != null && !shell.isRemoved()) {
            shellPos = shell.position();
            if (shell.getDeltaMovement().lengthSqr() > 1.0E-6) {
               shellVel = shell.getDeltaMovement();
            }
         } else {
            // Out of the client's entity range: fly the shell's own ballistics.
            Vec3 next = shellPos.add(shellVel);
            if (ArcPredictor.loaded(mc.level, next)) {
               BlockHitResult hit = mc.level.clip(new ClipContext(shellPos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
               if (hit.getType() != HitResult.Type.MISS) {
                  shellPos = hit.getLocation();
                  landed = true;
                  return;
               }
            }

            shellPos = next;
            shellVel = shellVel.add(0.0, -gravity, 0.0);
         }

         Vec3 dir = shellVel.normalize();
         Vec3 want = shellPos.subtract(dir.scale(BACK)).add(0.0, UP, 0.0);
         // Close the gap quickly but smoothly; the shell moves several blocks a tick.
         camPos = age < 3 ? want : camPos.lerp(want, 0.6);
         aimAt(shellPos.add(dir.scale(6.0)), false);
      }
   }
}
