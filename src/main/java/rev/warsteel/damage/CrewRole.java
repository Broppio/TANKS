package rev.rev.warsteel.damage;

public enum CrewRole {
   DRIVER("Driver", true),
   GUNNER("Gunner", true),
   COMMANDER("Commander", false),
   LOADER("Loader", false),
   RADIO("Radio operator", false);

   public final String displayName;
   /** Essential roles are refilled by a surviving non-essential crew member after a short delay. */
   public final boolean essential;

   CrewRole(String displayName, boolean essential) {
      this.displayName = displayName;
      this.essential = essential;
   }
}
