package rev.rev.warsteel.client.sight.template;

import kotlin.jvm.internal.Intrinsics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import rev.rev.warsteel.client.sight.LadderSide;
import rev.rev.warsteel.client.sight.LadderSpec;
import rev.rev.warsteel.client.sight.SightRender;

@OnlyIn(Dist.CLIENT)
public final class T26TripleTemplate implements ReticleTemplate {
   @NotNull
   public static final T26TripleTemplate INSTANCE = new T26TripleTemplate();
   @NotNull
   private static final String id = "t26_triple";

   private T26TripleTemplate() {
   }

   @NotNull
   @Override
   public String getId() {
      return id;
   }

   @Override
   public void draw(@NotNull SightContext ctx) {
      float r = ctx.getRadius();
      SightRender.hLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx() - r * 0.9F, ctx.getCx() + r * 0.9F, ctx.getCy(), ctx.getColor(), 0, 32, null);
      float step = ctx.unit(0.19F);
      LadderSpec var10000 = ctx.getSpec().ladderFor(LadderSide.LEFT);
      if (var10000 != null) {
         LadderSpec it = var10000;
         LadderDraw.vertical$default(LadderDraw.INSTANCE, ctx, it, ctx.getCx() - step, ctx.unit(0.045F), true, false, 32, null);
      }

      var10000 = ctx.getSpec().ladderFor(LadderSide.CENTER);
      if (var10000 != null) {
         LadderSpec it = var10000;
         LadderDraw.vertical$default(LadderDraw.INSTANCE, ctx, it, ctx.getCx(), ctx.unit(0.045F), true, false, 32, null);
      }

      var10000 = ctx.getSpec().ladderFor(LadderSide.RIGHT);
      if (var10000 != null) {
         LadderSpec it = var10000;
         LadderDraw.vertical$default(LadderDraw.INSTANCE, ctx, it, ctx.getCx() + step, ctx.unit(0.045F), false, false, 32, null);
      }

      this.drawSubScale(ctx, r);
      if (ctx.getSpec().getDesignation().length() > 0) {
         SightRender.INSTANCE.textCentered(ctx.getGg(), ctx.getSpec().getDesignation(), ctx.getCx(), ctx.getCy() + r * 0.74F, ctx.getColor());
      }
   }

   private final void drawSubScale(SightContext ctx, float r) {
      float y = ctx.getCy() + r * 0.6F;
      float span = r * 0.68F;
      int divisions = 10;
      SightRender.hLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx() - span, ctx.getCx() + span, y, ctx.getColor(), 0, 32, null);
      int i = -divisions;
      if (i <= divisions) {
         while (true) {
            float x = ctx.getCx() + span * i / divisions;
            boolean major = i % 2 == 0;
            float len = major ? ctx.unit(0.026F) : ctx.unit(0.014F);
            SightRender.vLine$default(SightRender.INSTANCE, ctx.getGg(), x, y - len, y, ctx.getColor(), 0, 32, null);
            if (i == divisions) {
               break;
            }

            i++;
         }
      }

      SightRender.INSTANCE.vLine(ctx.getGg(), ctx.getCx(), y - ctx.unit(0.05F), y, ctx.getColor(), 2);
   }
}
