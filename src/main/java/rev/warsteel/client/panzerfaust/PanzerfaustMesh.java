package rev.rev.warsteel.client.panzerfaust;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * The Panzerfaust 60 triangle mesh (see tools/panzerfaust_export.py): parts "tube", "sights" and
 * "warhead", each a flat list of x, y, z, u, v, nx, ny, nz per corner, three corners per
 * triangle, in blocks. Drawn into quad buffers by repeating each triangle's last corner.
 */
public final class PanzerfaustMesh {
   public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("warsteel", "textures/item/panzerfaust_60.png");
   private static final ResourceLocation MESH = ResourceLocation.fromNamespaceAndPath("warsteel", "models/panzerfaust/panzerfaust_60.mesh.json");
   private static Map<String, float[]> parts;
   private static float[] warheadCentre;

   private PanzerfaustMesh() {
   }

   private static Map<String, float[]> parts() {
      if (parts == null) {
         Map<String, float[]> out = new HashMap<>();
         try (Reader r = new InputStreamReader(Minecraft.getInstance().getResourceManager().open(MESH), StandardCharsets.UTF_8)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            for (String k : o.keySet()) {
               JsonArray a = o.getAsJsonArray(k);
               float[] f = new float[a.size()];
               for (int i = 0; i < f.length; i++) {
                  f[i] = a.get(i).getAsFloat();
               }
               out.put(k, f);
            }
         } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("warsteel/panzerfaust").error("could not load {}", MESH, e);
         }
         parts = out;
      }

      return parts;
   }

   /** Centre of the warhead part, for drawing the projectile around its own middle. */
   public static float[] warheadCentre() {
      if (warheadCentre == null) {
         float[] w = parts().getOrDefault("warhead", new float[0]);
         float[] c = new float[3];
         int n = 0;
         for (int i = 0; i < w.length; i += 8, n++) {
            c[0] += w[i];
            c[1] += w[i + 1];
            c[2] += w[i + 2];
         }
         if (n > 0) {
            c[0] /= n;
            c[1] /= n;
            c[2] /= n;
         }
         warheadCentre = c;
      }

      return warheadCentre;
   }

   public static void draw(String part, PoseStack.Pose pose, VertexConsumer vc, int light, int overlay, int colour) {
      float[] f = parts().get(part);
      if (f == null) {
         return;
      }

      for (int t = 0; t < f.length; t += 24) {
         for (int c = 0; c < 4; c++) {
            int i = t + Math.min(c, 2) * 8;
            vc.addVertex(pose, f[i], f[i + 1], f[i + 2])
               .setColor(colour)
               .setUv(f[i + 3], f[i + 4])
               .setOverlay(overlay)
               .setLight(light)
               .setNormal(pose, f[i + 5], f[i + 6], f[i + 7]);
         }
      }
   }
}
