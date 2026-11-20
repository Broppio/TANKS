package rev.rev.warsteel.client.sight;

import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import org.jetbrains.annotations.NotNull;

public enum FurnitureAnchor {
   APERTURE,
   BELOW_APERTURE,
   ABOVE_APERTURE;

   // $VF: synthetic field
   private static final EnumEntries $ENTRIES = EnumEntriesKt.enumEntries(values());

   @NotNull
   public static EnumEntries<FurnitureAnchor> getEntries() {
      return $ENTRIES;
   }

   // $VF: synthetic method
   private static final FurnitureAnchor[] $values() {
      return new FurnitureAnchor[]{APERTURE, BELOW_APERTURE, ABOVE_APERTURE};
   }
}
