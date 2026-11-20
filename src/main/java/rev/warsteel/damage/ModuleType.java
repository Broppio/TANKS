package rev.rev.warsteel.damage;

/** Internal component kinds. Defaults are tuned so a single well-placed hit behaves like War Thunder. */
public enum ModuleType {
   CREW(100.0F, false, 0.0, 0.0, 0.0, "Crew"),
   ENGINE(120.0F, false, 0.0, 0.15, 0.0, "Engine"),
   TRANSMISSION(110.0F, false, 0.0, 0.0, 0.0, "Transmission"),
   FUEL_TANK(80.0F, false, 0.0, 0.3, 0.0, "Fuel tank"),
   AMMO_RACK(60.0F, false, 0.0, 0.0, 0.45, "Ammo rack"),
   BREECH(160.0F, false, 0.0, 0.0, 0.0, "Breech"),
   BARREL(90.0F, true, 30.0, 0.0, 0.0, "Barrel"),
   TURRET_DRIVE(90.0F, false, 0.0, 0.0, 0.0, "Turret drive"),
   TRACK(140.0F, true, 20.0, 0.0, 0.0, "Track");

   public final float defaultHealth;
   public final boolean defaultExternal;
   public final double defaultArmour;
   public final double defaultFireChance;
   public final double defaultDetonationChance;
   public final String displayName;

   ModuleType(float health, boolean external, double armour, double fireChance, double detonationChance, String displayName) {
      this.defaultHealth = health;
      this.defaultExternal = external;
      this.defaultArmour = armour;
      this.defaultFireChance = fireChance;
      this.defaultDetonationChance = detonationChance;
      this.displayName = displayName;
   }

   public boolean repairable() {
      return this != CREW;
   }
}
