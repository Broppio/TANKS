package rev.rev.warsteel.damage;

import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rev.rev.warsteel.combat.ArmorData;
import rev.rev.warsteel.combat.ArmorPlate;
import rev.rev.warsteel.combat.ShellData;

/**
 * War Thunder style damage: a tank has no hit points. Shells that defeat the armour are traced
 * through the hull as a core plus fragments (or a burst, for explosive rounds), and whatever
 * modules those rays cross take damage. The tank is knocked out when its ammunition detonates or
 * too few crew are left to fight it.
 */
public final class TankDamage {
   /** Shells whose hit we already resolved; SBW's follow-up damage from them is ignored. */
   private static final Set<Entity> HANDLED = Collections.newSetFromMap(new WeakHashMap<>());
   /** Fraction of fragment damage passed on to a real player sitting in a hit crew position. */
   private static final float PASSENGER_SHARE = 0.12F;
   private static final float PASSENGER_KO_DAMAGE = 8.0F;
   private static final double MAX_INTERIOR_RAY = 9.0;

   private TankDamage() {
   }

   public static void markHandled(Entity shell) {
      HANDLED.add(shell);
   }

   public static boolean isHandled(@Nullable Entity e) {
      return e != null && HANDLED.contains(e);
   }

   // ------------------------------------------------------------------------------------------
   // Hit report
   // ------------------------------------------------------------------------------------------

   /** One traced line for the shot replay: 0 = shell core, 1 = spall, 2 = burst fragment. */
   public record ShotRay(Vec3 from, Vec3 to, int kind, float t0) {
   }

   public static final class Report {
      private final List<MutableComponent> events = new ArrayList<>();
      /** Everything traced during this hit, for the shot replay (world coordinates). */
      public final List<ShotRay> rays = new ArrayList<>();
      /** Module index -> distance along the shot (blocks) at which it was first damaged. */
      public final java.util.Map<Integer, Float> hitTime = new java.util.HashMap<>();
      /** Where an explosive shell burst inside, or null. */
      @Nullable
      public Vec3 burstPoint;
      /** Module index -> whether it was out before this hit (to show what this shot broke). */
      public final java.util.Set<Integer> outBefore = new java.util.HashSet<>();
      private final List<String> keys = new ArrayList<>();
      boolean knockedOut;
      boolean detonated;
      boolean fireStarted;

      void add(String key, MutableComponent c) {
         if (!this.keys.contains(key)) {
            this.keys.add(key);
            this.events.add(c);
         }
      }

      public boolean knockedOut() {
         return this.knockedOut;
      }

      /** The individual event lines (for the replay panel). */
      public List<MutableComponent> eventLines() {
         return this.events;
      }

      public boolean detonated() {
         return this.detonated;
      }

      public boolean isEmpty() {
         return this.events.isEmpty() && !this.knockedOut;
      }

      private static int rank(String key) {
         if (key.equals("knocked_out")) {
            return 0;
         } else if (key.equals("detonation")) {
            return 1;
         } else if (key.equals("fire")) {
            return 2;
         } else if (key.startsWith("ko:")) {
            return 3;
         } else {
            return key.startsWith("dmg:") || key.startsWith("hurt:") ? 4 : 5;
         }
      }

      /** "KNOCKED OUT · Driver ✖ · Engine ✖ · Fire!" style summary, most important first. */
      public MutableComponent summary() {
         List<Integer> order = new ArrayList<>();

         for (int i = 0; i < this.keys.size(); i++) {
            String k = this.keys.get(i);
            String name = k.contains(":") ? k.substring(k.indexOf(':') + 1) : null;
            if (name != null && !k.startsWith("ko:") && this.keys.contains("ko:" + name)) {
               continue;
            }

            order.add(i);
         }

         order.sort(Comparator.comparingInt(i -> rank(this.keys.get(i))));
         MutableComponent out = Component.empty();
         int shown = Math.min(order.size(), 7);

         for (int n = 0; n < shown; n++) {
            if (n > 0) {
               out.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
            }

            out.append(this.events.get(order.get(n)));
         }

         if (order.size() > shown) {
            out.append(Component.literal(" +" + (order.size() - shown)).withStyle(ChatFormatting.DARK_GRAY));
         }

         return out;
      }
   }

   // ------------------------------------------------------------------------------------------
   // Simulation context
   // ------------------------------------------------------------------------------------------

