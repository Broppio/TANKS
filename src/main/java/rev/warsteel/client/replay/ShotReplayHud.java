package rev.rev.warsteel.client.replay;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import rev.rev.warsteel.network.ShotReplayPayload;

/**
 * War Thunder style hit camera: a small x-ray of the target in the bottom-right corner. The
 * shell flies in, the core, spall and fragments spread through the hull, and each module turns
 * orange (damaged) or black (destroyed) at the moment the replay reaches it.
 */
public final class ShotReplayHud implements LayeredDraw.Layer {
   public static final ShotReplayHud INSTANCE = new ShotReplayHud();
   public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("warsteel", "shot_replay");
   private static final int W = 196;
   private static final int H = 112;
   /** Seconds for the shell to fly in. */
   private static final float APPROACH = 0.6F;
   /** Seconds per block of travel inside the hull. */
   private static final float PER_BLOCK = 0.22F;
   private static final float HOLD = 2.6F;
   private static final float FADE = 0.45F;
   private ShotReplayPayload replay;
   private long startNanos;
   private float duration;
   private Vector3f centre = new Vector3f();
   private float scale = 1.0F;
   private float baseYaw;

   private ShotReplayHud() {
   }

   public void play(ShotReplayPayload p) {
      this.replay = p;
      this.startNanos = System.nanoTime();
      float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
      float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};

      for (ShotReplayPayload.Box b : p.boxes()) {
         for (int i = 0; i < 24; i++) {
            min[i % 3] = Math.min(min[i % 3], b.corners()[i]);
            max[i % 3] = Math.max(max[i % 3], b.corners()[i]);
         }
      }

      this.centre = new Vector3f((min[0] + max[0]) / 2, (min[1] + max[1]) / 2, (min[2] + max[2]) / 2);
      float radius = 0.5F * (float)Math.sqrt(sq(max[0] - min[0]) + sq(max[1] - min[1]) + sq(max[2] - min[2]));
      this.scale = (H * 0.46F) / Math.max(1.0F, radius);
      Vector3f d = new Vector3f(p.shellHit()).sub(p.shellFrom());
      this.baseYaw = (float)Math.atan2(d.x, d.z);
      float end = APPROACH;

      for (ShotReplayPayload.Ray r : p.rays()) {
         end = Math.max(end, time(r.t0() + r.from().distance(r.to())));
      }

      if (p.shellOut() != null) {
         end = Math.max(end, APPROACH + 0.5F);
      }

