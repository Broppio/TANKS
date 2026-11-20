package rev.rev.warsteel.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import org.jetbrains.annotations.NotNull;

public class AfterburnerFlameParticle extends TextureSheetParticle {
   private final SpriteSet spriteSet;
   private final float initialScale;

   public AfterburnerFlameParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprite) {
      super(level, x, y, z, dx, dy, dz);
      this.spriteSet = sprite;
      this.lifetime = 15 + this.random.nextInt(8);
      this.gravity = -0.02F;
      this.xd = dx + (this.random.nextDouble() - 0.5) * 0.05;
      this.yd = dy + (this.random.nextDouble() - 0.5) * 0.05;
      this.zd = dz + (this.random.nextDouble() - 0.5) * 0.05;
      this.alpha = 0.95F;
      this.initialScale = 1.8F + this.random.nextFloat() * 0.4F;
      this.quadSize = this.initialScale;
      this.rCol = 1.0F;
      this.gCol = 0.7F + this.random.nextFloat() * 0.2F;
      this.bCol = 0.3F + this.random.nextFloat() * 0.2F;
      this.setSpriteFromAge(this.spriteSet);
   }

   public void tick() {
      super.tick();
      float ageRatio = (float)this.age / this.lifetime;
      if (ageRatio < 0.3F) {
         this.quadSize = this.initialScale * (1.0F + ageRatio * 0.5F);
      } else {
         this.quadSize = this.initialScale * (1.15F - (ageRatio - 0.3F) * 1.2F);
      }

      if (ageRatio < 0.7F) {
         this.alpha = 0.95F * (1.0F - ageRatio * 0.5F);
      } else {
         this.alpha = 0.95F * (1.0F - ageRatio) * 0.5F;
      }

      this.gCol = (0.7F + this.random.nextFloat() * 0.2F) * (1.0F - ageRatio * 0.5F);
      this.bCol = (0.3F + this.random.nextFloat() * 0.2F) * (1.0F - ageRatio * 0.7F);
      this.xd *= 0.95;
      this.yd *= 0.95;
      this.zd *= 0.95;
      this.setSpriteFromAge(this.spriteSet);
   }

   @NotNull
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public int getLightColor(float partialTick) {
      float ageRatio = (this.age + partialTick) / this.lifetime;
      int brightness = (int)(15.0F * (1.0F - ageRatio * 0.5F));
      brightness = Math.max(brightness, 0);
      return brightness << 20 | brightness << 4;
   }
}
