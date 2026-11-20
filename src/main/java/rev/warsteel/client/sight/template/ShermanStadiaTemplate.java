package rev.rev.warsteel.client.sight.template;

import kotlin.jvm.internal.Intrinsics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import rev.rev.warsteel.client.sight.LadderSide;
import rev.rev.warsteel.client.sight.LadderSpec;
import rev.rev.warsteel.client.sight.SightRender;

@OnlyIn(Dist.CLIENT)
public final class ShermanStadiaTemplate implements ReticleTemplate {
   @NotNull
   public static final ShermanStadiaTemplate INSTANCE = new ShermanStadiaTemplate();
   @NotNull
   private static final String id = "sherman_stadia";

   private ShermanStadiaTemplate() {
   }

   @NotNull
   @Override
   public String getId() {
      return id;
   }

   @Override
   public void draw(@NotNull SightContext ctx) {
      float r = ctx.getRadius();
      if (ctx.getSpec().getDesignation().length() > 0) {
         SightRender.INSTANCE.textCentered(ctx.getGg(), ctx.getSpec().getDesignation(), ctx.getCx(), ctx.getCy() - r * 0.8F, ctx.getColor());
      }

      float gap = ctx.unit(0.018F);
      SightRender.hLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx() - r * 0.58F, ctx.getCx() - gap, ctx.getCy(), ctx.getColor(), 0, 32, null);
      SightRender.hLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx() + gap, ctx.getCx() + r * 0.58F, ctx.getCy(), ctx.getColor(), 0, 32, null);
      SightRender.vLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx(), ctx.getCy() - ctx.unit(0.05F), ctx.getCy() - gap, ctx.getColor(), 0, 32, null);
      SightRender.vDashed$default(
         SightRender.INSTANCE, ctx.getGg(), ctx.getCx(), ctx.getCy() + gap, ctx.getCy() + r * 0.92F, ctx.getColor(), 3, 5, 0, 128, null
      );
      this.drawLeadMarks(ctx, r);
      LadderSpec var10000 = ctx.getSpec().ladderFor(LadderSide.LEFT);
      if (var10000 != null) {
         LadderSpec it = var10000;
         LadderDraw.vertical$default(LadderDraw.INSTANCE, ctx, it, ctx.getCx() - ctx.unit(0.1F), ctx.unit(0.055F), true, false, 32, null);
      }

      var10000 = ctx.getSpec().ladderFor(LadderSide.RIGHT);
      if (var10000 != null) {
         LadderSpec it = var10000;
         LadderDraw.vertical$default(LadderDraw.INSTANCE, ctx, it, ctx.getCx() + ctx.unit(0.1F), ctx.unit(0.055F), false, false, 32, null);
      }
   }

   private final void drawLeadMarks(SightContext ctx, float r) {
      float spacing = r * 0.115F;

      for (int i = 1; i < 5; i++) {
         float dx = spacing * i;
         float len = i % 2 == 0 ? ctx.unit(0.028F) : ctx.unit(0.018F);
         SightRender.vLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx() - dx, ctx.getCy() - len, ctx.getCy(), ctx.getColor(), 0, 32, null);
         SightRender.vLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx() + dx, ctx.getCy() - len, ctx.getCy(), ctx.getColor(), 0, 32, null);
      }
   }
}