   private static final class Sim {
      final WarSteelTank tank;
      final TankState st;
      final DamageModel model;
      final ServerLevel level;
      final RandomSource rnd;
      final DamageSource source;
      @Nullable
      final Entity attacker;
      final Report report = new Report();
      /** Damage taken by each ammo rack during this hit; the detonation roll happens once, at the end. */
      final java.util.Map<Integer, Float> ammoHits = new java.util.HashMap<>();
      /** Modules that already rolled for fire during this hit. */
      final java.util.Set<Integer> fireRolled = new java.util.HashSet<>();
      /** Current distance along the shot, in blocks; stamps module hit times for the replay. */
      float clock;
      int shellCalibre;
      final Obb[] modules;
      final Obb[] plates;
      /** The vehicle's hull and turret hit boxes: nothing inside travels beyond them. */
      final List<Obb> envelope = new ArrayList<>();
      final int entryPlate;

      Sim(WarSteelTank tank, ServerLevel level, @Nullable ArmorData armour, int entryPlate, DamageSource source, @Nullable Entity attacker) {
         this.tank = tank;
         this.st = tank.damageState();
         this.model = this.st.model();
         this.level = level;
         this.rnd = level.random;
         this.source = source;
         this.attacker = attacker;
         this.entryPlate = entryPlate;
         List<ModuleDef> defs = this.model.modules();
         this.modules = new Obb[defs.size()];

         for (ModuleDef d : defs) {
            this.modules[d.index()] = Obb.of(tank, d.transform(), d.position(), d.size());
            if (this.st.destroyed(d.index())) {
               this.report.outBefore.add(d.index());
            }
         }

         for (com.atsuishio.superbwarfare.tools.OBB o : tank.getOBBs()) {
            String part = o.part == null ? "BODY" : o.part.name();
            if (!part.startsWith("WHEEL") && !part.equals("COLLISION")) {
               this.envelope.add(new Obb(new Vec3(o.center.x, o.center.y, o.center.z), o.rotation(), o.extents().x, o.extents().y, o.extents().z));
            }
         }

         if (armour != null) {
            List<ArmorPlate> ps = armour.getPlates();
            this.plates = new Obb[ps.size()];

            for (int i = 0; i < ps.size(); i++) {
               ArmorPlate p = ps.get(i);
               this.plates[i] = Obb.of(tank, p.getTransform(), p.getPosition(), p.getSize());
            }
         } else {
            this.plates = new Obb[0];
         }
      }
   }

   private record RayHit(int module, double t) {
   }

   // ------------------------------------------------------------------------------------------
   // Entry points used by the armour hit handler
   // ------------------------------------------------------------------------------------------

   /**
    * External modules (tracks, barrel) the shell passes through just before reaching the hull,
    * nearest first. They behave like spaced armour.
    */
   public static List<ModuleDef> externalModulesOnPath(WarSteelTank tank, Vec3 hit, Vec3 dir) {
      TankState st = tank.damageState();
      DamageModel model = st.model();
      List<ModuleDef> out = new ArrayList<>();
      if (model == null) {
         return out;
      }

      // Only modules the shell actually struck: SBW reports the impact on its hit box, so an
      // external module counts when that point lies on (or within a few cm of) it.
      Vec3 start = hit.subtract(dir.scale(1.5));
      List<RayHit> hits = new ArrayList<>();

      for (ModuleDef d : model.modules()) {
         if (d.external()) {
            Obb box = Obb.of(tank, d.transform(), d.position(), d.size());
            if (box.distanceSqr(hit) < 0.0025) {
               double[] t = box.intersect(start, dir);
               hits.add(new RayHit(d.index(), t != null ? t[0] : 1.5));
            }
         }
      }

      hits.sort(Comparator.comparingDouble(RayHit::t));

      for (RayHit h : hits) {
         out.add(model.modules().get(h.module()));
      }

      return out;
   }

   /** Debug: damage one module by name, with the normal consequences (fire, detonation, knock-out). */
   public static Report damageModule(WarSteelTank tank, ServerLevel level, String module, float amount, @Nullable Entity attacker) {
      Sim s = new Sim(tank, level, null, -1, tank.damageSources().generic(), attacker);
      int i = s.model.indexOf(module);
      if (i < 0) {
         throw new IllegalArgumentException("no module named " + module);
      }

      damage(s, i, amount);
      finish(s);
      return s.report;
   }

   /** Direct hit on an external module by a kinetic round (before or instead of the hull). */
   public static Report hitExternal(WarSteelTank tank, ServerLevel level, ModuleDef module, ShellData shell, DamageSource source, @Nullable Entity attacker) {
      Sim s = new Sim(tank, level, null, -1, source, attacker);
      damage(s, module.index(), coreDamage(shell) * 0.8F);
      finish(s);
      return s.report;
   }

