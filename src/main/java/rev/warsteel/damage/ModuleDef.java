package rev.rev.warsteel.damage;

import org.jetbrains.annotations.Nullable;

/**
 * One internal (or external) component, as read from data/<ns>/warsteel/modules/<vehicle>.json.
 * Position is the box centre and Size the HALF-extents, both in blocks under Transform, exactly
 * like armour plates.
 */
public record ModuleDef(
   int index,
   String name,
   ModuleType type,
   @Nullable CrewRole role,
   @Nullable String side,
   String transform,
   double[] position,
   double[] size,
   float maxHealth,
   boolean external,
   double armour,
   double fireChance,
   double detonationChance,
   int seat
) {
   public String label() {
      if (this.type == ModuleType.CREW && this.role != null) {
         return this.role.displayName;
      } else if (this.type == ModuleType.TRACK && this.side != null) {
         return (this.side.equalsIgnoreCase("LEFT") ? "Left" : "Right") + " track";
      } else if ((this.type == ModuleType.AMMO_RACK || this.type == ModuleType.FUEL_TANK) && suffix(this.name) != null) {
         // Several racks/tanks per vehicle: "Ammo rack (floor front)", "Fuel tank (left)".
         return this.type.displayName + " (" + suffix(this.name) + ")";
      } else {
         return this.type.displayName;
      }
   }

   /** "AmmoFloorFront" -> "floor front", "FuelLeft" -> "left"; null when there is nothing after the prefix. */
   @Nullable
   private static String suffix(String name) {
      String rest = name.replaceFirst("^(Ammo|Fuel)", "");
      if (rest.isEmpty() || rest.equals(name)) {
         return null;
      }

      return rest.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(java.util.Locale.ROOT);
   }
}
