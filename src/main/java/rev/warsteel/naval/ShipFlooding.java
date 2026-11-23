package rev.rev.warsteel.naval;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Flooding of a ship's watertight compartments.
 *
 * <p>The hull is divided into compartments along its length. Every hole lets water in at the
 * hydrostatic rate {@code Q = 0.6 * A * sqrt(2 g h)} (m^3/s), where h is how far the hole lies
 * below the water outside (taken from the world, so trim and list feed back into the flooding)
 * minus the water already standing inside. A compartment that fills up spills over its bulkheads
 * into its neighbours once its deck is under water - progressive flooding. Pumps take water out
 * while there is steam.
 *
 * <p>The flood water is extra weight: it pushes the ship deeper (Superb Warfare's buoyancy then
 * finds a new, lower waterline), trims it towards the flooded end and lists it towards the holed
 * side. Past its reserve buoyancy the ship goes down.
 *
 * <p>Units: metres, cubic metres (= tonnes of sea water), seconds. Positions are in the ship's
 * Vehicle frame (x to port, y up from the keel, z forward).
 */
public final class ShipFlooding {
   public record Hole(int comp, float x, float y, float z, float area) {
   }

   /** Share of the fill difference between neighbours that seeps across per second. */
   private static final double SEEPAGE = 0.0025;

   private final int n;
   private final double z0;
   private final double length;
   private final double depth;
   private final double displacement;
   private final double[] capacity;
   private final double[] floor;
   final double[] water;
   public final List<Hole> holes = new ArrayList<>();
   /** Port (+) / starboard (-) bias of the water in each compartment, from where its holes are. */
   final double[] side;
   public boolean scuttled;

   /**
    * @param compartments   watertight compartments along the hull
    * @param z0             aft end of the hull (Vehicle frame z)
    * @param length         hull length, m
    * @param beam           hull beam, m
    * @param depth          keel to main deck, m
    * @param displacement   loaded displacement, t
    */
   public ShipFlooding(int compartments, double z0, double length, double beam, double depth, double displacement) {
      this.n = compartments;
      this.z0 = z0;
      this.length = length;
      this.depth = depth;
      this.displacement = displacement;
      this.capacity = new double[compartments];
      this.floor = new double[compartments];
      this.water = new double[compartments];
      this.side = new double[compartments];
      for (int i = 0; i < compartments; i++) {
         // Fine ends: the bow and stern compartments hold far less than those amidships.
         double zc = this.centreZ(i) / (length / 2);
         double fullness = Math.max(0.25, 1.0 - 0.75 * zc * zc);
         this.capacity[i] = beam * depth * 0.85 * fullness * (length / compartments);
         this.floor[i] = this.capacity[i] / depth;
      }
   }

   public int compartments() {
      return this.n;
   }

   public double centreZ(int i) {
      return this.z0 + (i + 0.5) * this.length / this.n;
   }

   public int compartmentAt(double z) {
      int i = (int)Math.floor((z - this.z0) / (this.length / this.n));
      return Math.max(0, Math.min(this.n - 1, i));
   }

   public double totalWater() {
      double w = 0;
      for (double v : this.water) {
         w += v;
      }
      return w;
   }

   /** Share of the reserve buoyancy used up (1 = awash; beyond it the ship can't stay up). */
   public double floodRatio() {
      double reserve = 0;
      for (double c : this.capacity) {
         reserve += c;
      }
      return this.totalWater() / Math.max(1.0, reserve - this.displacement);
   }

   /** Flood water as a share of the ship's displacement (the weight it adds). */
   public double weightRatio() {
      return this.totalWater() / this.displacement;
   }

   public double fill(int i) {
      return this.water[i] / this.capacity[i];
   }

   public void addHole(int comp, float x, float y, float z, float area) {
      if (area <= 0.0F) {
         return;
      }

      // Merge with a nearby hole rather than growing the list forever.
      for (int k = 0; k < this.holes.size(); k++) {
         Hole h = this.holes.get(k);
         if (h.comp == comp && Math.abs(h.x - x) < 1.5F && Math.abs(h.y - y) < 1.5F && Math.abs(h.z - z) < 2.0F) {
            this.holes.set(k, new Hole(comp, h.x, Math.min(h.y, y), h.z, Math.min(60.0F, h.area + area)));
            this.bias(comp, x, area);
            return;
         }
      }

      if (this.holes.size() >= 48) {
         this.holes.remove(0);
      }

      this.holes.add(new Hole(comp, x, y, z, Math.min(60.0F, area)));
      this.bias(comp, x, area);
   }

   private void bias(int comp, float x, float area) {
      double s = Math.signum(x) * Math.min(1.0, Math.abs(x) / 3.0);
      this.side[comp] = Math.max(-1.0, Math.min(1.0, this.side[comp] * 0.8 + s * 0.2 * Math.min(1.0, area + 0.2)));
   }

   /** Inputs the flooding step needs from the ship. */
   public interface Hull {
      Level level();

      /** Vehicle-frame point -> world. */
      Vec3 toWorld(double x, double y, double z);
   }