   /** An explosive round bursting outside the armour: only external modules nearby suffer. */
   public static Report outsideBlast(WarSteelTank tank, ServerLevel level, Vec3 at, ShellData shell, DamageSource source, @Nullable Entity attacker) {
      return blastFromOutside(tank, level, at, shell.getFiller(), source, attacker);
   }

   /** A charge of {@code fillerGrams} TNT going off outside this tank's armour. */
   public static Report blastFromOutside(WarSteelTank tank, ServerLevel level, Vec3 at, double fillerGrams, DamageSource source, @Nullable Entity attacker) {
      Sim s = new Sim(tank, level, null, -1, source, attacker);
      // Outside the armour the blast vents into open air: much smaller lethal radius.
      double filler = Math.max(fillerGrams, 20.0);
      double r = Math.max(0.5, 0.15 * Math.cbrt(filler));
      float peak = blastPeak(filler) * 0.5F;

      double kg = filler / 1000.0;
      double cbrt = Math.cbrt(kg);
      double fragRange = 30.0 * cbrt + 6.0;

      for (ModuleDef d : s.model.modules()) {
         if (!d.external()) {
            continue;
         }

         double dist = Math.sqrt(s.modules[d.index()].distanceSqr(at));
         if (d.type() == ModuleType.CREW) {
            // Crew working in the open (gun crews): same blast and fragment model as infantry,
            // scaled from a player's 20 health to the module's 100.
            double rr = Math.max(0.3, dist);
            double z = rr / cbrt;
            double dmg = z < 7.5 ? 55.0 * Math.pow(1.0 - z / 7.5, 1.6) : 0.0;
            if (rr < fragRange) {
               double expected = 900.0 * Math.sqrt(kg) * 0.8 / (4.0 * Math.PI * rr * rr) * 0.6;
               double mean = expected;
               int hits = 0;
               double l = Math.exp(-Math.min(mean, 30.0));
               double p = 1.0;

               do {
                  hits++;
                  p *= s.rnd.nextDouble();
               } while (p > l && hits < 200);

               dmg += (hits - 1) * (9.0 * (1.0 - rr / fragRange) + 2.0);
            }

            damage(s, d.index(), (float)(dmg * 5.0));
         } else if (dist < r) {
            damage(s, d.index(), (float)(peak * (1.0 - dist / r)));
         }
      }

      finish(s);
      return s.report;
   }

   /**
    * The shell has defeated plate {@code entryPlate}. {@code excess} is penetration left over
    * (mm) and {@code effective} the line-of-sight thickness it went through.
    */
   public static Report penetrate(
      WarSteelTank tank,
      ServerLevel level,
      ArmorData armour,
      int entryPlate,
      ShellData shell,
      Vec3 hit,
      Vec3 dir,
      double effective,
      double excess,
      DamageSource source,
      @Nullable Entity attacker
   ) {
      Sim s = new Sim(tank, level, armour, entryPlate, source, attacker);
      s.shellCalibre = shell.getCalibre();
      Vec3 entry = hit.add(dir.scale(0.08));
      double wall = wallDistance(s, entry, dir);
      String cls = shell.getShellClass();
      int calibre = Math.max(shell.getCalibre(), 7);
      double ratio = Math.min(1.0, excess / Math.max(effective, 5.0));

      switch (cls) {
         case "HE":
         case "FLAK": {
            // Overmatched: bursts just behind the plate.
            double d0 = Math.min(0.25, wall * 0.5);
            s.report.rays.add(new ShotRay(entry, entry.add(dir.scale(d0)), 0, 0.0F));
            burst(s, entry.add(dir.scale(d0)), shell.getFiller(), (float)d0);
            break;
         }
         case "HEAT": {
            // The shaped-charge jet: a thin, extremely hot stream that wrecks whatever it touches
            // along a short line behind the plate, plus a narrow cone of molten armour. It is
            // spent within a metre or two unless it has a lot of penetration to spare.
            double jet = Math.min(wall, 1.0 + excess / 80.0);
            core(s, entry, dir, jet, 260.0F);
            int n = clamp((int)(6 + excess / 25.0), 6, 14);
            spall(s, entry, dir, n, 14.0, 30.0F, 0.0F);
            if (jet >= wall - 0.05) {
               exitHole(s, armour, entry, dir, wall, excess);
            }

            s.report.add("jet", Component.literal("HEAT jet " + String.format(java.util.Locale.ROOT, "%.1f", jet) + " m").withStyle(ChatFormatting.GRAY));
            break;
         }
         case "HESH": {
            int n = clamp((int)(shell.getFiller() / 40.0), 12, 60);
            spall(s, entry, dir, n, 60.0, 34.0F, 0.0F);
            break;
         }
         case "APHE":
         case "APCBC":
         case "APHEBC": {
            int spallCount = clamp((int)(calibre / 9.0 * (0.5 + ratio)), 3, 14);
            spall(s, entry, dir, spallCount, 22.0, 20.0F + calibre * 0.15F, 0.0F);
            boolean armed = effective >= shell.getFuzeSensitivity() && shell.getFiller() > 0.0;
            if (armed) {
               double travel = Math.min(shell.getFuzeDelay(), wall - 0.15);
               core(s, entry, dir, Math.max(0.0, travel), coreDamage(shell));
               burst(s, entry.add(dir.scale(Math.max(0.05, travel))), shell.getFiller(), (float)Math.max(0.05, travel));
            } else {
               // Fuze did not arm (plate too thin): the shell over-penetrates as solid shot.
               core(s, entry, dir, wall, coreDamage(shell));
               s.report.add("unfuzed", Component.literal("fuze not armed").withStyle(ChatFormatting.GRAY));
               exitHole(s, armour, entry, dir, wall, excess);
            }
            break;
         }
         case "APCR":
         case "APDS": {
            core(s, entry, dir, wall, coreDamage(shell) * 0.8F);
            exitHole(s, armour, entry, dir, wall, excess);
            int n = clamp((int)(calibre / 12.0 * (0.4 + ratio)), 3, 10);
            spall(s, entry, dir, n, 13.0, 16.0F + calibre * 0.12F, 0.0F);
            break;
         }
         default: {
            // AP solid shot
            core(s, entry, dir, wall, coreDamage(shell));
            exitHole(s, armour, entry, dir, wall, excess);
            int n = clamp((int)(calibre / 7.0 * (0.5 + ratio)), 4, 20);
            spall(s, entry, dir, n, 24.0, 22.0F + calibre * 0.18F, 0.0F);
         }
      }

      finish(s);
      return s.report;
   }

