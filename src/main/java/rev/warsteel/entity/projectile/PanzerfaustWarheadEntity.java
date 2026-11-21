package rev.rev.warsteel.entity.projectile;

import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import rev.rev.warsteel.combat.ShellData;
import rev.rev.warsteel.combat.ShellRegistry;

/**
 * The Panzerfaust 60's fin-stabilised shaped-charge warhead. It flies as a Superb Warfare cannon
 * shell (so it goes through WarSteel's armour model like any other round) with the "pzf60_heat"
 * profile, and slow and heavy enough to drop like the real one: 45 m/s, about 9 m of drop at 60 m.
 */
public class PanzerfaustWarheadEntity extends CannonShellEntity {
   public static final ResourceLocation PROFILE = ResourceLocation.fromNamespaceAndPath("warsteel", "pzf60_heat");
   /** 9.81 m/s^2 in blocks per tick^2. */
   public static final float GRAVITY = 9.81F / 400.0F;

   public PanzerfaustWarheadEntity(EntityType<? extends CannonShellEntity> type, Level level) {
      super(type, level);
   }

   @Nullable
   public ShellData profile() {
      return ShellRegistry.INSTANCE.byId(PROFILE);
   }
}
