package rev.rev.warsteel.client.sight.template;

import kotlin.jvm.internal.Intrinsics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import rev.rev.warsteel.client.sight.SightRender;

@OnlyIn(Dist.CLIENT)
public final class MgCrossTemplate implements ReticleTemplate {
   @NotNull
   public static final MgCrossTemplate INSTANCE = new MgCrossTemplate();
   @NotNull
   private static final String id = "mg_cross";

   private MgCrossTemplate() {
   }

   @NotNull
   @Override
   public String getId() {
      return id;
   }

   @Override
   public void draw(@NotNull SightContext ctx) {
      float gap = ctx.unit(0.02F);
      float arm = ctx.unit(0.21F);
      float lead = ctx.unit(0.07F);
      float tick = ctx.unit(0.026F);
      SightRender.hLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx() - arm, ctx.getCx() - gap, ctx.getCy(), ctx.getColor(), 0, 32, null);
      SightRender.hLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx() + gap, ctx.getCx() + arm, ctx.getCy(), ctx.getColor(), 0, 32, null);
      SightRender.vLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx(), ctx.getCy() - arm, ctx.getCy() - gap, ctx.getColor(), 0, 32, null);
      SightRender.vLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx(), ctx.getCy() + gap, ctx.getCy() + arm, ctx.getColor(), 0, 32, null);
      SightRender.hLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx() - 1.0F, ctx.getCx() + 1.0F, ctx.getCy(), ctx.getColor(), 0, 32, null);
      SightRender.vLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx(), ctx.getCy() - 1.0F, ctx.getCy() + 1.0F, ctx.getColor(), 0, 32, null);

      for (int i = 1; i < 3; i++) {
         float dx = lead * i;
         float len = i == 2 ? tick * 1.5F : tick;
         SightRender.vLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx() - dx, ctx.getCy(), ctx.getCy() + len, ctx.getColor(), 0, 32, null);
         SightRender.vLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx() + dx, ctx.getCy(), ctx.getCy() + len, ctx.getColor(), 0, 32, null);
      }

      if (ctx.getSpec().getDesignation().length() > 0) {
         SightRender.INSTANCE.textCentered(ctx.getGg(), ctx.getSpec().getDesignation(), ctx.getCx(), ctx.getCy() + ctx.getRadius() * 0.52F, ctx.getFaint());
      }
   }
}
