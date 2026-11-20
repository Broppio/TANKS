package rev.rev.warsteel.combat;

import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import org.jetbrains.annotations.NotNull;

public enum HitOutcome {
   PENETRATION,
   RICOCHET,
   NON_PENETRATION;

   // $VF: synthetic field
   private static final EnumEntries $ENTRIES = EnumEntriesKt.enumEntries(values());

   @NotNull
   public static EnumEntries<HitOutcome> getEntries() {
      return $ENTRIES;
   }

   // $VF: synthetic method
   private static final HitOutcome[] $values() {
      return new HitOutcome[]{PENETRATION, RICOCHET, NON_PENETRATION};
   }
}
