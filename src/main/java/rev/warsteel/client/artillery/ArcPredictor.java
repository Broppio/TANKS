package rev.rev.warsteel.client.artillery;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import rev.rev.warsteel.entity.vehicle.Flak88Entity;

/**
 * ARC mode fire control: integrates the shell's flight exactly as Superb Warfare moves it (position
 * += velocity, then velocity.y -= gravity, every tick, no drag) from the gun's current lay and
 * charge, draws the trajectory and impact point in the world and shows range and time of flight.
 */
public final class ArcPredictor implements LayeredDraw.Layer {
   public static final ArcPredictor INSTANCE = new ArcPredictor();
   public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("warsteel", "arc_fire_control");
   /** Superb Warfare removes cannon shells after this many ticks (ProjectileLife). */
   private static final int SHELL_LIFE = 800;

   private static final List<Vec3> path = new ArrayList<>();
   @Nullable
   private static Vec3 impact;
   private static boolean approximate;
   private static boolean outOfLife;
   private static int flightTicks;
   private static double range;
   private static double heightDiff;
   private static double elevation;
   private static double muzzleVelocity;
   private static String chargeName = "";
   private static boolean shown;

   private ArcPredictor() {
   }

   static void clear() {
      shown = false;
      path.clear();
      impact = null;
   }

