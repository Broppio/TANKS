package rev.rev.warsteel.combat;

import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import rev.rev.warsteel.entity.vehicle.Flak88Entity;
import rev.rev.warsteel.network.ArtilleryPayloads;

/**
 * Server side of the Flak 88's ARC mode: shells leave with the selected propellant charge, and
 * the gunner's client is told about each shell (for the shell camera) and where it came down.
 */
public final class ArtilleryMode {
   public static final ArtilleryMode INSTANCE = new ArtilleryMode();
   private final Map<CannonShellEntity, Tracked> live = new WeakHashMap<>();

   private record Tracked(ServerPlayer gunner, long firedAt) {
   }

   private ArtilleryMode() {
   }

   public static void handleControl(ArtilleryPayloads.ArcControl msg, IPayloadContext ctx) {
      if (!(ctx.player() instanceof ServerPlayer player) || !(player.getVehicle() instanceof Flak88Entity gun)) {
         return;
      }

      switch (msg.action()) {
         case ArtilleryPayloads.ArcControl.TOGGLE -> gun.setArcMode(!gun.arcMode());
         case ArtilleryPayloads.ArcControl.CHARGE_UP -> gun.setCharge(gun.charge() + 1);
         case ArtilleryPayloads.ArcControl.CHARGE_DOWN -> gun.setCharge(gun.charge() - 1);
         default -> {
            return;
         }
      }

      player.displayClientMessage(
         gun.arcMode()
            ? Component.literal("ARC mode · " + Flak88Entity.chargeName(gun.charge())).withStyle(ChatFormatting.GOLD)
            : Component.literal("Direct fire").withStyle(ChatFormatting.GRAY),
         true
      );
   }

   @SubscribeEvent
   public void onJoin(EntityJoinLevelEvent event) {
      if (event.getLevel().isClientSide() || !(event.getEntity() instanceof CannonShellEntity shell)) {
         return;
      }

      Entity owner = shell.getOwner();
      if (owner instanceof ServerPlayer gunner && gunner.getVehicle() instanceof Flak88Entity gun && gun.arcMode()
         && gun.getSeatIndex(gunner) == 0) {
         // Reduced propellant charge: same shell, lower muzzle velocity.
         shell.setDeltaMovement(shell.getDeltaMovement().scale(Flak88Entity.chargeFactor(gun.charge())));
         this.live.put(shell, new Tracked(gunner, event.getLevel().getGameTime()));
         PacketDistributor.sendToPlayer(
            gunner, new ArtilleryPayloads.ShellLaunched(shell.getId(), shell.position(), shell.getDeltaMovement(), shell.getCustomGravity())
         );
      }
   }

   @SubscribeEvent
   public void onTick(ServerTickEvent.Post event) {
      if (this.live.isEmpty()) {
         return;
      }

      Iterator<Map.Entry<CannonShellEntity, Tracked>> it = this.live.entrySet().iterator();

      while (it.hasNext()) {
         Map.Entry<CannonShellEntity, Tracked> e = it.next();
         CannonShellEntity shell = e.getKey();
         Tracked t = e.getValue();
         if (shell == null) {
            it.remove();
         } else if (shell.isRemoved()) {
            it.remove();
            if (!t.gunner().hasDisconnected()) {
               PacketDistributor.sendToPlayer(t.gunner(), new ArtilleryPayloads.ShellLanded(shell.getId(), shell.position()));
            }
         } else if (shell.level().getGameTime() - t.firedAt() > 2400L) {
            it.remove();
         }
      }
   }
}
