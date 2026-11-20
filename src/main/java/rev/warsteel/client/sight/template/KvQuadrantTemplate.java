package rev.rev.warsteel.client.sight.template;

import kotlin.jvm.internal.Intrinsics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import rev.rev.warsteel.client.sight.LadderSide;
import rev.rev.warsteel.client.sight.LadderSpec;
import rev.rev.warsteel.client.sight.SightRender;

@OnlyIn(Dist.CLIENT)
public final class KvQuadrantTemplate implements ReticleTemplate {
   @NotNull
   public static final KvQuadrantTemplate INSTANCE = new KvQuadrantTemplate();
   @NotNull
   private static final String id = "kv_quadrant";

   private KvQuadrantTemplate() {
   }

   @NotNull
   @Override
   public String getId() {
      return id;
   }

   @Override
   public void draw(@NotNull SightContext ctx) {
      float r = ctx.getRadius();
      SightRender.hLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx() - r * 0.97F, ctx.getCx() + r * 0.97F, ctx.getCy(), ctx.getColor(), 0, 32, null);
      SightRender.vLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx(), ctx.getCy() - r * 0.97F, ctx.getCy() + r * 0.97F, ctx.getColor(), 0, 32, null);
      this.drawDeflectionScale(ctx, r);
      LadderSpec var10000 = ctx.getSpec().ladderFor(LadderSide.LEFT);
      if (var10000 != null) {
         LadderSpec it = var10000;
         LadderDraw.vertical$default(LadderDraw.INSTANCE, ctx, it, ctx.getCx() - ctx.unit(0.16F), ctx.unit(0.06F), true, false, 32, null);
      }

      var10000 = ctx.getSpec().ladderFor(LadderSide.RIGHT);
      if (var10000 != null) {
         LadderSpec it = var10000;
         LadderDraw.vertical$default(LadderDraw.INSTANCE, ctx, it, ctx.getCx() + ctx.unit(0.16F), ctx.unit(0.06F), false, false, 32, null);
      }

      if (!SightRender.INSTANCE.exists(ctx.getSpec().getFurniture()) && ctx.getSpec().getDesignation().length() > 0) {
         SightRender.INSTANCE.textCentered(ctx.getGg(), ctx.getSpec().getDesignation(), ctx.getCx(), ctx.getCy() + r + ctx.unit(0.04F), ctx.getColor());
      }
   }

   private final void drawDeflectionScale(SightContext ctx, float r) {
      float band = ctx.getCy() - r * 0.44F;
      float span = r * 0.72F;
      int divisions = 8;
      SightRender.hLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx() - span, ctx.getCx() + span, band, ctx.getColor(), 0, 32, null);
      int i = -divisions;
      if (i <= divisions) {
         while (true) {
            float x = ctx.getCx() + span * i / divisions;
            boolean major = i % 2 == 0;
            float len = major ? ctx.unit(0.028F) : ctx.unit(0.016F);
            SightRender.vLine$default(SightRender.INSTANCE, ctx.getGg(), x, band - len, band, ctx.getColor(), 0, 32, null);
            if (major && i != 0) {
               SightRender.INSTANCE.textCentered(ctx.getGg(), String.valueOf(Math.abs(i) * 4), x, band - len - 9.0F, ctx.getColor());
            }

            if (i == divisions) {
               break;
            }

            i++;
         }
      }
   }
}
