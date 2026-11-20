package rev.rev.warsteel.client.sight.template;

import java.util.Iterator;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rev.rev.warsteel.client.sight.LadderSpec;
import rev.rev.warsteel.client.sight.SightBallistics;
import rev.rev.warsteel.client.sight.SightRender;
import rev.rev.warsteel.client.sight.SightSpec;

@OnlyIn(Dist.CLIENT)
@SourceDebugExtension({"SMAP\nReticleTemplate.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ReticleTemplate.kt\nrev/rev/warsteel/client/sight/template/SightContext\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,146:1\n295#2,2:147\n*S KotlinDebug\n*F\n+ 1 ReticleTemplate.kt\nrev/rev/warsteel/client/sight/template/SightContext\n*L\n65#1:147,2\n*E\n"})
public final class SightContext {
   @NotNull
   private final GuiGraphics gg;
   @NotNull
   private final SightSpec spec;
   private final int screenW;
   private final int screenH;
   private final double fovVerticalDeg;
   private final double gravity;
   private final double activeVelocity;
   private final double rangeToTarget;
   private final boolean hasTarget;
   private final float cx;
   private final float cy;
   private final float aperture;
   private final float radius;
   private final int color;
   private final int faint;

   public SightContext(
      @NotNull GuiGraphics gg,
      @NotNull SightSpec spec,
      int screenW,
      int screenH,
      double fovVerticalDeg,
      double gravity,
      double activeVelocity,
      double rangeToTarget,
      boolean hasTarget
   ) {
      super();
      this.gg = gg;
      this.spec = spec;
      this.screenW = screenW;
      this.screenH = screenH;
      this.fovVerticalDeg = fovVerticalDeg;
      this.gravity = gravity;
      this.activeVelocity = activeVelocity;
      this.rangeToTarget = rangeToTarget;
      this.hasTarget = hasTarget;
      this.cx = this.screenW * 0.5F;
      this.cy = this.screenH * 0.5F;
      this.aperture = Math.min(this.screenW, this.screenH) * this.spec.getApertureScale();
      this.radius = this.aperture * 0.5F;
      this.color = SightRender.INSTANCE.opaque(this.spec.getReticleColor());
      this.faint = SightRender.INSTANCE.withAlpha(this.spec.getReticleColor(), 0.42F);
   }

   @NotNull
   public final GuiGraphics getGg() {
      return this.gg;
   }

   @NotNull
   public final SightSpec getSpec() {
      return this.spec;
   }

   public final int getScreenW() {
      return this.screenW;
   }

   public final int getScreenH() {
      return this.screenH;
   }

   public final double getFovVerticalDeg() {
      return this.fovVerticalDeg;
   }

   public final double getGravity() {
      return this.gravity;
   }

   public final double getActiveVelocity() {
      return this.activeVelocity;
   }

   public final double getRangeToTarget() {
      return this.rangeToTarget;
   }

   public final boolean getHasTarget() {
      return this.hasTarget;
   }

   public final float getCx() {
      return this.cx;
   }

   public final float getCy() {
      return this.cy;
   }

   public final float getAperture() {
      return this.aperture;
   }

   public final float getRadius() {
      return this.radius;
   }

   public final int getColor() {
      return this.color;
   }

   public final int getFaint() {
      return this.faint;
   }

   public final float unit(float fraction) {
      return this.aperture * fraction;
   }

   @Nullable
   public final Float offsetFor(int range, double velocity) {
      return SightBallistics.INSTANCE.markOffset(range, velocity, this.gravity, this.fovVerticalDeg, this.screenH);
   }

   public final boolean isLive(@NotNull LadderSpec ladder) {
      return SightBallistics.INSTANCE.sameShell(ladder.getVelocity(), this.activeVelocity);
   }

   public final int colorFor(@NotNull LadderSpec ladder) {
      return this.isLive(ladder) ? this.color : this.faint;
   }

   @Nullable
   public final LadderSpec liveLadder() {
      for (LadderSpec ladder : this.spec.getLadders()) {
         if (this.isLive(ladder)) {
            return ladder;
         }
      }

      return this.spec.getLadders().isEmpty() ? null : this.spec.getLadders().get(0);
   }
}
