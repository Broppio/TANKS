package rev.rev.warsteel.client.sight.template;

import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.math.MathKt;
import kotlin.ranges.RangesKt;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import rev.rev.warsteel.client.sight.LadderSide;
import rev.rev.warsteel.client.sight.LadderSpec;
import rev.rev.warsteel.client.sight.SightRender;

@OnlyIn(Dist.CLIENT)
@SourceDebugExtension({"SMAP\nTigerArcTemplate.kt\nKotlin\n*S Kotlin\n*F\n+ 1 TigerArcTemplate.kt\nrev/rev/warsteel/client/sight/template/TigerArcTemplate\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n+ 3 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,159:1\n1#2:160\n1872#3,3:161\n*S KotlinDebug\n*F\n+ 1 TigerArcTemplate.kt\nrev/rev/warsteel/client/sight/template/TigerArcTemplate\n*L\n102#1:161,3\n*E\n"})
public final class TigerArcTemplate implements ReticleTemplate {
   @NotNull
   public static final TigerArcTemplate INSTANCE = new TigerArcTemplate();
   @NotNull
   private static final String id = "tiger_arc";

   private TigerArcTemplate() {
   }

   @NotNull
   @Override
   public String getId() {
      return id;
   }

   @Override
   public void draw(@NotNull SightContext ctx) {
      float r = ctx.getRadius();
      SightRender.INSTANCE.chevronDown(ctx.getGg(), ctx.getCx(), ctx.getCy() - ctx.unit(0.012F), ctx.unit(0.03F), ctx.unit(0.07F), ctx.getColor());
      SightRender.vDashed$default(
         SightRender.INSTANCE, ctx.getGg(), ctx.getCx(), ctx.getCy() + ctx.unit(0.05F), ctx.getCy() + r * 0.92F, ctx.getColor(), 3, 5, 0, 128, null
      );
      LadderSpec var10000 = ctx.liveLadder();
      if (var10000 != null) {
         LadderSpec it = var10000;
         LadderDraw.INSTANCE.vertical(ctx, it, ctx.getCx(), ctx.unit(0.05F), true, false);
      }

      float spacing = r * 0.12F;

      for (int i = 1; i < 5; i++) {
         float dx = spacing * i;
         float len = i % 2 == 0 ? ctx.unit(0.03F) : ctx.unit(0.02F);
         SightRender.vLine$default(
            SightRender.INSTANCE, ctx.getGg(), ctx.getCx() - dx, ctx.getCy() - len * 0.5F, ctx.getCy() + len * 0.5F, ctx.getColor(), 0, 32, null
         );
         SightRender.vLine$default(
            SightRender.INSTANCE, ctx.getGg(), ctx.getCx() + dx, ctx.getCy() - len * 0.5F, ctx.getCy() + len * 0.5F, ctx.getColor(), 0, 32, null
         );
      }

      var10000 = ctx.getSpec().ladderFor(LadderSide.ARC_TOP);
      if (var10000 != null) {
         LadderSpec it = var10000;
         INSTANCE.drawArc(ctx, it, true);
      }

      var10000 = ctx.getSpec().ladderFor(LadderSide.ARC_BOTTOM);
      if (var10000 != null) {
         LadderSpec it = var10000;
         INSTANCE.drawArc(ctx, it, false);
      }

      if (ctx.getSpec().getDesignation().length() > 0) {
         SightRender.INSTANCE.textCentered(ctx.getGg(), ctx.getSpec().getDesignation(), ctx.getCx(), ctx.getCy() + r * 0.86F, ctx.getColor());
      }
   }