   // ------------------------------------------------------------------------------------------
   // Shell behaviours
   // ------------------------------------------------------------------------------------------

   private static float coreDamage(ShellData shell) {
      return 140.0F + shell.getCalibre() * 1.6F;
   }

   private static double blastRadius(double fillerGrams) {
      return Math.max(0.6, 0.33 * Math.cbrt(fillerGrams));
   }

   private static float blastPeak(double fillerGrams) {
      return (float)(60.0 + 40.0 * Math.cbrt(fillerGrams));
   }

   /** The shell body itself: everything it passes through takes heavy damage. */
   private static void core(Sim s, Vec3 from, Vec3 dir, double length, float damage) {
      float d = damage;
      s.report.rays.add(new ShotRay(from, from.add(dir.scale(length)), 0, 0.0F));

      for (RayHit h : trace(s, from, dir, length)) {
         s.clock = (float)h.t();
         damage(s, h.module(), d);
         d *= 0.8F;
      }
   }

   /**
    * A solid shot that crossed the fighting compartment meets the far armour. If what is left of
    * its penetration beats that plate's effective thickness it goes out the other side, leaving
    * an exit hole there.
    */
   private static void exitHole(Sim s, @Nullable ArmorData armour, Vec3 from, Vec3 dir, double wall, double excess) {
      if (armour == null || s.plates.length == 0) {
         return;
      }

      int far = -1;
      double[] span = null;

      for (int i = 0; i < s.plates.length; i++) {
         if (i == s.entryPlate) {
            continue;
         }

         double[] t = s.plates[i].intersect(from, dir);
         if (t != null && t[0] > 0.12 && (span == null || t[0] < span[0])) {
            far = i;
            span = t;
         }
      }

      // Left through a gap in the armour model, or never reached a plate.
      if (far < 0 || span[0] > wall + 0.3) {
         return;
      }

      ArmorPlate plate = armour.getPlates().get(far);
      org.joml.Vector3d ln = s.tank.getRotationFromString(plate.getTransform()).transform(new org.joml.Vector3d(plate.getLocalNormal()));
      Vec3 normal = new Vec3(ln.x, ln.y, ln.z);
      normal = normal.lengthSqr() < 1.0E-9 ? dir : normal.normalize();
      if (normal.dot(dir) < 0.0) {
         normal = normal.scale(-1.0);
      }

      double cos = Math.max(0.087, Math.min(1.0, normal.dot(dir)));
      double farEffective = plate.getThickness() / cos;
      // The shot has lost some energy crossing the compartment and whatever it hit inside.
      double left = excess * 0.85;
      if (left < farEffective) {
         return;
      }

      Vec3 out = from.add(dir.scale(Math.max(span[0], span[1])));
      s.report.rays.add(new ShotRay(from.add(dir.scale(span[0])), out.add(dir.scale(1.5)), 0, (float)span[0]));
      s.report.add("exit", Component.literal("went through (" + plate.getName() + ")").withStyle(ChatFormatting.GRAY));
      HitDecals.add(s.tank, plate.getTransform(), out, normal, dir, HitDecals.EXIT, Math.max(7, s.shellCalibre));
   }

