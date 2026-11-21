package rev.rev.warsteel.client.sight;

import com.atsuishio.superbwarfare.client.overlay.CommonOverlay;
import com.atsuishio.superbwarfare.client.overlay.OverlayTraceHandler;
import com.atsuishio.superbwarfare.client.overlay.RenderContext;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.tools.MinecraftUtil;
import com.atsuishio.superbwarfare.tools.VectorToolKt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import kotlin.NoWhenBranchMatchedException;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.RangesKt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import rev.rev.warsteel.client.sight.template.SightContext;
import rev.rev.warsteel.client.sight.template.Templates;

@OnlyIn(Dist.CLIENT)
@SourceDebugExtension({"SMAP\nWarSteelSightOverlay.kt\nKotlin\n*S Kotlin\n*F\n+ 1 WarSteelSightOverlay.kt\nrev/rev/warsteel/client/sight/WarSteelSightOverlay\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,248:1\n1#2:249\n*E\n"})
public final class WarSteelSightOverlay extends CommonOverlay {
   @NotNull
   public static final WarSteelSightOverlay INSTANCE = new WarSteelSightOverlay();
   private static float openAmount;

   private WarSteelSightOverlay() {
      super("sight");
   }

   public boolean shouldRender() {
      if (!super.shouldRender()) {
         return false;
      } else {
         LocalPlayer var10000 = MinecraftUtil.getLocalPlayer();
         if (var10000 == null) {
            return false;
         } else {
            LocalPlayer player = var10000;
            Entity vehicle = player.getVehicle();
            if (!(vehicle instanceof VehicleEntity)) {
               openAmount = 0.0F;
               return false;
            } else {
               return SightRegistry.INSTANCE.forEntity(vehicle) != null;
            }
         }
      }
   }

