package rev.rev.warsteel.client.sight;

import com.atsuishio.superbwarfare.client.RenderHelper;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.math.MathKt;
import kotlin.ranges.RangesKt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
@SourceDebugExtension({"SMAP\nSightRender.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SightRender.kt\nrev/rev/warsteel/client/sight/SightRender\n+ 2 Maps.kt\nkotlin/collections/MapsKt__MapsKt\n*L\n1#1,323:1\n381#2,7:324\n*S KotlinDebug\n*F\n+ 1 SightRender.kt\nrev/rev/warsteel/client/sight/SightRender\n*L\n65#1:324,7\n*E\n"})
public final class SightRender {
   @NotNull
   public static final SightRender INSTANCE = new SightRender();
   public static final int OPAQUE = -16777216;
   @NotNull
   private static final HashMap<ResourceLocation, Boolean> textureExists = new HashMap<>();
   public static final float TEXT_HALF_HEIGHT = 3.5F;

   private SightRender() {
   }

   public final int opaque(int rgb) {
      return 0xFF000000 | rgb & 16777215;
   }

   public final int withAlpha(int rgb, float alpha) {
      return (int)(RangesKt.coerceIn(alpha, 0.0F, 1.0F) * 255.0F) << 24 | rgb & 16777215;
   }

   private final int black(float alpha) {
      return (int)(RangesKt.coerceIn(alpha, 0.0F, 1.0F) * 255.0F) << 24;
   }

   public final void clearTextureCache() {
      textureExists.clear();
   }

   public final boolean exists(@Nullable ResourceLocation loc) {
      if (loc == null) {
         return false;
      } else {
         Map $this$getOrPut$iv = textureExists;
         Object value$iv = $this$getOrPut$iv.get(loc);
         Object var10000;
         if (value$iv == null) {
            Object answer$iv = Minecraft.getInstance().getResourceManager().getResource(loc).isPresent();
            $this$getOrPut$iv.put(loc, answer$iv);
            var10000 = answer$iv;
         } else {
            var10000 = value$iv;
         }

         return (Boolean)var10000;
      }
   }

   public final void hLine(@NotNull GuiGraphics gg, float x1, float x2, float y, int color, int thickness) {
      int a = MathKt.roundToInt(x1);
      int b = MathKt.roundToInt(x2);
      int top = MathKt.roundToInt(y);
      if (b > a) {
         gg.fill(a, top, b, top + RangesKt.coerceAtLeast(thickness, 1), color);
      }
   }

   // $VF: synthetic method
   public static void hLine$default(SightRender var0, GuiGraphics var1, float var2, float var3, float var4, int var5, int var6, int var7, Object var8) {
      if ((var7 & 32) != 0) {
         var6 = 1;
      }

      var0.hLine(var1, var2, var3, var4, var5, var6);
   }

   public final void vLine(@NotNull GuiGraphics gg, float x, float y1, float y2, int color, int thickness) {
      int left = MathKt.roundToInt(x);
      int a = MathKt.roundToInt(y1);
      int b = MathKt.roundToInt(y2);
      if (b > a) {
         gg.fill(left, a, left + RangesKt.coerceAtLeast(thickness, 1), b, color);
      }
   }

   // $VF: synthetic method
   public static void vLine$default(SightRender var0, GuiGraphics var1, float var2, float var3, float var4, int var5, int var6, int var7, Object var8) {
      if ((var7 & 32) != 0) {
         var6 = 1;
      }

      var0.vLine(var1, var2, var3, var4, var5, var6);
   }

   public final void vDashed(@NotNull GuiGraphics gg, float x, float y1, float y2, int color, int dash, int gap, int thickness) {
      float y = y1;

      while (y < y2) {
         this.vLine(gg, x, y, Math.min(y + dash, y2), color, thickness);
         y += dash + gap;
      }
   }

   // $VF: synthetic method
   public static void vDashed$default(
      SightRender var0, GuiGraphics var1, float var2, float var3, float var4, int var5, int var6, int var7, int var8, int var9, Object var10
   ) {
      if ((var9 & 32) != 0) {
         var6 = 4;
      }

      if ((var9 & 64) != 0) {
         var7 = 4;
      }

      if ((var9 & 128) != 0) {
         var8 = 1;
      }

      var0.vDashed(var1, var2, var3, var4, var5, var6, var7, var8);
   }

   public final void hDashed(@NotNull GuiGraphics gg, float x1, float x2, float y, int color, int dash, int gap, int thickness) {
      float x = x1;

      while (x < x2) {
         this.hLine(gg, x, Math.min(x + dash, x2), y, color, thickness);
         x += dash + gap;
      }
   }

