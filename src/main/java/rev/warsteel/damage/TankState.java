package rev.rev.warsteel.damage;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable damage state of one tank. On the server it is authoritative; on the client it is
 * rebuilt from the synced status string so HUD and input checks (can I fire? can I traverse?)
 * agree with the server.
 */
public final class TankState {
   @Nullable
   private DamageModel model;
   private float[] health = new float[0];
   private int[] swapTicks = new int[0];
   int fireTicks;
   int fireModule = -1;
   int quietTicks;
   int crewDownTicks;
   boolean knockedOut;
   boolean detonated;
   boolean dirty = true;

   // Client-side view, parsed from the sync string.
   private final List<Entry> clientEntries = new ArrayList<>();
   private String clientFlags = "";
   private String lastParsed = null;

   public record Entry(String label, String kind, int percent) {
   }

   /** Ensures the arrays match the current data-pack model (data can be reloaded at runtime). */
   public void bind(@Nullable DamageModel m) {
      if (m == this.model) {
         return;
      }

      float[] old = this.health;
      DamageModel oldModel = this.model;
      this.model = m;
      if (m == null) {
         this.health = new float[0];
         this.swapTicks = new int[0];
         return;
      }

      this.health = new float[m.modules().size()];
      this.swapTicks = new int[m.modules().size()];

      for (ModuleDef def : m.modules()) {
         float h = def.maxHealth();
         if (oldModel != null) {
            int j = oldModel.indexOf(def.name());
            if (j >= 0 && j < old.length) {
               h = Math.min(def.maxHealth(), old[j]);
            }
         }

         this.health[def.index()] = h;
      }

      this.dirty = true;
   }

   @Nullable
   public DamageModel model() {
      return this.model;
   }

   public float health(int i) {
      return this.health[i];
   }

   void setHealth(int i, float h) {
      this.health[i] = h;
      this.dirty = true;
   }

   public boolean destroyed(int i) {
      return this.health[i] <= 0.0F;
   }

   int swapTicks(int i) {
      return this.swapTicks[i];
   }

   void setSwapTicks(int i, int t) {
      this.swapTicks[i] = t;
   }

   public boolean burning() {
      return this.fireTicks > 0;
   }

   public boolean knockedOut() {
      return this.knockedOut;
   }

   public int crewAlive() {
      if (this.model == null) {
         return 0;
      }

      int n = 0;

      for (ModuleDef d : this.model.modules()) {
         if (d.type() == ModuleType.CREW && !this.destroyed(d.index())) {
            n++;
         }
      }

      return n;
   }

   public int crewTotal() {
      if (this.model == null) {
         return 0;
      }

      int n = 0;

      for (ModuleDef d : this.model.modules()) {
         if (d.type() == ModuleType.CREW) {
            n++;
         }
      }

      return n;
   }

   /** True if any module of this type (and role, if given) exists and every one of them is out. */
   public boolean allOut(ModuleType type, @Nullable CrewRole role) {
      if (this.model == null) {
         return false;
      }

      boolean any = false;

      for (ModuleDef d : this.model.modules()) {
         if (d.type() == type && (role == null || d.role() == role)) {
            any = true;
            if (!this.destroyed(d.index())) {
               return false;
            }
         }
      }

      return any;
   }

   public boolean trackOut(String side) {
      if (this.model == null) {
         return false;
      }

      for (ModuleDef d : this.model.modules()) {
         if (d.type() == ModuleType.TRACK && side.equalsIgnoreCase(d.side()) && this.destroyed(d.index())) {
            return true;
         }
      }

      return false;
   }

   // ---- derived gameplay flags (server computes from health; client reads from sync flags) ----

   public boolean immobile(boolean client) {
      if (client) {
         return this.clientFlags.contains("M");
      }

      return this.allOut(ModuleType.ENGINE, null) || this.allOut(ModuleType.TRANSMISSION, null) || this.allOut(ModuleType.CREW, CrewRole.DRIVER);
   }