      this.duration = end + HOLD;
   }

   private static float time(float blocks) {
      return APPROACH + blocks * PER_BLOCK;
   }

   @Override
   public void render(GuiGraphics g, DeltaTracker delta) {
      ShotReplayPayload p = this.replay;
      Minecraft mc = Minecraft.getInstance();
      if (p == null || mc.options.hideGui) {
         return;
      }

      float t = (System.nanoTime() - this.startNanos) / 1.0E9F;
      if (t > this.duration) {
         this.replay = null;
         return;
      }

      float alpha = t > this.duration - FADE ? Math.max(0.0F, (this.duration - t) / FADE) : Math.min(1.0F, t / 0.15F);
      int x0 = g.guiWidth() - W - 6;
      int y0 = g.guiHeight() - H - 78;
      int cx = x0 + W / 2;
      int cy = y0 + H / 2 + 6;
      g.fill(x0 - 2, y0 - 2, x0 + W + 2, y0 + H + 2, argb(0.78F * alpha, 0x0C0E10));
      g.renderOutline(x0 - 2, y0 - 2, W + 4, H + 4, argb(0.9F * alpha, 0x5A6470));
      g.enableScissor(x0, y0, x0 + W, y0 + H);
      // Camera: from beside and above the shot line, turning slowly.
      float yaw = this.baseYaw + 0.75F + t * 0.18F;
      float pitch = 0.5F;
      Matrix4f m = g.pose().last().pose();
      VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
      List<Face> faces = new ArrayList<>();

      for (ShotReplayPayload.Box b : p.boxes()) {
         float[] s = new float[24];

         for (int i = 0; i < 8; i++) {
            float[] q = this.project(b.corners()[i * 3], b.corners()[i * 3 + 1], b.corners()[i * 3 + 2], yaw, pitch, cx, cy);
            s[i * 3] = q[0];
            s[i * 3 + 1] = q[1];
            s[i * 3 + 2] = q[2];
         }

         int fill = boxColour(b, t);
         for (int[] f : FACES) {
            float depth = (s[f[0] * 3 + 2] + s[f[1] * 3 + 2] + s[f[2] * 3 + 2] + s[f[3] * 3 + 2]) / 4.0F;
            faces.add(new Face(s, f, depth, mulAlpha(fill, alpha), b.kind() == 0));
         }
      }

      faces.sort(Comparator.comparingDouble((Face f) -> f.depth).reversed());

      for (Face f : faces) {
         quad(vc, m, f.s, f.idx, f.colour);
      }

      // Plate and module edges on top for definition.
      for (ShotReplayPayload.Box b : p.boxes()) {
         int edge = b.kind() == 0 ? (b.entry() ? 0xFFFF5040 : 0x70FFB060) : 0x50FFFFFF;
         float[] s = new float[24];

         for (int i = 0; i < 8; i++) {
            float[] q = this.project(b.corners()[i * 3], b.corners()[i * 3 + 1], b.corners()[i * 3 + 2], yaw, pitch, cx, cy);
            System.arraycopy(q, 0, s, i * 3, 3);
         }

         for (int[] e : EDGES) {
            line(vc, m, s[e[0] * 3], s[e[0] * 3 + 1], s[e[1] * 3], s[e[1] * 3 + 1], b.entry() ? 1.1F : 0.5F, mulAlpha(edge, alpha));
         }
      }

      // The shell flying in, and bouncing off on a ricochet.
      float approach = Math.min(1.0F, t / APPROACH);
      Vector3f from = p.shellFrom();
      Vector3f hit = p.shellHit();
      Vector3f tip = new Vector3f(from).lerp(hit, approach);
      Vector3f tail = new Vector3f(from).lerp(hit, Math.max(0.0F, approach - 0.35F));
      this.line3(vc, m, tail, tip, 1.6F, mulAlpha(0xFFFFE070, alpha), yaw, pitch, cx, cy);
      if (p.shellOut() != null && t > APPROACH) {
         float k = Math.min(1.0F, (t - APPROACH) / 0.5F);
         this.line3(vc, m, hit, new Vector3f(hit).lerp(p.shellOut(), k), 1.2F, mulAlpha(0xFF60D0FF, alpha), yaw, pitch, cx, cy);
      }

      // Core, spall and fragments growing outward from where they start.
      for (ShotReplayPayload.Ray r : p.rays()) {
         float start = time(r.t0());
         if (t < start) {
            continue;
         }

         float len = r.from().distance(r.to());
         float k = len <= 0.0F ? 1.0F : Math.min(1.0F, (t - start) / (len * PER_BLOCK));
         Vector3f end = new Vector3f(r.from()).lerp(r.to(), k);
         int colour = switch (r.kind()) {
            case 0 -> 0xFFFFF4C0;
            case 1 -> 0xE0FF8A30;
            default -> 0xD0FFD050;
         };
         this.line3(vc, m, r.from(), end, r.kind() == 0 ? 1.5F : 0.7F, mulAlpha(colour, alpha), yaw, pitch, cx, cy);
      }

      if (p.burst() != null && t >= time(p.burstTime()) && t < time(p.burstTime()) + 0.6F) {
         float[] q = this.project(p.burst().x, p.burst().y, p.burst().z, yaw, pitch, cx, cy);
         float r = 3.0F + 9.0F * (t - time(p.burstTime())) / 0.6F;
         int c = mulAlpha(0xC0FFE080, alpha * (1.0F - (t - time(p.burstTime())) / 0.6F));
         quadXY(vc, m, q[0] - r, q[1] - r * 0.25F, q[0] + r, q[1] + r * 0.25F, c);
         quadXY(vc, m, q[0] - r * 0.25F, q[1] - r, q[0] + r * 0.25F, q[1] + r, c);
      }

      g.flush();
      g.disableScissor();
      Font font = mc.font;
      int a = (int)(alpha * 255.0F) << 24;
      if (alpha > 0.05F) {
         g.drawString(font, p.title(), x0 + 4, y0 + 3, 0xFFFFFF | a, true);
         List<ShotReplayPayload.Line> lines = p.lines();
         if (!lines.isEmpty() && t > APPROACH) {
            ShotReplayPayload.Line head = lines.get(0);
            g.drawString(font, outcome(head.text()), x0 + 4, y0 + 13, head.colour() | a, true);
         }

         // What this shot did, revealed as the replay reaches it.
         g.pose().pushPose();
         g.pose().translate(x0 + 4, y0 + H - 4, 0.0F);
         g.pose().scale(0.75F, 0.75F, 1.0F);
         int shown = 0;
         float progress = Math.min(1.0F, (t - APPROACH) / Math.max(0.3F, this.duration - HOLD - APPROACH));

         for (int i = 1; i < lines.size() && shown < 4; i++) {
            if (progress >= (float)i / lines.size()) {
               shown++;
            }
         }

         for (int i = 0; i < shown; i++) {
            ShotReplayPayload.Line l = lines.get(i + 1);
            g.drawString(font, l.text(), 0, -10 * (shown - i), l.colour() | a, true);
         }

         g.pose().popPose();
      }
   }

   // ---------------------------------------------------------------------------------------------

   /** The outcome word(s) at the start of the hit headline ("NO PENETRATION", "RICOCHET", ...). */
   private static String outcome(String headline) {
      for (String o : new String[]{"NO PENETRATION", "PENETRATION", "RICOCHET", "ABSORBED", "HIT"}) {
         if (headline.startsWith(o)) {
            return o;
         }
      }

      int cut = headline.indexOf(' ');
      return cut > 0 ? headline.substring(0, cut) : headline;
   }

   private int boxColour(ShotReplayPayload.Box b, float t) {
      if (b.kind() == 0) {
         return b.entry() ? 0x55FF5030 : 0x22FFA040;
      } else if (b.state() == ShotReplayPayload.Box.WAS_OUT) {
         return 0x90303030;
      } else if (b.state() != ShotReplayPayload.Box.INTACT && t >= time(b.hitTime())) {
         if (b.state() == ShotReplayPayload.Box.DESTROYED) {
            boolean flash = t < time(b.hitTime()) + 0.25F;
            return flash ? 0xF0FF3020 : (b.kind() == 1 ? 0xE0701010 : 0xE0181818);
         }

         return 0xD0FF9020;
      } else {
         return switch (b.kind()) {
            case 1 -> 0x804A86FF;
            case 2 -> 0x80C04040;
            case 3 -> 0x80E0C030;
            case 4 -> 0x80C040C0;
            case 5 -> 0x80C8C8C8;
            case 6 -> 0x8030C0C0;
            default -> 0x6040A040;
         };
      }
   }

   private float[] project(float x, float y, float z, float yaw, float pitch, int cx, int cy) {
      float px = x - this.centre.x;
      float py = y - this.centre.y;
      float pz = z - this.centre.z;
      float cosY = Mth.cos(yaw);
      float sinY = Mth.sin(yaw);
      float x1 = px * cosY - pz * sinY;
      float z1 = px * sinY + pz * cosY;
      float cosP = Mth.cos(pitch);
      float sinP = Mth.sin(pitch);
      float y2 = py * cosP - z1 * sinP;
      float z2 = py * sinP + z1 * cosP;
      // Mild perspective so depth reads clearly.
      float persp = 1.0F / (1.0F + z2 * 0.035F);
      return new float[]{cx - x1 * this.scale * persp, cy - y2 * this.scale * persp, z2};
   }

   private void line3(VertexConsumer vc, Matrix4f m, Vector3f a, Vector3f b, float w, int colour, float yaw, float pitch, int cx, int cy) {
      float[] p = this.project(a.x, a.y, a.z, yaw, pitch, cx, cy);
      float[] q = this.project(b.x, b.y, b.z, yaw, pitch, cx, cy);
      line(vc, m, p[0], p[1], q[0], q[1], w, colour);
   }

   private static void line(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2, float w, int colour) {
      float dx = x2 - x1;
      float dy = y2 - y1;
      float len = (float)Math.sqrt(dx * dx + dy * dy);
      if (len < 0.01F) {
         return;
      }

      float nx = -dy / len * w * 0.5F;
      float ny = dx / len * w * 0.5F;
      emit(vc, m, new float[]{x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 - nx, y2 - ny, x1 - nx, y1 - ny}, colour);
   }

   private static void quad(VertexConsumer vc, Matrix4f m, float[] s, int[] idx, int colour) {
      emit(vc, m, new float[]{s[idx[0] * 3], s[idx[0] * 3 + 1], s[idx[1] * 3], s[idx[1] * 3 + 1], s[idx[2] * 3], s[idx[2] * 3 + 1], s[idx[3] * 3], s[idx[3] * 3 + 1]}, colour);
   }

   private static void quadXY(VertexConsumer vc, Matrix4f m, float xa, float ya, float xb, float yb, int colour) {
      emit(vc, m, new float[]{xa, ya, xb, ya, xb, yb, xa, yb}, colour);
   }

   /** One quad, wound so it survives back-face culling whichever way it faces on screen. */
   private static void emit(VertexConsumer vc, Matrix4f m, float[] v, int colour) {
      float area = 0.0F;

      for (int i = 0; i < 4; i++) {
         int j = (i + 1) % 4;
         area += v[i * 2] * v[j * 2 + 1] - v[j * 2] * v[i * 2 + 1];
      }

      int[] order = area > 0.0F ? new int[]{0, 3, 2, 1} : new int[]{0, 1, 2, 3};

      for (int i : order) {
         vc.addVertex(m, v[i * 2], v[i * 2 + 1], 0.0F).setColor(colour);
      }
   }

   private static int mulAlpha(int argb, float k) {
      int a = (int)(((argb >>> 24) & 0xFF) * Mth.clamp(k, 0.0F, 1.0F));
      return a << 24 | argb & 0xFFFFFF;
   }

   private static int argb(float a, int rgb) {
      return (int)(Mth.clamp(a, 0.0F, 1.0F) * 255.0F) << 24 | rgb;
   }

   private static float sq(float v) {
      return v * v;
   }

   private record Face(float[] s, int[] idx, float depth, int colour, boolean plate) {
   }

   /** Corner index = (x>0 ? 4 : 0) + (y>0 ? 2 : 0) + (z>0 ? 1 : 0). */
   private static final int[][] FACES = {{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
   private static final int[][] EDGES = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
}