   public void render(@NotNull RenderContext $this$render) {
      if (rev.rev.warsteel.client.artillery.ShellCamera.active()) {
         // The camera is following a shell, not looking through the sight.
         return;
      }

      Entity seat = $this$render.getPlayer().getVehicle();
      VehicleEntity var10000 = seat instanceof VehicleEntity ? (VehicleEntity)seat : null;
      if ((seat instanceof VehicleEntity ? (VehicleEntity)seat : null) != null) {
         VehicleEntity vehicle = var10000;
         SightSpec var32 = SightRegistry.INSTANCE.forEntity((Entity)vehicle);
         if (var32 != null) {
            SightSpec spec = var32;
            int seatx = vehicle.getSeatIndex((Entity)$this$render.getPlayer());
            boolean gunner = seatx == 0 && vehicle.hasWeapon(seatx);
            boolean secondary = gunner && !Intrinsics.areEqual(vehicle.getGunName(seatx), spec.getWeapon());
            boolean zoomed = ClientEventHandler.zoomVehicle;
            if (!zoomed) {
               openAmount = 0.0F;
            } else {
               openAmount = Mth.lerp(RangesKt.coerceIn(0.4F * $this$render.getPartialTick(), 0.0F, 1.0F), openAmount, 1.0F);
               GunData gunData = vehicle.getGunData(seatx);
               // The loaded round's own ballistics (an HE shell is slower than AP).
               double velocity = vehicle.getProjectileVelocity((Entity)$this$render.getPlayer());
               double gravity = vehicle.getProjectileGravity((Entity)$this$render.getPlayer());
               if (velocity <= 0.0 && gunData != null) {
                  velocity = ((Number)gunData.get(GunProp.VELOCITY)).doubleValue();
                  gravity = ((Number)gunData.get(GunProp.GRAVITY)).doubleValue();
               }
               double zoomFactor = RangesKt.coerceAtLeast(vehicle.getDefaultZoom((Entity)$this$render.getPlayer()), 1.0);
               Double lookedAt = ClientEventHandler.fov;
               double it = lookedAt.doubleValue();
               double fovVertical = ((it > 1.0 ? lookedAt : null) != null ? it > 1.0 ? lookedAt : null : 70.0) / zoomFactor;
               Vec3 shootPos = vehicle.getShootPosForHud((Entity)$this$render.getPlayer(), $this$render.getPartialTick());
               Entity lookedAtx = OverlayTraceHandler.cameraMaxRangeEntity;
               Vec3 hitPos = OverlayTraceHandler.blockMaxRangeResult != null ? OverlayTraceHandler.blockMaxRangeResult.getLocation() : null;
               double range = 0.0;
               boolean hasTarget = false;
               if (lookedAtx != null) {
                  range = shootPos.distanceTo(lookedAtx.position());
                  hasTarget = true;
               } else if (hitPos != null) {
                  range = shootPos.distanceTo(hitPos);
                  hasTarget = range < 500.0;
               } else {
                  range = 0.0;
                  hasTarget = false;
               }

               float cx = $this$render.getScreenWidth() * 0.5F;
               float cy = $this$render.getScreenHeight() * 0.5F;
               // Centre the reticle on where the gun really points, not on the middle of the screen:
               // the sight sits beside the bore and the turret lags the view while it traverses,
               // so a centred reticle doesn't show where the shell will go.
               Vec3 bore = vehicle.getShootVec((Entity)$this$render.getPlayer(), $this$render.getPartialTick());
               if (bore != null && bore.lengthSqr() > 1.0E-6) {
                  Vec3 aim = shootPos.add(bore.normalize().scale(hasTarget ? Math.max(range, 5.0) : 400.0));
                  if (VectorToolKt.canSee(aim)) {
                     Vec3 p = VectorToolKt.worldToScreen(aim);
                     cx = Mth.clamp((float)p.x, 0.0F, $this$render.getScreenWidth());
                     cy = Mth.clamp((float)p.y, 0.0F, $this$render.getScreenHeight());
                  }
               }
               SightRender.INSTANCE.beginOverlay();
               this.renderAiming($this$render, spec, vehicle, gunner, secondary, cx, cy, fovVertical, gravity, velocity, range, hasTarget);
               if (spec.getTargetMarker() && hasTarget && lookedAtx != null) {
                  Vec3 worldPos = lookedAtx.position().add(0.0, lookedAtx.getBbHeight() * 0.5, 0.0);
                  Intrinsics.checkNotNull(worldPos);
                  if (VectorToolKt.canSee(worldPos)) {
                     Vec3 p = VectorToolKt.worldToScreen(worldPos);
                     this.drawTargetMarker($this$render, (float)p.x, (float)p.y, range);
                  }
               }

               SightRender.INSTANCE.endOverlay();
            }
         }
      }
   }

   private final void renderAiming(
      RenderContext ctx,
      SightSpec spec,
      VehicleEntity vehicle,
      boolean gunner,
      boolean secondary,
      float cx,
      float cy,
      double fovVertical,
      double gravity,
      double velocity,
      double range,
      boolean hasTarget
   ) {
      float aperture = Math.min(ctx.getScreenWidth(), ctx.getScreenHeight()) * spec.getApertureScale();
      float radius = aperture * 0.5F;
      SightRender.INSTANCE.drawCircleMask(ctx.getGuiGraphics(), ctx.getScreenWidth(), ctx.getScreenHeight(), cx, cy, radius, openAmount);
      SightRender var10000 = SightRender.INSTANCE;
      GuiGraphics var10001 = ctx.getGuiGraphics();
      ResourceLocation var10002;
      if (gunner) {
         var10002 = spec.getMask();
      } else {
         var10002 = spec.getCommanderMask();
         if (var10002 == null) {
            var10002 = spec.getMask();
         }
      }

      var10000.drawMaskTexture(var10001, var10002, ctx.getScreenWidth(), ctx.getScreenHeight(), openAmount);
      PoseStack poseStack = ctx.getGuiGraphics().pose();
      poseStack.pushPose();
      this.applyRecoil(ctx, vehicle);
      SightContext sightCtx = new SightContext(
         ctx.getGuiGraphics(), spec, ctx.getScreenWidth(), ctx.getScreenHeight(), fovVertical, gravity, velocity, range, hasTarget
      );
      this.drawFurniture(sightCtx, gunner && !secondary);
      String template = !gunner ? spec.getCommanderTemplate() : (secondary ? spec.getMgTemplate() : spec.getTemplate());
      Templates.INSTANCE.get(template).draw(sightCtx);
      poseStack.popPose();
   }