   /** A cone of armour fragments thrown off the inner face of the plate. */
   private static void spall(Sim s, Vec3 from, Vec3 dir, int count, double halfAngle, float damage, float t0) {
      for (int i = 0; i < count; i++) {
         Vec3 d = coneDir(dir, halfAngle, s.rnd);
         fragment(s, from, d, damage * (0.75F + s.rnd.nextFloat() * 0.5F), t0, 1);
      }
   }

   /** An explosive burst inside the hull: overpressure plus a sphere of fragments. */
   private static void burst(Sim s, Vec3 at, double fillerGrams, float t0) {
      s.report.burstPoint = at;
      s.clock = t0;
      double filler = Math.max(fillerGrams, 10.0);
      double r = blastRadius(filler);
      float peak = blastPeak(filler);

      for (ModuleDef d : s.model.modules()) {
         double dist = Math.sqrt(s.modules[d.index()].distanceSqr(at));
         if (dist < r) {
            damage(s, d.index(), (float)(peak * (1.0 - dist / r)));
         }
      }

      int n = clamp((int)(filler / 2.5), 12, 150);

      for (int i = 0; i < n; i++) {
         fragment(s, at, sphereDir(s.rnd), 26.0F + s.rnd.nextFloat() * 14.0F, t0, 2);
      }

      s.level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
   }

   /** One fragment: damages the first module it meets fully and a second one at half. */
   private static void fragment(Sim s, Vec3 from, Vec3 dir, float damage, float t0, int kind) {
      double wall = wallDistance(s, from, dir);
      double stop = wall;
      float d = damage;
      int hits = 0;

      for (RayHit h : trace(s, from, dir, wall)) {
         ModuleDef def = s.model.modules().get(h.module());
         if (def.external()) {
            continue;
         }

         s.clock = t0 + (float)h.t();
         damage(s, h.module(), d);
         hits++;
         stop = h.t() + 0.15;
         if (def.type() == ModuleType.CREW || hits >= 2) {
            break;
         }

         d *= 0.5F;
      }

      s.report.rays.add(new ShotRay(from, from.add(dir.scale(Math.min(stop, wall))), kind, t0));
   }

   // ------------------------------------------------------------------------------------------
   // Module damage rules
   // ------------------------------------------------------------------------------------------

   private static void damage(Sim s, int i, float amount) {
      if (amount <= 0.0F) {
         return;
      }

      TankState st = s.st;
      ModuleDef d = s.model.modules().get(i);
      float before = st.health(i);
      if (before <= 0.0F) {
         return;
      }

      s.report.hitTime.putIfAbsent(i, s.clock);
      float after = Math.max(0.0F, before - amount);
      st.setHealth(i, after);
      st.quietTicks = 0;
      boolean killed = after <= 0.0F;
      switch (d.type()) {
         case CREW:
            hurtPassenger(s, d, amount * PASSENGER_SHARE + (killed ? PASSENGER_KO_DAMAGE : 0.0F));
            if (killed) {
               s.report.add("ko:" + d.name(), Component.literal(d.label() + " ✖").withStyle(ChatFormatting.RED));
            } else {
               s.report.add("hurt:" + d.name(), Component.literal(d.label() + " wounded").withStyle(ChatFormatting.GOLD));
            }

            return;
         case AMMO_RACK:
            s.ammoHits.merge(i, amount, Float::sum);
            break;
         case ENGINE:
         case FUEL_TANK:
            if (s.fireRolled.add(i) && s.rnd.nextDouble() < d.fireChance() * (killed ? 2.0 : 1.0)) {
               startFire(s.tank, st, s.report, i);
            }
            break;
         default:
            break;
      }

      if (killed) {
         s.report.add("ko:" + d.name(), Component.literal(d.label() + " ✖").withStyle(ChatFormatting.RED));
      } else if (after / d.maxHealth() < 0.6F) {
         s.report.add("dmg:" + d.name(), Component.literal(d.label() + " damaged").withStyle(ChatFormatting.GOLD));
      }
   }

   private static void hurtPassenger(Sim s, ModuleDef d, float amount) {
      if (d.seat() < 0 || amount <= 0.0F) {
         return;
      }

      Entity e = s.tank.getNthEntity(d.seat());
      if (e instanceof LivingEntity living && e.isAlive()) {
         living.invulnerableTime = 0;
         living.hurt(s.source, amount);
      }
   }

