package rev.rev.warsteel.combat;

import java.util.Locale;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * One shell profile from data/<ns>/warsteel/shells/*.json.
 *
 * Class decides what happens after the armour is defeated:
 *  AP    solid shot  - the core keeps flying through the hull and throws a spall cone.
 *  APCR  sub-calibre - like AP with a narrower, weaker spall cone.
 *  APHE  AP with an explosive filler - travels FuzeDelay blocks inside, then explodes
 *        (only if the plate it went through was at least FuzeSensitivity mm thick).
 *  HE    detonates on contact; if it defeats the armour (overmatch) it bursts inside,
 *        otherwise it only damages external modules (tracks, barrel).
 *  HESH  detonates on contact; on a penetration it throws a wide spall cone off the inner face.
 */
public final class ShellData {
   private final ResourceLocation id;
   private final String name;
   private final String shellClass;
   private final int calibre;
   private final double velocity;
   private final double damage;
   private final Penetration penetration;
   private final RicochetProfile ricochet;
   private final Set<String> usedBy;
   private final boolean defeatedByArmour;
   private final double filler;
   private final double fuzeDelay;
   private final double fuzeSensitivity;
   private final double mass;
   private int fuzeTicks;
   private double proximityRange;

   public ShellData(
      ResourceLocation id,
      String name,
      String shellClass,
      int calibre,
      double velocity,
      double damage,
      Penetration penetration,
      RicochetProfile ricochet,
      Set<String> usedBy,
      boolean defeatedByArmour,
      double filler,
      double fuzeDelay,
      double fuzeSensitivity,
      double mass
   ) {
      this.id = id;
      this.name = name;
      this.shellClass = shellClass.toUpperCase(Locale.ROOT);
      this.calibre = calibre;
      this.velocity = velocity;
      this.damage = damage;
      this.penetration = penetration;
      this.ricochet = ricochet;
      this.usedBy = usedBy;
      this.defeatedByArmour = defeatedByArmour;
      this.filler = filler;
      this.fuzeDelay = fuzeDelay;
      this.fuzeSensitivity = fuzeSensitivity;
      this.mass = mass;
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public String getShellClass() {
      return this.shellClass;
   }

   public int getCalibre() {
      return this.calibre;
   }

   public double getVelocity() {
      return this.velocity;
   }

   public double getDamage() {
      return this.damage;
   }

   public Penetration getPenetration() {
      return this.penetration;
   }

   public RicochetProfile getRicochet() {
      return this.ricochet;
   }

   public Set<String> getUsedBy() {
      return this.usedBy;
   }

   public boolean getDefeatedByArmour() {
      return this.defeatedByArmour;
   }

   /** Explosive filler in grams of TNT equivalent (0 for solid shot). */
   public double getFiller() {
      return this.filler;
   }

   /** Distance in blocks an APHE shell travels after penetrating before it bursts. */
   public double getFuzeDelay() {
      return this.fuzeDelay;
   }

   /** Minimum armour (mm) needed to arm an APHE fuze. */
   public double getFuzeSensitivity() {
      return this.fuzeSensitivity;
   }

   public double getMass() {
      return this.mass;
   }

   /** FLAK rounds: ticks until the time fuze bursts the shell. */
   public int getFuzeTicks() {
      return this.fuzeTicks;
   }

   /** FLAK rounds: distance from an aircraft at which the shell bursts. */
   public double getProximityRange() {
      return this.proximityRange;
   }

   ShellData withFlakFuze(int ticks, double range) {
      this.fuzeTicks = ticks;
      this.proximityRange = range;
      return this;
   }

   public boolean isFlak() {
      return this.shellClass.equals("FLAK");
   }

   /** Detonates on contact with anything (HE, HESH). */
   public boolean isContactFuzed() {
      return this.shellClass.equals("HE") || this.shellClass.equals("HESH") || this.shellClass.equals("FLAK");
   }

   /** Armour-piercing family (fired as SBW's AP shell type). */
   public boolean isArmourPiercing() {
      return !this.isContactFuzed();
   }

   /**
    * A profile for cannon shells that have no WarSteel data (Superb Warfare's own tanks and
    * other add-ons), built from the shell's own numbers so they still use the module model.
    */
   public static ShellData generic(boolean ap, float damage, float explosionDamage, float explosionRadius) {
      int calibre = (int)Math.max(20.0, Math.min(155.0, Math.sqrt(Math.max(damage, 1.0F)) * 4.0));
      if (ap) {
         double pen = Math.max(20.0, Math.min(250.0, damage * 0.22));
         return new ShellData(
            ResourceLocation.fromNamespaceAndPath("warsteel", "generic_ap"),
            "shell.warsteel.generic_ap",
            "AP",
            calibre,
            20.0,
            damage,
            new Penetration(pen, 0.02, pen * 0.6),
            new RicochetProfile(true, 62.0, 80.0, 2.0, 0.55, 6.0),
            Set.of(),
            true,
            0.0,
            0.0,
            0.0,
            0.0
         );
      } else {
         double filler = Math.max(50.0, explosionDamage * explosionRadius * 4.0);
         double pen = Math.max(5.0, Math.min(60.0, Math.cbrt(filler) * 2.2));
         return new ShellData(
            ResourceLocation.fromNamespaceAndPath("warsteel", "generic_he"),
            "shell.warsteel.generic_he",
            "HE",
            calibre,
            20.0,
            damage,
            new Penetration(pen, 0.0, pen),
            new RicochetProfile(true, 72.0, 85.0, 2.0, 0.55, 6.0),
            Set.of(),
            false,
            filler,
            0.0,
            0.0,
            0.0
         );
      }
   }
}