   /**
    * Advances the flooding by {@code dt} seconds.
    *
    * @param pumping m^3/s the pumps can remove (0 without steam)
    * @return the water that came in this step (m^3), for effects
    */
   public double step(Hull hull, double dt, double pumping) {
      Level level = hull.level();
      double inflow = 0;
      for (Hole h : this.holes) {
         Vec3 at = hull.toWorld(h.x, h.y, h.z);
         double outside = depthBelowSurface(level, at);
         if (outside <= 0) {
            continue;
         }

         double inside = this.water[h.comp] / this.floor[h.comp];
         double head = outside - Math.max(0.0, inside - h.y);
         if (head <= 0) {
            continue;
         }

         double q = Math.min(0.6 * h.area * Math.sqrt(2 * 9.81 * head) * dt, this.capacity[h.comp] - this.water[h.comp]);
         if (q > 0) {
            this.water[h.comp] += q;
            inflow += q;
         }
      }

      // Progressive flooding: a full compartment whose deck is under water spills over its bulkheads.
      for (int i = 0; i < this.n; i++) {
         if (this.water[i] < this.capacity[i] * 0.97) {
            continue;
         }

         double deck = depthBelowSurface(level, hull.toWorld(0, this.depth, this.centreZ(i)));
         if (deck <= 0) {
            continue;
         }

         double q = 0.6 * 3.0 * Math.sqrt(2 * 9.81 * deck) * dt;
         for (int j : new int[]{i - 1, i + 1}) {
            if (j >= 0 && j < this.n && this.water[j] < this.capacity[j]) {
               double add = Math.min(q / 2, this.capacity[j] - this.water[j]);
               this.water[j] += add;
               this.side[j] = this.side[j] * 0.9 + this.side[i] * 0.1;
               inflow += add;
            }
         }
      }

      // Seepage: wartime bulkheads, doors and ventilation trunks were never truly watertight. Water
      // creeps from a fuller compartment into its neighbours, so a badly holed ship keeps losing
      // ground unless the pumps keep up.
      for (int i = 0; i + 1 < this.n; i++) {
         double diff = this.fill(i) - this.fill(i + 1);
         double q = SEEPAGE * Math.abs(diff) * Math.min(this.capacity[i], this.capacity[i + 1]) * dt;
         int from = diff > 0 ? i : i + 1;
         int to = diff > 0 ? i + 1 : i;
         q = Math.min(q, Math.min(this.water[from], this.capacity[to] - this.water[to]));
         if (q > 0) {
            this.water[from] -= q;
            this.water[to] += q;
         }
      }

      // Pumps work on the worst compartment that isn't open to the sea faster than they can cope.
      if (pumping > 0) {
         int worst = -1;
         for (int i = 0; i < this.n; i++) {
            if (this.water[i] > 0.01 && (worst < 0 || this.water[i] > this.water[worst])) {
               worst = i;
            }
         }

         if (worst >= 0) {
            this.water[worst] = Math.max(0, this.water[worst] - pumping * dt);
         }
      }

      return inflow;
   }

   /** Bow-down trim the flooding produces, degrees (positive = bow down). */
   public double trimDegrees() {
      double moment = 0;
      for (int i = 0; i < this.n; i++) {
         moment += this.water[i] * this.centreZ(i);
      }

      // Longitudinal metacentric height of a slim hull is about its length.
      return Math.toDegrees(Math.atan2(moment, (this.displacement + this.totalWater()) * this.length * 0.6));
   }

   /** List the flooding produces, degrees (positive = towards port). */
   public double listDegrees() {
      double moment = 0;
      for (int i = 0; i < this.n; i++) {
         moment += this.water[i] * this.side[i] * 2.2;
      }

      // Transverse metacentric height of a destroyer: about 0.9 m.
      return Math.toDegrees(Math.atan2(moment, (this.displacement + this.totalWater()) * 0.9));
   }

   /** Metres the point lies below the water surface above it (0 if it's not in water). */
   public static double depthBelowSurface(Level level, Vec3 at) {
      BlockPos p = BlockPos.containing(at);
      if (!level.getFluidState(p).is(FluidTags.WATER)) {
         return 0;
      }

      BlockPos.MutableBlockPos m = p.mutable();
      int steps = 0;
      while (steps++ < 48 && level.getFluidState(m.above()).is(FluidTags.WATER)) {
         m.move(0, 1, 0);
      }

      double surface = m.getY() + level.getFluidState(m).getHeight(level, m);
      return Math.max(0, surface - at.y);
   }

   public CompoundTag save() {
      CompoundTag t = new CompoundTag();
      ListTag w = new ListTag();
      for (int i = 0; i < this.n; i++) {
         CompoundTag c = new CompoundTag();
         c.putDouble("W", this.water[i]);
         c.putDouble("S", this.side[i]);
         w.add(c);
      }

      t.put("Compartments", w);
      ListTag hs = new ListTag();
      for (Hole h : this.holes) {
         CompoundTag c = new CompoundTag();
         c.putInt("C", h.comp);
         c.putFloat("X", h.x);
         c.putFloat("Y", h.y);
         c.putFloat("Z", h.z);
         c.putFloat("A", h.area);
         hs.add(c);
      }

      t.put("Holes", hs);
      t.putBoolean("Scuttled", this.scuttled);
      return t;
   }

   public void load(CompoundTag t) {
      ListTag w = t.getList("Compartments", Tag.TAG_COMPOUND);
      for (int i = 0; i < Math.min(this.n, w.size()); i++) {
         this.water[i] = Math.min(this.capacity[i], w.getCompound(i).getDouble("W"));
         this.side[i] = w.getCompound(i).getDouble("S");
      }

      this.holes.clear();
      ListTag hs = t.getList("Holes", Tag.TAG_COMPOUND);
      for (int i = 0; i < hs.size(); i++) {
         CompoundTag c = hs.getCompound(i);
         this.holes.add(new Hole(Math.max(0, Math.min(this.n - 1, c.getInt("C"))), c.getFloat("X"), c.getFloat("Y"), c.getFloat("Z"), c.getFloat("A")));
      }

      this.scuttled = t.getBoolean("Scuttled");
   }

   /** Patch every hole and pump the ship dry (repairs, /warsteel reset). */
   public void reset() {
      this.holes.clear();
      java.util.Arrays.fill(this.water, 0);
      java.util.Arrays.fill(this.side, 0);
      this.scuttled = false;
   }
}