   public boolean turretLocked(boolean client) {
      if (client) {
         return this.clientFlags.contains("T");
      }

      return this.allOut(ModuleType.TURRET_DRIVE, null) || this.allOut(ModuleType.CREW, CrewRole.GUNNER);
   }

   public boolean mainGunDisabled(boolean client) {
      if (client) {
         return this.clientFlags.contains("G");
      }

      return this.allOut(ModuleType.BREECH, null) || this.allOut(ModuleType.BARREL, null) || this.allOut(ModuleType.CREW, CrewRole.GUNNER);
   }

   /** Whether the crew member assigned to this seat (if any) is out. */
   public boolean seatCrewOut(int seat, boolean client) {
      if (client) {
         return this.clientFlags.contains("S" + seat + ",");
      }

      if (this.model == null) {
         return false;
      }

      for (ModuleDef d : this.model.modules()) {
         if (d.type() == ModuleType.CREW && d.seat() == seat && this.destroyed(d.index())) {
            return true;
         }
      }

      return false;
   }

   // ---- sync ----

   /** Format: flags|label~kind~percent;label~kind~percent;... */
   public String encode() {
      if (this.model == null) {
         return "";
      }

      StringBuilder f = new StringBuilder();
      if (this.burning()) {
         f.append('F');
      }

      if (this.knockedOut) {
         f.append('K');
      }

      if (this.immobile(false)) {
         f.append('M');
      }

      if (this.turretLocked(false)) {
         f.append('T');
      }

      if (this.mainGunDisabled(false)) {
         f.append('G');
      }

      for (ModuleDef d : this.model.modules()) {
         if (d.type() == ModuleType.CREW && d.seat() >= 0 && this.destroyed(d.index())) {
            f.append('S').append(d.seat()).append(',');
         }
      }

      StringBuilder s = new StringBuilder(f).append('|');

      for (ModuleDef d : this.model.modules()) {
         int pct = (int)Math.ceil(100.0F * Math.max(0.0F, this.health[d.index()]) / d.maxHealth());
         s.append(d.label()).append('~').append(d.type().name()).append('~').append(pct).append(';');
      }

      return s.toString();
   }

   public void decode(String s) {
      if (s.equals(this.lastParsed)) {
         return;
      }

      this.lastParsed = s;
      this.clientEntries.clear();
      int bar = s.indexOf('|');
      if (bar < 0) {
         this.clientFlags = "";
         return;
      }

      this.clientFlags = s.substring(0, bar);

      for (String part : s.substring(bar + 1).split(";")) {
         String[] p = part.split("~");
         if (p.length == 3) {
            try {
               this.clientEntries.add(new Entry(p[0], p[1], Integer.parseInt(p[2])));
            } catch (NumberFormatException ignored) {
            }
         }
      }
   }

   public List<Entry> clientEntries() {
      return this.clientEntries;
   }

   public String clientFlags() {
      return this.clientFlags;
   }

   // ---- persistence ----

   public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      if (this.model != null) {
         CompoundTag h = new CompoundTag();

         for (ModuleDef d : this.model.modules()) {
            h.putFloat(d.name(), this.health[d.index()]);
         }

         tag.put("Health", h);
         tag.putInt("FireTicks", this.fireTicks);
         tag.putString("FireModule", this.fireModule >= 0 ? this.model.modules().get(this.fireModule).name() : "");
      }

      tag.putBoolean("KnockedOut", this.knockedOut);
      return tag;
   }

   public void load(CompoundTag tag, @Nullable DamageModel m) {
      this.bind(m);
      this.knockedOut = tag.getBoolean("KnockedOut");
      if (this.model != null && tag.contains("Health")) {
         CompoundTag h = tag.getCompound("Health");

         for (ModuleDef d : this.model.modules()) {
            if (h.contains(d.name())) {
               this.health[d.index()] = Math.min(d.maxHealth(), h.getFloat(d.name()));
            }
         }

         this.fireTicks = tag.getInt("FireTicks");
         this.fireModule = this.model.indexOf(tag.getString("FireModule"));
      }

      this.dirty = true;
   }
}