   private final void applyRecoil(RenderContext ctx, VehicleEntity vehicle) {
      PoseStack poseStack = ctx.getGuiGraphics().pose();
      float recoil = Mth.lerp(ctx.getPartialTick(), (float)vehicle.getRecoilShakeO(), (float)vehicle.getRecoilShake());
      float pitch = Mth.lerp(ctx.getPartialTick(), vehicle.getFakePitchO(), vehicle.getFakePitch());
      poseStack.translate(ctx.getScreenWidth() * 0.02F * recoil, recoil * 2.5F + ctx.getScreenHeight() * 0.02F * recoil - pitch, 0.0F);
      poseStack.rotateAround(Axis.ZP.rotationDegrees(-0.3F * ClientEventHandler.cameraRoll), ctx.getScreenWidth() / 2.0F, ctx.getScreenHeight() / 2.0F, 0.0F);
   }

   private final void drawFurniture(SightContext ctx, boolean gunner) {
      if (gunner) {
         ResourceLocation var10000 = ctx.getSpec().getFurniture();
         if (var10000 != null) {
            ResourceLocation texture = var10000;
            switch (ctx.getSpec().getFurnitureAnchor()) {
               case APERTURE:
                  SightRender.INSTANCE.drawFurniture(ctx.getGg(), texture, ctx.getCx(), ctx.getCy(), ctx.getAperture(), ctx.getAperture(), ctx.getColor());
                  break;
               case BELOW_APERTURE:
                  SightRender.INSTANCE
                     .drawFurniture(
                        ctx.getGg(),
                        texture,
                        ctx.getCx(),
                        ctx.getCy() + ctx.getRadius() + ctx.unit(0.09F),
                        ctx.getAperture() * 0.6F,
                        ctx.getAperture() * 0.15F,
                        ctx.getColor()
                     );
                  break;
               case ABOVE_APERTURE:
                  SightRender.INSTANCE
                     .drawFurniture(
                        ctx.getGg(),
                        texture,
                        ctx.getCx(),
                        ctx.getCy() - ctx.getRadius() - ctx.unit(0.09F),
                        ctx.getAperture() * 0.6F,
                        ctx.getAperture() * 0.15F,
                        ctx.getColor()
                     );
                  break;
               default:
                  throw new NoWhenBranchMatchedException();
            }
         }
      }
   }

   private final void drawTargetMarker(RenderContext ctx, float x, float y, double range) {
      int red = -50384;
      SightRender.INSTANCE.diamond(ctx.getGuiGraphics(), x, y, 6.0F, red);
      SightRender.INSTANCE.textCentered(ctx.getGuiGraphics(), (int)range + " m", x, y + 10.0F, red);
   }

   // $VF: synthetic class
   public class WhenMappings {
      // $VF: synthetic field
      public static final int[] $EnumSwitchMapping$0;

      static {
         int[] var0 = new int[FurnitureAnchor.values().length];

         try {
            var0[FurnitureAnchor.APERTURE.ordinal()] = 1;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[FurnitureAnchor.BELOW_APERTURE.ordinal()] = 2;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[FurnitureAnchor.ABOVE_APERTURE.ordinal()] = 3;
         } catch (NoSuchFieldError var2) {
         }

         $EnumSwitchMapping$0 = var0;
      }
   }
}