   // $VF: synthetic method
   public static void hDashed$default(
      SightRender var0, GuiGraphics var1, float var2, float var3, float var4, int var5, int var6, int var7, int var8, int var9, Object var10
   ) {
      if ((var9 & 32) != 0) {
         var6 = 4;
      }

      if ((var9 & 64) != 0) {
         var7 = 4;
      }

      if ((var9 & 128) != 0) {
         var8 = 1;
      }

      var0.hDashed(var1, var2, var3, var4, var5, var6, var7, var8);
   }

   public final void tick(@NotNull GuiGraphics gg, float cx, float y, float len, int color, int thickness) {
      this.hLine(gg, cx - len * 0.5F, cx + len * 0.5F, y, color, thickness);
   }

   // $VF: synthetic method
   public static void tick$default(SightRender var0, GuiGraphics var1, float var2, float var3, float var4, int var5, int var6, int var7, Object var8) {
      if ((var7 & 32) != 0) {
         var6 = 1;
      }

      var0.tick(var1, var2, var3, var4, var5, var6);
   }

   public final void chevronDown(@NotNull GuiGraphics gg, float cx, float apexY, float halfWidth, float height, int color) {
      if (!(height <= 0.0F)) {
         int top = MathKt.roundToInt(apexY - height);
         int bottom = MathKt.roundToInt(apexY);

         for (int y = top; y < bottom; y++) {
            float t = (bottom - y) / height;
            float half = halfWidth * t;
            gg.fill(MathKt.roundToInt(cx - half), y, MathKt.roundToInt(cx + half), y + 1, color);
         }
      }
   }

   public final void diamond(@NotNull GuiGraphics gg, float cx, float cy, float radius, int color) {
      int r = MathKt.roundToInt(radius);
      if (r > 0) {
         int i = 0;
         if (i <= r) {
            while (true) {
               int x = MathKt.roundToInt(cx);
               int y = MathKt.roundToInt(cy);
               gg.fill(x - i, y - (r - i), x - i + 1, y - (r - i) + 1, color);
               gg.fill(x + i, y - (r - i), x + i + 1, y - (r - i) + 1, color);
               gg.fill(x - i, y + (r - i), x - i + 1, y + (r - i) + 1, color);
               gg.fill(x + i, y + (r - i), x + i + 1, y + (r - i) + 1, color);
               if (i == r) {
                  break;
               }

               i++;
            }
         }
      }
   }

   public final void textCentered(@NotNull GuiGraphics gg, @NotNull String text, float cx, float y, int color) {
      if (text.length() != 0) {
         Font font = Minecraft.getInstance().font;
         gg.drawString(font, text, MathKt.roundToInt(cx - font.width(text) / 2.0F), MathKt.roundToInt(y), color, false);
      }
   }

   public final void textRight(@NotNull GuiGraphics gg, @NotNull String text, float right, float y, int color) {
      if (text.length() != 0) {
         Font font = Minecraft.getInstance().font;
         gg.drawString(font, text, MathKt.roundToInt(right - font.width(text)), MathKt.roundToInt(y), color, false);
      }
   }

   public final void textLeft(@NotNull GuiGraphics gg, @NotNull String text, float left, float y, int color) {
      if (text.length() != 0) {
         Font font = Minecraft.getInstance().font;
         gg.drawString(font, text, MathKt.roundToInt(left), MathKt.roundToInt(y), color, false);
      }
   }

   public final void drawCircleMask(@NotNull GuiGraphics gg, int screenW, int screenH, float cx, float cy, float radius, float alpha) {
      float a = RangesKt.coerceIn(alpha, 0.0F, 1.0F);
      if (!(a <= 0.004F)) {
         int col = this.black(a);
         int top = RangesKt.coerceIn(MathKt.roundToInt(cy - radius), 0, screenH);
         int bottom = RangesKt.coerceIn(MathKt.roundToInt(cy + radius), 0, screenH);
         if (top > 0) {
            gg.fill(0, 0, screenW, top, col);
         }

         if (bottom < screenH) {
            gg.fill(0, bottom, screenW, screenH, col);
         }

         for (int y = top; y < bottom; y++) {
            float dy = y + 0.5F - cy;
            float inside = radius * radius - dy * dy;
            if (inside <= 0.0F) {
               gg.fill(0, y, screenW, y + 1, col);
            } else {
               float half = (float)Math.sqrt(inside);
               int left = MathKt.roundToInt(cx - half);
               int right = MathKt.roundToInt(cx + half);
               if (left > 0) {
                  gg.fill(0, y, left, y + 1, col);
               }

               if (right < screenW) {
                  gg.fill(right, y, screenW, y + 1, col);
               }
            }
         }
      }
   }