   static void update(Flak88Entity gun, Player player) {
      Level level = player.level();
      Vec3 pos = gun.getShootPos(player, 1.0F);
      Vec3 dir = gun.getShootVec(player, 1.0F).normalize();
      float factor = Flak88Entity.chargeFactor(gun.charge());
      double v = gun.getProjectileVelocity(player) * factor;
      double g = ShellCamera.observedGravity != null ? ShellCamera.observedGravity : gun.getProjectileGravity(player);
      Vec3 vel = dir.scale(v);
      Vec3 start = pos;
      shown = true;
      path.clear();
      path.add(pos);
      impact = null;
      approximate = false;
      outOfLife = false;
      elevation = Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, dir.y))));
      muzzleVelocity = v * 20.0;
      chargeName = Flak88Entity.chargeName(gun.charge());
      int t = 0;

      for (; t < SHELL_LIFE; t++) {
         Vec3 next = pos.add(vel);
         if (level.hasChunkAt(BlockPos.containing(next))) {
            BlockHitResult hit = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
            if (hit.getType() != HitResult.Type.MISS) {
               impact = hit.getLocation();
               path.add(impact);
               break;
            }
         } else {
            approximate = true;
            // Beyond the loaded world: assume the target is level with the gun.
            if (vel.y < 0.0 && next.y <= start.y) {
               double f = (pos.y - start.y) / Math.max(pos.y - next.y, 1.0E-6);
               impact = pos.add(vel.scale(f));
               path.add(impact);
               break;
            }
         }

         pos = next;
         vel = vel.add(0.0, -g, 0.0);
         if (path.size() < 400 || t % 4 == 0) {
            path.add(pos);
         }

         if (pos.y < level.getMinBuildHeight() - 16) {
            break;
         }
      }

      flightTicks = t + 1;
      if (impact == null) {
         outOfLife = true;
         impact = pos;
      }

      Vec3 d = impact.subtract(start);
      range = Math.sqrt(d.x * d.x + d.z * d.z);
      heightDiff = d.y;
   }

   @Override
   public void render(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.options.hideGui || mc.player == null || !(mc.player.getVehicle() instanceof Flak88Entity gun)) {
         return;
      }

      Font font = mc.font;
      int x = 8;
      int y = g.guiHeight() / 2 - 40;
      if (ShellCamera.active()) {
         String s = String.format(Locale.ROOT, "SHELL CAM  T+%.1f s%s", ShellCamera.flightSeconds(), ShellCamera.hasLanded() ? "  · IMPACT" : "");
         g.drawString(font, s, x, y, 0xFFD27F, true);
         g.drawString(font, "[" + ShellCamera.ARC_TOGGLE.getTranslatedKeyMessage().getString() + "] back to the gun", x, y + 11, 0xA0A0A0, true);
         return;
      }

      String key = ShellCamera.ARC_TOGGLE.getTranslatedKeyMessage().getString();
      if (!gun.arcMode()) {
         g.drawString(font, "[" + key + "] ARC artillery mode", x, y, 0x909090, true);
         return;
      }

      if (!shown) {
         return;
      }

      int lineY = y;
      g.drawString(font, "ARC MODE  [" + key + "] direct fire", x, lineY, 0xFFB000, true);
      lineY += 12;
      g.drawString(font, String.format(Locale.ROOT, "%s  (%.0f m/s)  [%s/%s]", chargeName, muzzleVelocity,
         ShellCamera.CHARGE_DOWN.getTranslatedKeyMessage().getString(), ShellCamera.CHARGE_UP.getTranslatedKeyMessage().getString()), x, lineY, 0xFFFFFF, true);
      lineY += 11;
      g.drawString(font, String.format(Locale.ROOT, "Elevation %.1f°", elevation), x, lineY, 0xFFFFFF, true);
      lineY += 11;
      if (outOfLife) {
         g.drawString(font, "Beyond fuze range: lower the charge or elevation", x, lineY, 0xFF6060, true);
      } else {
         g.drawString(font, String.format(Locale.ROOT, "Range %s%,.0f m   Time of flight %.1f s", approximate ? "≈" : "", range, flightTicks / 20.0), x, lineY, 0x80FF80, true);
         lineY += 11;
         if (approximate) {
            g.drawString(font, "(impact beyond view: level with the gun)", x, lineY, 0xA0A0A0, true);
         } else {
            g.drawString(font, String.format(Locale.ROOT, "Impact %+.0f m from the gun's height", heightDiff), x, lineY, 0xA0A0A0, true);
         }
      }
   }

   @EventBusSubscriber(modid = "warsteel", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
   public static final class Layers {
      @SubscribeEvent
      public static void register(RegisterGuiLayersEvent event) {
         event.registerAboveAll(ID, INSTANCE);
      }
   }

   @EventBusSubscriber(modid = "warsteel", value = Dist.CLIENT)
   public static final class WorldRender {
      @SubscribeEvent
      public static void onRender(RenderLevelStageEvent event) {
         if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !shown || path.size() < 2 || ShellCamera.active()) {
            return;
         }

         Minecraft mc = Minecraft.getInstance();
         Vec3 cam = event.getCamera().getPosition();
         PoseStack ps = event.getPoseStack();
         ps.pushPose();
         ps.translate(-cam.x, -cam.y, -cam.z);
         MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
         VertexConsumer vc = buffers.getBuffer(RenderType.lines());
         PoseStack.Pose pose = ps.last();
         int r = outOfLife ? 255 : 255;
         int gr = outOfLife ? 80 : 190;
         int b = outOfLife ? 80 : 40;

         for (int i = 1; i < path.size(); i++) {
            line(vc, pose, path.get(i - 1), path.get(i), r, gr, b, 200);
         }

         if (impact != null && !outOfLife) {
            // Impact ring and cross, about the lethal radius of the 88 mm HE shell.
            double rad = 4.0;
            Vec3 c = impact.add(0.0, 0.08, 0.0);

            for (int i = 0; i < 32; i++) {
               double a0 = i * Math.PI * 2.0 / 32.0;
               double a1 = (i + 1) * Math.PI * 2.0 / 32.0;
               line(vc, pose, c.add(Math.cos(a0) * rad, 0.0, Math.sin(a0) * rad), c.add(Math.cos(a1) * rad, 0.0, Math.sin(a1) * rad), 255, 60, 40, 255);
            }

            line(vc, pose, c.add(-1.5, 0.0, 0.0), c.add(1.5, 0.0, 0.0), 255, 60, 40, 255);
            line(vc, pose, c.add(0.0, 0.0, -1.5), c.add(0.0, 0.0, 1.5), 255, 60, 40, 255);
            line(vc, pose, c, c.add(0.0, 6.0, 0.0), 255, 60, 40, 255);
         }

         ps.popPose();
         buffers.endBatch(RenderType.lines());
      }

      private static void line(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, int r, int g, int bl, int al) {
         Vec3 n = b.subtract(a);
         n = n.lengthSqr() > 1.0E-9 ? n.normalize() : new Vec3(0.0, 1.0, 0.0);
         vc.addVertex(pose, (float)a.x, (float)a.y, (float)a.z).setColor(r, g, bl, al).setNormal(pose, (float)n.x, (float)n.y, (float)n.z);
         vc.addVertex(pose, (float)b.x, (float)b.y, (float)b.z).setColor(r, g, bl, al).setNormal(pose, (float)n.x, (float)n.y, (float)n.z);
      }
   }
}