   static void detonate(WarSteelTank tank, TankState st, Report report, int ammoModule) {
      if (st.detonated) {
         return;
      }

      st.detonated = true;
      st.setHealth(ammoModule, 0.0F);
      report.detonated = true;
      report.add("detonation", Component.literal("AMMO DETONATION").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
   }

   static void startFire(WarSteelTank tank, TankState st, Report report, int module) {
      if (st.fireTicks <= 0) {
         report.fireStarted = true;
         report.add("fire", Component.literal("Fire!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
      }

      st.fireTicks = Math.max(st.fireTicks, (int)(st.model().fireSeconds() * 20.0F));
      st.fireModule = module;
      st.dirty = true;
   }

   private static void finish(Sim s) {
      // One detonation roll per ammo rack per hit, scaled by how hard it was hit. A rack that is
      // destroyed outright usually goes up; otherwise the charges may just burn.
      for (java.util.Map.Entry<Integer, Float> e : s.ammoHits.entrySet()) {
         ModuleDef d = s.model.modules().get(e.getKey());
         boolean destroyed = s.st.destroyed(d.index());
         double severity = Math.min(1.0, e.getValue() / d.maxHealth());
         double chance = destroyed ? Math.min(0.9, d.detonationChance() * 1.6) : d.detonationChance() * severity * 0.6;
         if (s.rnd.nextDouble() < Math.min(0.95, chance)) {
            detonate(s.tank, s.st, s.report, d.index());
            break;
         } else if (destroyed || s.rnd.nextDouble() < 0.25 * severity) {
            startFire(s.tank, s.st, s.report, d.index());
         }
      }

      checkKnockout(s.tank, s.st, s.report, s.attacker);
   }

   /** Decides whether the tank is out of the fight and, if so, hands it to SBW's destruction. */
   static void checkKnockout(WarSteelTank tank, TankState st, Report report, @Nullable Entity attacker) {
      DamageModel model = st.model();
      if (model == null || st.knockedOut) {
         return;
      }

      boolean crewLost = st.crewTotal() > 0 && st.crewAlive() < Math.max(1, model.minCrew());
      if (!st.detonated && !crewLost) {
         return;
      }

      st.knockedOut = true;
      st.dirty = true;
      report.knockedOut = true;
      report.add("knocked_out", Component.literal("KNOCKED OUT").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
      if (attacker != null) {
         tank.setLastAttackerUUID(attacker.getStringUUID());
      }

      if (st.detonated && tank.level() instanceof ServerLevel level) {
         Vec3 at = ammoPosition(tank, model);
         level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.6F);
         tank.createCustomExplosion().position(at).radius(model.ammoExplosionRadius()).damage(60.0F).keepBlock().explode();
      }

      tank.setHealth(0.0F);
   }

   private static Vec3 ammoPosition(WarSteelTank tank, DamageModel model) {
      for (ModuleDef d : model.modules()) {
         if (d.type() == ModuleType.AMMO_RACK && tank.damageState().destroyed(d.index())) {
            return Obb.of(tank, d.transform(), d.position(), d.size()).centre;
         }
      }

      return tank.position().add(0.0, 1.2, 0.0);
   }

   // ------------------------------------------------------------------------------------------
   // Per-tick behaviour: fire, crew swaps, repairs, and pushing module state into SBW
   // ------------------------------------------------------------------------------------------

   public static void serverTick(WarSteelTank tank) {
      TankState st = tank.damageState();
      DamageModel model = st.model();
      if (model == null || !(tank.level() instanceof ServerLevel level)) {
         return;
      }

      if (!st.knockedOut && !tank.isWreck()) {
         st.quietTicks++;
         crewSwaps(tank, st, model);
         fire(tank, st, model, level);
         repairs(tank, st, model);
         applyEffects(tank, st);
      } else if (st.fireTicks > 0) {
         st.fireTicks = 0;
         st.dirty = true;
      }

      if (st.decalsDirty) {
         tank.getEntityData().set(WarSteelTank.DECALS, st.encodeDecals());
         st.decalsDirty = false;
      }

      if (st.dirty || tank.tickCount % 40 == 0) {
         tank.getEntityData().set(WarSteelTank.MODULE_STATUS, st.encode());
         st.dirty = false;
      }
   }

   /** Repair-tool work: patch one hit mark, and repair the worst-off component a little. */
   public static void repairWork(WarSteelTank tank, float amount) {
      TankState st = tank.damageState();
      st.removeOldestDecal();
      DamageModel model = st.model();
      if (model == null) {
         return;
      }

      ModuleDef worst = null;

      for (ModuleDef d : model.modules()) {
         if (d.type().repairable() && st.health(d.index()) < d.maxHealth()
            && (worst == null || st.health(d.index()) / d.maxHealth() < st.health(worst.index()) / worst.maxHealth())) {
            worst = d;
         }
      }

      if (worst != null) {
         st.setHealth(worst.index(), Math.min(worst.maxHealth(), st.health(worst.index()) + Math.max(6.0F, amount * 8.0F)));
      }
   }

   private static void applyEffects(WarSteelTank tank, TankState st) {
      boolean immobile = st.immobile(false);
      tank.setMainEngineDamaged(immobile);
      if (immobile) {
         tank.setPower(0.0F);
      }

      tank.setLeftWheelDamaged(st.trackOut("LEFT"));
      tank.setRightWheelDamaged(st.trackOut("RIGHT"));
      tank.setTurretDamaged(st.allOut(ModuleType.TURRET_DRIVE, null));
   }

   private static void crewSwaps(WarSteelTank tank, TankState st, DamageModel model) {
      int swapTicks = (int)(model.crewSwapSeconds() * 20.0F);

      for (ModuleDef d : model.modules()) {
         if (d.type() != ModuleType.CREW || d.role() == null || !d.role().essential || !st.destroyed(d.index())) {
            continue;
         }

         ModuleDef spare = null;

         for (CrewRole pref : new CrewRole[]{CrewRole.LOADER, CrewRole.RADIO, CrewRole.COMMANDER}) {
            for (ModuleDef c : model.modules()) {
               if (c.type() == ModuleType.CREW && c.role() == pref && !st.destroyed(c.index())) {
                  spare = c;
                  break;
               }
            }

            if (spare != null) {
               break;
            }
         }

         if (spare == null) {
            st.setSwapTicks(d.index(), 0);
            continue;
         }

         st.setSwapTicks(d.index(), st.swapTicks(d.index()) + 1);
         if (st.swapTicks(d.index()) >= swapTicks) {
            st.setSwapTicks(d.index(), 0);
            st.setHealth(d.index(), st.health(spare.index()));
            st.setHealth(spare.index(), 0.0F);
            tellCrew(tank, Component.literal(spare.label() + " took over as " + d.label()).withStyle(ChatFormatting.YELLOW));
         }
      }
   }

   private static void fire(WarSteelTank tank, TankState st, DamageModel model, ServerLevel level) {
      if (st.fireTicks <= 0) {
         return;
      }

      st.fireTicks--;
      Vec3 at = st.fireModule >= 0
         ? Obb.of(tank, model.modules().get(st.fireModule).transform(), model.modules().get(st.fireModule).position(), model.modules().get(st.fireModule).size()).centre
         : tank.position().add(0.0, 1.0, 0.0);
      if (tank.tickCount % 3 == 0) {
         level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.6, at.z, 6, 0.35, 0.25, 0.35, 0.02);
         level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1.0, at.z, 3, 0.3, 0.3, 0.3, 0.03);
         ParticleTool.sendParticle(level, ModParticleTypes.RISING_SMOKE.get(), at.x, at.y + 1.2, at.z, 2, 0.3, 0.2, 0.3, 0.03, false);
      }

      if (tank.tickCount % 20 == 0) {
         level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.8F);
         Report report = new Report();
         DamageSource burn = tank.damageSources().onFire();
         List<ModuleDef> crew = new ArrayList<>();

         for (ModuleDef d : model.modules()) {
            if (d.type() == ModuleType.CREW && !st.destroyed(d.index())) {
               crew.add(d);
            }
         }

         if (!crew.isEmpty()) {
            ModuleDef victim = crew.get(level.random.nextInt(crew.size()));
            float before = st.health(victim.index());
            st.setHealth(victim.index(), Math.max(0.0F, before - 7.0F));
            if (st.destroyed(victim.index())) {
               report.add("ko:" + victim.name(), Component.literal(victim.label() + " ✖").withStyle(ChatFormatting.RED));
               Entity p = victim.seat() >= 0 ? tank.getNthEntity(victim.seat()) : null;
               if (p instanceof LivingEntity living) {
                  living.hurt(burn, PASSENGER_KO_DAMAGE);
               }
            }
         }

         if (st.fireModule >= 0) {
            st.setHealth(st.fireModule, Math.max(0.0F, st.health(st.fireModule) - 8.0F));
         }

         for (ModuleDef d : model.modules()) {
            // Cook-off: only ammunition close enough to the fire to be heated by it.
            if (d.type() == ModuleType.AMMO_RACK
               && !st.destroyed(d.index())
               && Obb.of(tank, d.transform(), d.position(), d.size()).distanceSqr(at) < 6.25
               && level.random.nextDouble() < 0.015) {
               detonate(tank, st, report, d.index());
               break;
            }
         }

         if (st.fireTicks <= 0) {
            tellCrew(tank, Component.literal("Fire extinguished").withStyle(ChatFormatting.GREEN));
         }

         checkKnockout(tank, st, report, tank.getLastAttacker());
         if (!report.isEmpty()) {
            tellCrew(tank, report.summary());
         }
      }
   }

