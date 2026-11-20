package rev.rev.warsteel.client.hud;

import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import rev.rev.warsteel.damage.TankState;
import rev.rev.warsteel.damage.WarSteelTank;

/** Bottom-left damage panel for the crew: one row per module, coloured by condition. */
@OnlyIn(Dist.CLIENT)
public final class DamageModelHud implements LayeredDraw.Layer {
   public static final DamageModelHud INSTANCE = new DamageModelHud();
   public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("warsteel", "damage_model");

   private DamageModelHud() {
   }

   @Override
   public void render(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player == null || mc.options.hideGui || !(player.getVehicle() instanceof WarSteelTank tank) || !tank.usesModuleDamage()) {
         return;
      }

      TankState st = tank.damageState();
      List<TankState.Entry> entries = st.clientEntries();
      if (entries.isEmpty()) {
         return;
      }

      Font font = mc.font;
      String flags = st.clientFlags();
      int rowH = 9;
      int width = 112;
      int crew = 0;
      int crewUp = 0;

      for (TankState.Entry e : entries) {
         if (e.kind().equals("CREW")) {
            crew++;
            if (e.percent() > 0) {
               crewUp++;
            }
         }
      }

      int statusLines = (flags.contains("F") ? 1 : 0) + (flags.contains("M") ? 1 : 0) + (flags.contains("G") ? 1 : 0) + (flags.contains("T") ? 1 : 0)
         + (flags.contains("K") ? 1 : 0);
      int height = 14 + entries.size() * rowH + statusLines * rowH + 4;
      // Drawn at 75% scale in the clear band on the left, below SBW's compass and above its
      // weapon/energy widgets.
      float scale = 0.75F;
      g.pose().pushPose();
      g.pose().scale(scale, scale, 1.0F);
      int x = 6;
      int y = (int)(g.guiHeight() * 0.2F / scale);
      g.fill(x - 3, y - 3, x + width, y + height - 3, 0x88000000);
      g.drawString(font, "CREW " + crewUp + "/" + crew, x, y, crewUp * 2 > crew ? 0xFFFFFFFF : 0xFFFF5555, false);
      y += 12;

      for (TankState.Entry e : entries) {
         int colour = e.percent() <= 0 ? 0xFF202020 : (e.percent() < 60 ? 0xFFFFAA00 : 0xFF55DD55);
         g.fill(x, y + 1, x + 6, y + 7, colour);
         int text = e.percent() <= 0 ? 0xFF777777 : 0xFFDDDDDD;
         String label = e.label() + (e.percent() <= 0 ? " ✖" : "");
         g.drawString(font, label, x + 9, y, text, false);
         y += rowH;
      }

      boolean blink = (player.tickCount / 5) % 2 == 0;
      if (flags.contains("K")) {
         g.drawString(font, "KNOCKED OUT", x, y, 0xFFFF2222, false);
         y += rowH;
      }

      if (flags.contains("F")) {
         g.drawString(font, "FIRE!", x, y, blink ? 0xFFFF8800 : 0xFFFFDD00, false);
         y += rowH;
      }

      if (flags.contains("M")) {
         g.drawString(font, "IMMOBILISED", x, y, 0xFFFF5555, false);
         y += rowH;
      }

      if (flags.contains("G")) {
         g.drawString(font, "MAIN GUN OUT", x, y, 0xFFFF5555, false);
         y += rowH;
      }

      if (flags.contains("T")) {
         g.drawString(font, "TURRET LOCKED", x, y, 0xFFFF5555, false);
      }

      g.pose().popPose();
   }
}
