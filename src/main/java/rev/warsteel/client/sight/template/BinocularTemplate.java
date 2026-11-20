package rev.rev.warsteel.client.sight.template;

import kotlin.jvm.internal.Intrinsics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import rev.rev.warsteel.client.sight.SightRender;

@OnlyIn(Dist.CLIENT)
public final class BinocularTemplate implements ReticleTemplate {
   @NotNull
   public static final BinocularTemplate INSTANCE = new BinocularTemplate();
   @NotNull
   private static final String id = "binocular";

   private BinocularTemplate() {
   }

   @NotNull
   @Override
   public String getId() {
      return id;
   }

   @Override
   public void draw(@NotNull SightContext ctx) {
      float r = ctx.getRadius();
      float inner = ctx.unit(0.022F);
      float outer = ctx.unit(0.055F);
      SightRender.hLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx() - outer, ctx.getCx() - inner, ctx.getCy(), ctx.getFaint(), 0, 32, null);
      SightRender.hLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx() + inner, ctx.getCx() + outer, ctx.getCy(), ctx.getFaint(), 0, 32, null);
      SightRender.vLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx(), ctx.getCy() - outer, ctx.getCy() - inner, ctx.getFaint(), 0, 32, null);
      SightRender.vLine$default(SightRender.INSTANCE, ctx.getGg(), ctx.getCx(), ctx.getCy() + inner, ctx.getCy() + outer, ctx.getFaint(), 0, 32, null);
      if (ctx.getSpec().getDesignation().length() > 0) {
         SightRender.INSTANCE.textCentered(ctx.getGg(), ctx.getSpec().getDesignation(), ctx.getCx(), ctx.getCy() + r * 0.46F, ctx.getFaint());
      }
   }
}