   // $VF: synthetic method
   public static void drawCircleMask$default(
      SightRender var0, GuiGraphics var1, int var2, int var3, float var4, float var5, float var6, float var7, int var8, Object var9
   ) {
      if ((var8 & 64) != 0) {
         var7 = 1.0F;
      }

      var0.drawCircleMask(var1, var2, var3, var4, var5, var6, var7);
   }

   public final void drawVisionBlockMask(
      @NotNull GuiGraphics gg, int screenW, int screenH, float cx, float cy, float halfW, float halfH, float corner, float alpha
   ) {
      float a = RangesKt.coerceIn(alpha, 0.0F, 1.0F);
      if (!(a <= 0.004F)) {
         int col = this.black(a);
         float r = RangesKt.coerceIn(corner, 0.0F, Math.min(halfW, halfH));
         int top = RangesKt.coerceIn(MathKt.roundToInt(cy - halfH), 0, screenH);
         int bottom = RangesKt.coerceIn(MathKt.roundToInt(cy + halfH), 0, screenH);
         int left = RangesKt.coerceIn(MathKt.roundToInt(cx - halfW), 0, screenW);
         int right = RangesKt.coerceIn(MathKt.roundToInt(cx + halfW), 0, screenW);
         if (top > 0) {
            gg.fill(0, 0, screenW, top, col);
         }

         if (bottom < screenH) {
            gg.fill(0, bottom, screenW, screenH, col);
         }

         if (left > 0) {
            gg.fill(0, top, left, bottom, col);
         }

         if (right < screenW) {
            gg.fill(right, top, screenW, bottom, col);
         }

         if (!(r <= 0.0F)) {
            float straightTop = cy - halfH + r;
            float straightBottom = cy + halfH - r;

            for (int y = top; y < bottom; y++) {
               float yc = y + 0.5F;
               float var10000;
               if (yc < straightTop) {
                  var10000 = straightTop - yc;
               } else {
                  if (!(yc > straightBottom)) {
                     continue;
                  }

                  var10000 = yc - straightBottom;
               }

               float dy = var10000;
               float inside = r * r - dy * dy;
               float inset = inside <= 0.0F ? r : r - (float)Math.sqrt(inside);
               if (!(inset <= 0.0F)) {
                  int li = MathKt.roundToInt(cx - halfW + inset);
                  int ri = MathKt.roundToInt(cx + halfW - inset);
                  if (li > left) {
                     gg.fill(left, y, li, y + 1, col);
                  }

                  if (ri < right) {
                     gg.fill(ri, y, right, y + 1, col);
                  }
               }
            }
         }
      }
   }

   // $VF: synthetic method
   public static void drawVisionBlockMask$default(
      SightRender var0, GuiGraphics var1, int var2, int var3, float var4, float var5, float var6, float var7, float var8, float var9, int var10, Object var11
   ) {
      if ((var10 & 256) != 0) {
         var9 = 1.0F;
      }

      var0.drawVisionBlockMask(var1, var2, var3, var4, var5, var6, var7, var8, var9);
   }

   public final void drawMaskTexture(@NotNull GuiGraphics gg, @Nullable ResourceLocation texture, int screenW, int screenH, float alpha) {
      if (this.exists(texture)) {
         float side = Math.min(screenW, screenH);
         float x = (screenW - side) * 0.5F;
         float y = (screenH - side) * 0.5F;
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, RangesKt.coerceIn(alpha, 0.0F, 1.0F));
         RenderHelper.preciseBlit(gg, texture, x, y, 0.0F, 0.0F, side, side, side, side);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   // $VF: synthetic method
   public static void drawMaskTexture$default(SightRender var0, GuiGraphics var1, ResourceLocation var2, int var3, int var4, float var5, int var6, Object var7) {
      if ((var6 & 16) != 0) {
         var5 = 1.0F;
      }

      var0.drawMaskTexture(var1, var2, var3, var4, var5);
   }

   public final void drawFurniture(@NotNull GuiGraphics gg, @Nullable ResourceLocation texture, float cx, float cy, float width, float height, int color) {
      if (this.exists(texture)) {
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
         RenderSystem.setShader(SightRender::drawFurniture$lambda$1);
         RenderHelper.preciseBlitWithColor(gg, texture, cx - width * 0.5F, cy - height * 0.5F, 0.0F, 0.0F, width, height, width, height, color);
      }
   }

   public final void beginOverlay() {
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public final void endOverlay() {
      RenderSystem.defaultBlendFunc();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static final ShaderInstance drawFurniture$lambda$1() {
      return GameRenderer.getPositionTexColorShader();
   }
}
