package rev.rev.warsteel.damage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector4d;

/**
 * Marks left on a tank's armour by hits. Positions are stored in the frame of the plate that was
 * struck ("Vehicle" or "Turret"), so marks on the turret turn with it. They stay until the tank
 * is repaired with Superb Warfare's repair tool (or /warsteel reset).
 */
public final class HitDecals {
   public static final int MAX = 40;
   public static final byte PENETRATION = 0;
   public static final byte NON_PENETRATION = 1;
   public static final byte RICOCHET = 2;
   public static final byte SCORCH = 3;

   /** frame 0 = Vehicle, 1 = Turret; p = position, n = outward normal, t = along-shot tangent (all in that frame). */
   public record Decal(byte frame, float px, float py, float pz, float nx, float ny, float nz, float tx, float ty, float tz, byte kind, float size, float aspect) {
      public String frameName() {
         return this.frame == 1 ? "Turret" : "Vehicle";
      }
   }

   private HitDecals() {
   }

   /**
    * @param transform the struck plate's frame
    * @param normal    outward plate normal (world)
    * @param dir       shell direction (world)
    */
   public static void add(WarSteelTank tank, String transform, Vec3 hit, Vec3 normal, Vec3 dir, byte kind, int calibre) {
      Matrix4d toLocal = new Matrix4d(tank.getTransformFromString(transform)).invert();
      Quaterniond inv = new Quaterniond(tank.getRotationFromString(transform)).conjugate();
      Vector4d p = toLocal.transform(new Vector4d(hit.x, hit.y, hit.z, 1.0));
      Vector3d n = inv.transform(new Vector3d(normal.x, normal.y, normal.z)).normalize();
      Vector3d d = inv.transform(new Vector3d(dir.x, dir.y, dir.z)).normalize();
      // Tangent: the shot direction flattened onto the plate (direction the mark is stretched).
      Vector3d t = new Vector3d(d).sub(new Vector3d(n).mul(d.dot(n)));
      if (t.lengthSquared() < 1.0E-6) {
         t = Math.abs(n.y) < 0.9 ? new Vector3d(0, 1, 0).cross(n) : new Vector3d(1, 0, 0).cross(n);
      }

      t.normalize();
      double cos = Math.min(1.0, Math.abs(d.dot(n)));
      float size = (float)Math.max(0.24, calibre * (kind == SCORCH ? 0.009 : 0.0042));
      float aspect = switch (kind) {
         case RICOCHET -> 3.2F;
         case SCORCH -> 1.0F;
         default -> (float)Math.min(2.4, 1.0 / Math.max(0.42, cos));
      };
      Decal decal = new Decal(
         (byte)(transform.equals("Turret") ? 1 : 0), (float)p.x, (float)p.y, (float)p.z, (float)n.x, (float)n.y, (float)n.z, (float)t.x, (float)t.y, (float)t.z, kind, size, aspect
      );
      tank.damageState().addDecal(decal);
   }

   static String encode(List<Decal> decals) {
      StringBuilder sb = new StringBuilder();

      for (Decal d : decals) {
         sb.append(String.format(Locale.ROOT, "%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%.3f,%.2f;",
            d.frame, d.px, d.py, d.pz, d.nx, d.ny, d.nz, d.tx, d.ty, d.tz, d.kind, d.size, d.aspect));
      }

      return sb.toString();
   }

   static List<Decal> decode(String s) {
      List<Decal> out = new ArrayList<>();
      if (s == null || s.isEmpty()) {
         return out;
      }

      for (String part : s.split(";")) {
         String[] f = part.split(",");
         if (f.length != 13) {
            continue;
         }

         try {
            out.add(new Decal(Byte.parseByte(f[0]), Float.parseFloat(f[1]), Float.parseFloat(f[2]), Float.parseFloat(f[3]), Float.parseFloat(f[4]), Float.parseFloat(f[5]),
               Float.parseFloat(f[6]), Float.parseFloat(f[7]), Float.parseFloat(f[8]), Float.parseFloat(f[9]), Byte.parseByte(f[10]), Float.parseFloat(f[11]), Float.parseFloat(f[12])));
         } catch (NumberFormatException ignored) {
         }
      }

      return out;
   }
}
