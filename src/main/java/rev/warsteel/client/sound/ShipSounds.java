package rev.rev.warsteel.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import rev.rev.warsteel.entity.vehicle.T22Entity;
import rev.rev.warsteel.init.ModSounds;

/**
 * The T-22's own running sounds, on top of Superb Warfare's turbine (engine) sound:
 * machinery (boilers, fans, pumps) whenever she has steam, and the bow wave by speed. Both follow
 * the ship and fade out when she stops, sinks or unloads.
 */
public final class ShipSounds {
   private ShipSounds() {
   }

   public static void start(T22Entity ship) {
      Minecraft mc = Minecraft.getInstance();
      mc.getSoundManager().play(new Loop(ModSounds.T22_MACHINERY.get(), ship, false));
      mc.getSoundManager().play(new Loop(ModSounds.T22_WASH.get(), ship, true));
   }

   private static final class Loop extends AbstractTickableSoundInstance {
      private final T22Entity ship;
      private final boolean wash;
      private float level;

      Loop(SoundEvent sound, T22Entity ship, boolean wash) {
         super(sound, SoundSource.NEUTRAL, ship.getRandom());
         this.ship = ship;
         this.wash = wash;
         this.looping = true;
         this.delay = 0;
         this.volume = 0.01F;
         this.place();
      }

      private void place() {
         // The wash is loudest at the bow, the machinery amidships.
         Vec3 at = this.wash ? this.ship.toWorld(0.0, 3.3, 30.0) : this.ship.toWorld(0.0, 4.0, -4.0);
         this.x = at.x;
         this.y = at.y;
         this.z = at.z;
      }

      @Override
      public void tick() {
         if (this.ship.isRemoved()) {
            this.stop();
            return;
         }

         float target;
         if (this.wash) {
            float speed = (float)this.ship.getDeltaMovement().horizontalDistance();
            float s = Mth.clamp(speed / 0.85F, 0.0F, 1.0F);
            target = this.ship.isWreck() ? 0.0F : s * 3.5F;
            this.pitch = 0.8F + 0.35F * s;
         } else {
            float p = Math.abs(this.ship.getPower());
            target = this.ship.isWreck() || this.ship.floodRatio() > 1.0F ? 0.0F : 1.4F + 0.8F * p;
            this.pitch = 0.9F + 0.15F * p;
         }

         // Ease so speed changes don't step the volume.
         this.level += (target - this.level) * 0.1F;
         this.volume = Math.max(0.001F, this.level);
         this.place();
      }
   }
}
