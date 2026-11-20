package rev.rev.warsteel.client.sight.template;

import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import rev.rev.warsteel.client.sight.LadderSpec;
import rev.rev.warsteel.client.sight.SightRender;

@OnlyIn(Dist.CLIENT)
@SourceDebugExtension({"SMAP\nReticleTemplate.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ReticleTemplate.kt\nrev/rev/warsteel/client/sight/template/LadderDraw\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,146:1\n1872#2,3:147\n*S KotlinDebug\n*F\n+ 1 ReticleTemplate.kt\nrev/rev/warsteel/client/sight/template/LadderDraw\n*L\n114#1:147,3\n*E\n"})
public final class LadderDraw {
   @NotNull
   public static final LadderDraw INSTANCE = new LadderDraw();

   private LadderDraw() {
   }

   public final void vertical(@NotNull SightContext ctx, @NotNull LadderSpec ladder, float x, float tickLen, boolean labelToLeft, boolean header) {
      int col = ctx.colorFor(ladder);
      if (header && ladder.getLabel().length() > 0) {
         SightRender.INSTANCE.textCentered(ctx.getGg(), ladder.getLabel(), x, ctx.getCy() - ctx.getRadius() * 0.62F, col);
      }

      float lastY = 0.0F;
      lastY = ctx.getCy();
      Iterable $this$forEachIndexed$iv = ladder.getStops();
      int index$iv = 0;

      for (Object item$iv : $this$forEachIndexed$iv) {
         int var14 = index$iv++;
         if (var14 < 0) {
            CollectionsKt.throwIndexOverflow();
         }

         int range = ((Number)item$iv).intValue();
         Float var24 = ctx.offsetFor(range, ladder.getVelocity());
         if (var24 != null) {
            float drop = var24;
            float y = ctx.getCy() + drop;
            if (!(y > ctx.getCy() + ctx.getRadius() * 0.94F)) {
               boolean major = (var14 + 1) % ladder.getMajorEvery() == 0 || ladder.getMajorEvery() == 1;
               float len = major ? tickLen * 1.6F : tickLen;
               if (labelToLeft) {
                  SightRender.hLine$default(SightRender.INSTANCE, ctx.getGg(), x - len, x, y, col, 0, 32, null);
               } else {
                  SightRender.hLine$default(SightRender.INSTANCE, ctx.getGg(), x, x + len, y, col, 0, 32, null);
               }

               if (major) {
                  String label = String.valueOf(range);
                  if (labelToLeft) {
                     SightRender.INSTANCE.textRight(ctx.getGg(), label, x - len - 3.0F, y - 3.5F, col);
                  } else {
                     SightRender.INSTANCE.textLeft(ctx.getGg(), label, x + len + 3.0F, y - 3.5F, col);
                  }
               }

               lastY = y;
            }
         }
      }

      if (lastY > ctx.getCy()) {
         SightRender.vLine$default(SightRender.INSTANCE, ctx.getGg(), x, ctx.getCy(), lastY, col, 0, 32, null);
      }
   }

   // $VF: synthetic method
   public static void vertical$default(
      LadderDraw var0, SightContext var1, LadderSpec var2, float var3, float var4, boolean var5, boolean var6, int var7, Object var8
   ) {
      if ((var7 & 32) != 0) {
         var6 = true;
      }

      var0.vertical(var1, var2, var3, var4, var5, var6);
   }
}
