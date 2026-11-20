package rev.rev.warsteel.damage;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector4d;

/**
 * An oriented box in world space, built from a vehicle-local position and half-extents
 * under one of Superb Warfare's named transforms ("Vehicle", "Turret", ...).
 */
public final class Obb {
   public final Vec3 centre;
   public final Quaterniond rotation;
   private final Quaterniond inverse;
   public final double hx;
   public final double hy;
   public final double hz;

   public Obb(Vec3 centre, Quaterniond rotation, double hx, double hy, double hz) {
      this.centre = centre;
      this.rotation = new Quaterniond(rotation);
      this.inverse = new Quaterniond(rotation).conjugate();
      this.hx = Math.abs(hx);
      this.hy = Math.abs(hy);
      this.hz = Math.abs(hz);
   }

   public static Obb of(VehicleEntity vehicle, String transform, double[] position, double[] size) {
      Matrix4d m = vehicle.getTransformFromString(transform);
      Vector4d c = vehicle.transformPosition(m, position[0], position[1], position[2]);
      Quaterniond q = vehicle.getRotationFromString(transform);
      return new Obb(new Vec3(c.x, c.y, c.z), q, size[0], size[1], size[2]);
   }

   public Vector3d toLocal(Vec3 p) {
      Vector3d r = new Vector3d(p.x - this.centre.x, p.y - this.centre.y, p.z - this.centre.z);
      return this.inverse.transform(r);
   }

   public Vector3d dirToLocal(Vec3 d) {
      return this.inverse.transform(new Vector3d(d.x, d.y, d.z));
   }

   public Vec3 toWorld(double x, double y, double z) {
      Vector3d v = this.rotation.transform(new Vector3d(x, y, z));
      return new Vec3(this.centre.x + v.x, this.centre.y + v.y, this.centre.z + v.z);
   }

   public Vec3 axis(int i) {
      Vector3d v = this.rotation.transform(new Vector3d(i == 0 ? 1 : 0, i == 1 ? 1 : 0, i == 2 ? 1 : 0));
      return new Vec3(v.x, v.y, v.z);
   }

   /** Squared distance from a point to the box surface (0 inside). */
   public double distanceSqr(Vec3 p) {
      Vector3d l = this.toLocal(p);
      double dx = Math.max(0.0, Math.abs(l.x) - this.hx);
      double dy = Math.max(0.0, Math.abs(l.y) - this.hy);
      double dz = Math.max(0.0, Math.abs(l.z) - this.hz);
      return dx * dx + dy * dy + dz * dz;
   }

   /**
    * Slab test. Returns {tEnter, tExit} along origin + t * dir (dir need not be unit; t is in
    * multiples of dir), or null when the infinite line misses the box or the box is behind.
    */
   @Nullable
   public double[] intersect(Vec3 origin, Vec3 dir) {
      Vector3d o = this.toLocal(origin);
      Vector3d d = this.dirToLocal(dir);
      double tMin = Double.NEGATIVE_INFINITY;
      double tMax = Double.POSITIVE_INFINITY;
      double[] os = {o.x, o.y, o.z};
      double[] ds = {d.x, d.y, d.z};
      double[] hs = {this.hx, this.hy, this.hz};

      for (int i = 0; i < 3; i++) {
         if (Math.abs(ds[i]) < 1.0E-9) {
            if (os[i] < -hs[i] || os[i] > hs[i]) {
               return null;
            }
         } else {
            double t1 = (-hs[i] - os[i]) / ds[i];
            double t2 = (hs[i] - os[i]) / ds[i];
            if (t1 > t2) {
               double s = t1;
               t1 = t2;
               t2 = s;
            }

            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
               return null;
            }
         }
      }

      return tMax < 0.0 ? null : new double[]{tMin, tMax};
   }

   /** The 12 edges as pairs of world points, for debug drawing. */
   public Vec3[][] edges() {
      Vec3[] c = new Vec3[8];
      int n = 0;

      for (int x = -1; x <= 1; x += 2) {
         for (int y = -1; y <= 1; y += 2) {
            for (int z = -1; z <= 1; z += 2) {
               c[n++] = this.toWorld(x * this.hx, y * this.hy, z * this.hz);
            }
         }
      }

      int[][] idx = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
      Vec3[][] out = new Vec3[12][];

      for (int i = 0; i < 12; i++) {
         out[i] = new Vec3[]{c[idx[i][0]], c[idx[i][1]]};
      }

      return out;
   }
}