   private static void repairs(WarSteelTank tank, TankState st, DamageModel model) {
      if (st.fireTicks > 0) {
         return;
      }

      if (st.quietTicks >= (int)(model.repairSeconds() * 20.0F)) {
         boolean any = false;

         for (ModuleDef d : model.modules()) {
            if (d.type().repairable() && st.health(d.index()) < d.maxHealth()) {
               st.setHealth(d.index(), d.maxHealth());
               any = true;
            }
         }

         if (any) {
            tellCrew(tank, Component.literal("Field repairs complete").withStyle(ChatFormatting.GREEN));
         }
      }

      if (st.quietTicks >= (int)(model.crewReplaceSeconds() * 20.0F)) {
         boolean any = false;

         for (ModuleDef d : model.modules()) {
            if (d.type() == ModuleType.CREW && st.health(d.index()) < d.maxHealth()) {
               st.setHealth(d.index(), d.maxHealth());
               any = true;
            }
         }

         if (any) {
            tellCrew(tank, Component.literal("Crew replenished").withStyle(ChatFormatting.GREEN));
         }
      }
   }

   static void tellCrew(WarSteelTank tank, Component msg) {
      for (Entity e : tank.getPassengers()) {
         if (e instanceof ServerPlayer p) {
            p.displayClientMessage(msg, true);
         }
      }
   }

