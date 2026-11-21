package rev.rev.warsteel.item.gun;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import rev.rev.warsteel.combat.ShellData;
import rev.rev.warsteel.entity.projectile.PanzerfaustWarheadEntity;
import rev.rev.warsteel.init.ModEntities;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Panzerfaust 60: a single-shot, disposable recoilless launcher. It comes loaded, fires its
 * HEAT warhead (200 mm) with a backblast, and the empty tube is thrown away a moment later.
 * The leaf sight is marked for 30, 60 and 80 m; at 45 m/s the warhead drops about 9 m at 60 m.
 */
public class PanzerfaustItem extends GunGeoItem {
   private static final String LOADED = "WarSteelPzfLoaded";
   private static final String SPENT = "WarSteelPzfSpentTicks";
   /** Ticks between firing and dropping the empty tube. */
   private static final int DISCARD_AFTER = 30;

   public PanzerfaustItem() {
      super(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1));
   }

   @Override
   public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
      return rev.rev.warsteel.client.panzerfaust.PanzerfaustItemRenderer::new;
   }

   @Override
   public boolean canEditAttachments(GunData data) {
      return false;
   }

   @Override
   public void whenNoAmmo(GunData data) {
      data.isEmpty.set(true);
   }

   /** True while the warhead is still in the tube (for the renderer). */
   public static boolean loaded(ItemStack stack) {
      GunData data = GunData.from(stack);
      return !data.tag.getBoolean(LOADED) || data.ammo.get() > 0;
   }

   @Override
   public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
      super.inventoryTick(stack, level, entity, slot, selected);
      if (level.isClientSide) {
         return;
      }

      GunData data = GunData.from(stack);
      if (!data.tag.getBoolean(LOADED)) {
         // Issued ready to fire: one warhead, no reloading.
         data.ammo.set(1);
         data.isEmpty.set(false);
         data.tag.putBoolean(LOADED, true);
         data.save();
      } else if (data.ammo.get() <= 0) {
         int spent = data.tag.getInt(SPENT) + 1;
         data.tag.putInt(SPENT, spent);
         data.save();
         if (spent >= DISCARD_AFTER) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.CHAIN_FALL, SoundSource.PLAYERS, 0.8F, 0.7F);
            stack.shrink(1);
         }
      }
   }

   @Override
   public boolean shootBullet(@NotNull ShootParameters p) {
      ServerLevel level = p.level;
      Entity shooter = p.shooter;
      PanzerfaustWarheadEntity warhead = new PanzerfaustWarheadEntity(ModEntities.PANZERFAUST_WARHEAD.get(), level);
      ShellData profile = warhead.profile();
      Vec3 dir = p.shootDirection.normalize();
      warhead.setOwner(shooter);
      warhead.setType(CannonShellEntity.Type.HE);
      warhead.setDamage(profile != null ? (float)profile.getDamage() : 300.0F);
      warhead.setExplosionDamage(40.0F);
      warhead.setExplosionRadius(2.0F);
      warhead.setCustomGravity(PanzerfaustWarheadEntity.GRAVITY);
      warhead.setLife(200);
      warhead.setPos(p.shootPosition.add(dir.scale(0.6)));
      double v = profile != null ? profile.getVelocity() : 2.25;
      warhead.shoot(dir.x, dir.y, dir.z, (float)v, (float)Math.min(p.spread, 1.5));
      level.addFreshEntity(warhead);

      // Recoilless: the propellant gas goes out of the back of the tube.
      if (shooter != null) {
         Vec3 back = p.shootPosition.subtract(dir.scale(1.2));
         level.sendParticles(ParticleTypes.LARGE_SMOKE, back.x, back.y, back.z, 18, 0.35, 0.35, 0.35, 0.02);
         level.sendParticles(ParticleTypes.FLAME, back.x, back.y, back.z, 10, 0.15, 0.15, 0.15, 0.05);
         level.sendParticles(ParticleTypes.CLOUD, back.x - dir.x, back.y - dir.y, back.z - dir.z, 24, 0.6, 0.4, 0.6, 0.03);
         for (Entity e : level.getEntities(shooter, new net.minecraft.world.phys.AABB(back, back).inflate(3.0))) {
            // Anyone standing in the backblast cone gets burned.
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(back);
            if (to.length() < 3.0 && to.normalize().dot(dir.scale(-1)) > 0.6) {
               e.hurt(level.damageSources().inFire(), (float)(10.0 * (1.0 - to.length() / 3.0)));
               e.igniteForSeconds(2);
            }
         }
      }

      return true;
   }
}