   private final void drawArc(SightContext ctx, LadderSpec ladder, boolean top) {
      int col = ctx.colorFor(ladder);
      float arcR = ctx.getRadius() * ctx.getSpec().getArcRadius();
      float minRange = ((Number)CollectionsKt.first(ladder.getStops())).intValue();
      float maxRange = ((Number)CollectionsKt.last(ladder.getStops())).intValue();
      if (!(maxRange <= minRange)) {
         float start = ctx.getSpec().getArcStartDeg();
         float end = ctx.getSpec().getArcEndDeg();
         boolean inHundreds = ((Number)CollectionsKt.last(ladder.getStops())).intValue() >= 1000;
         Iterable $this$forEachIndexed$iv = ladder.getStops();
         int index$iv = 0;

         for (Object item$iv : $this$forEachIndexed$iv) {
            int py = index$iv++;
            if (py < 0) {
               CollectionsKt.throwIndexOverflow();
            }

            int range = ((Number)item$iv).intValue();
            double a = drawArc$angleFor(minRange, maxRange, start, end, top, range);
            float px = ctx.getCx() + (float)(Math.cos(a) * arcR);
            float pyx = ctx.getCy() + (float)(Math.sin(a) * arcR);
            boolean major = (py + 1) % ladder.getMajorEvery() == 0 || ladder.getMajorEvery() == 1;
            float len = major ? ctx.unit(0.035F) : ctx.unit(0.02F);
            float ox = (float)(Math.cos(a) * len);
            float oy = (float)(Math.sin(a) * len);
            INSTANCE.radialTick(ctx, px, pyx, ox, oy, col);
            if (major) {
               float lx = ctx.getCx() + (float)(Math.cos(a) * (arcR + len + ctx.unit(0.035F)));
               float ly = ctx.getCy() + (float)(Math.sin(a) * (arcR + len + ctx.unit(0.035F)));
               SightRender.INSTANCE.textCentered(ctx.getGg(), String.valueOf(inHundreds ? range / 100 : range), lx, ly - 3.5F, col);
            }
         }

         if (ladder.getLabel().length() > 0) {
            double mid = drawArc$angleFor(minRange, maxRange, start, end, top, (minRange + maxRange) * 0.5F);
            float lx = ctx.getCx() + (float)(Math.cos(mid) * (arcR - ctx.unit(0.075F)));
            float ly = ctx.getCy() + (float)(Math.sin(mid) * (arcR - ctx.unit(0.075F)));
            SightRender.INSTANCE.textCentered(ctx.getGg(), ladder.getLabel(), lx, ly - 3.5F, col);
         }

         if (ctx.getHasTarget() && ctx.isLive(ladder)) {
            float clamped = RangesKt.coerceIn((float)ctx.getRangeToTarget(), minRange, maxRange);
            double a = drawArc$angleFor(minRange, maxRange, start, end, top, clamped);
            float inner = arcR - ctx.unit(0.045F);
            float px = ctx.getCx() + (float)(Math.cos(a) * inner);
            float pyx = ctx.getCy() + (float)(Math.sin(a) * inner);
            SightRender.INSTANCE.hLine(ctx.getGg(), px - 2.0F, px + 3.0F, pyx, ctx.getColor(), 2);
            SightRender.INSTANCE.vLine(ctx.getGg(), px, pyx - 2.0F, pyx + 3.0F, ctx.getColor(), 2);
         }
      }
   }

   private final void radialTick(SightContext ctx, float x, float y, float ox, float oy, int color) {
      int steps = Math.max(2, MathKt.roundToInt(Math.max(Math.abs(ox), Math.abs(oy))));
      int i = 0;
      if (i <= steps) {
         while (true) {
            float t = (float)i / steps;
            int px = MathKt.roundToInt(x + ox * t);
            int py = MathKt.roundToInt(y + oy * t);
            ctx.getGg().fill(px, py, px + 1, py + 1, color);
            if (i == steps) {
               break;
            }

            i++;
         }
      }
   }

   private static final double drawArc$angleFor(float minRange, float maxRange, float start, float end, boolean $top, float range) {
      float t = RangesKt.coerceIn((range - minRange) / (maxRange - minRange), 0.0F, 1.0F);
      float deg = start + (end - start) * t;
      return Math.toRadians($top ? deg : 360.0 - deg);
   }
}