   // ------------------------------------------------------------------------------------------
   // Geometry helpers
   // ------------------------------------------------------------------------------------------

   /** Modules crossed by the segment from..from+dir*length, nearest first. */
   private static List<RayHit> trace(Sim s, Vec3 from, Vec3 dir, double length) {
      List<RayHit> out = new ArrayList<>();
      if (length <= 0.0) {
         return out;
      }

      for (int i = 0; i < s.modules.length; i++) {
         double[] t = s.modules[i].intersect(from, dir);
         if (t != null && t[0] <= length && t[1] >= 0.0) {
            out.add(new RayHit(i, Math.max(0.0, t[0])));
         }
      }

      out.sort(Comparator.comparingDouble(RayHit::t));
      return out;
   }

   /** Distance to the far armour wall (any plate other than the entry plate). */
   private static double wallDistance(Sim s, Vec3 from, Vec3 dir) {
      double best = MAX_INTERIOR_RAY;

      for (int i = 0; i < s.plates.length; i++) {
         if (i == s.entryPlate) {
            continue;
         }

         double[] t = s.plates[i].intersect(from, dir);
         if (t != null && t[0] > 0.12 && t[0] < best) {
            best = t[0];
         }
      }

      // Never beyond the hull/turret volume (gaps between plates, the turret ring...).
      if (!s.envelope.isEmpty()) {
         for (double d = 0.1; d < best; d += 0.1) {
            Vec3 p = from.add(dir.scale(d));
            boolean inside = false;

            for (Obb box : s.envelope) {
               if (box.distanceSqr(p) < 0.0025) {
                  inside = true;
                  break;
               }
            }

            if (!inside) {
               return d;
            }
         }
      }

      return best;
   }

   private static Vec3 coneDir(Vec3 axis, double halfAngleDeg, RandomSource rnd) {
      double cosMax = Math.cos(Math.toRadians(halfAngleDeg));
      double cosT = 1.0 - rnd.nextDouble() * (1.0 - cosMax);
      double sinT = Math.sqrt(Math.max(0.0, 1.0 - cosT * cosT));
      double phi = rnd.nextDouble() * Math.PI * 2.0;
      Vec3 a = axis.normalize();
      Vec3 up = Math.abs(a.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
      Vec3 u = a.cross(up).normalize();
      Vec3 v = a.cross(u).normalize();
      return a.scale(cosT).add(u.scale(sinT * Math.cos(phi))).add(v.scale(sinT * Math.sin(phi))).normalize();
   }

   private static Vec3 sphereDir(RandomSource rnd) {
      double z = rnd.nextDouble() * 2.0 - 1.0;
      double phi = rnd.nextDouble() * Math.PI * 2.0;
      double r = Math.sqrt(1.0 - z * z);
      return new Vec3(r * Math.cos(phi), z, r * Math.sin(phi));
   }

   private static int clamp(int v, int lo, int hi) {
      return Math.max(lo, Math.min(hi, v));
   }
}
