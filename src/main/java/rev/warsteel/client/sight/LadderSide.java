package rev.rev.warsteel.client.sight;

import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import org.jetbrains.annotations.NotNull;

public enum LadderSide {
   FAR_LEFT,
   LEFT,
   CENTER,
   RIGHT,
   FAR_RIGHT,
   ARC_TOP,
   ARC_BOTTOM;

   // $VF: synthetic field
   private static final EnumEntries $ENTRIES = EnumEntriesKt.enumEntries(values());

   @NotNull
   public static EnumEntries<LadderSide> getEntries() {
      return $ENTRIES;
   }

   // $VF: synthetic method
   private static final LadderSide[] $values() {
      return new LadderSide[]{FAR_LEFT, LEFT, CENTER, RIGHT, FAR_RIGHT, ARC_TOP, ARC_BOTTOM};
   }
}
