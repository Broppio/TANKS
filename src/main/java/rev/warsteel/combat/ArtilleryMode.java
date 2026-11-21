package rev.rev.warsteel.combat;

import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
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
   private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("warsteel/artillery");
   private final Map<CannonShellEntity, Tracked> live = new WeakHashMap<>();
   /** ARC shells fly for up to 20 minutes (Superb Warfare's own limit is 40 seconds). */
   public static final int ARC_SHELL_LIFE = 24000;
   /** Keeps the chunks around an ARC shell loaded and entity-ticking; refreshed every tick. */
   private static final TicketType<ChunkPos> SHELL_TICKET = TicketType.create("warsteel_arc_shell", Comparator.comparingLong(ChunkPos::toLong), 40);
   /** How many ticks of flight ahead to load, so the shell never stalls at the edge of loaded terrain. */
   private static final int LOOKAHEAD = 4;

   private record Tracked(ServerPlayer gunner, long firedAt) {
   }

   private ArtilleryMode() {
   }

   /** True if this shell was just fired in ARC mode by the gunner of a Flak 88. */
   public static boolean isArcShot(CannonShellEntity shell) {
      return shell.getOwner() instanceof ServerPlayer gunner && gunner.getVehicle() instanceof Flak88Entity gun && gun.arcMode()
         && gun.getSeatIndex(gunner) == 0;
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

      if (isArcShot(shell) && shell.getOwner() instanceof ServerPlayer gunner && gunner.getVehicle() instanceof Flak88Entity gun) {
         // Reduced propellant charge: same shell, lower muzzle velocity.
         shell.setDeltaMovement(shell.getDeltaMovement().scale(Flak88Entity.chargeFactor(gun.charge())));
         // No fuze or lifetime limit: the shell flies until it lands, however far that is.
         shell.setLife(ARC_SHELL_LIFE);
         this.loadAhead(shell);
         this.live.put(shell, new Tracked(gunner, event.getLevel().getGameTime()));
         PacketDistributor.sendToPlayer(
            gunner, new ArtilleryPayloads.ShellLaunched(shell.getId(), shell.position(), shell.getDeltaMovement(), shell.getCustomGravity())
         );
      }
   }

   private void loadAhead(CannonShellEntity shell) {
      if (!(shell.level() instanceof ServerLevel level)) {
         return;
      }

      Vec3 pos = shell.position();
      Vec3 vel = shell.getDeltaMovement();
      float g = shell.getCustomGravity();
      ChunkPos last = null;

      for (int i = 0; i <= LOOKAHEAD; i++) {
         if (pos.y < level.getMinBuildHeight() - 64) {
            break;
         }

         // Above the world the shell still needs its column loaded to keep ticking.
         ChunkPos cp = new ChunkPos(BlockPos.containing(pos.x, 0.0, pos.z));
         if (!cp.equals(last)) {
            // Distance 2: ticket level 31, the lowest level at which entities tick.
            level.getChunkSource().addRegionTicket(SHELL_TICKET, cp, 2, cp);
            last = cp;
         }

         pos = pos.add(vel);
         vel = vel.add(0.0, -g, 0.0);
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
            LOGGER.info("ARC shell came down at {} after {} ticks, {} blocks from the gun", shell.blockPosition(),
               shell.level().getGameTime() - t.firedAt(), (int)Math.sqrt(t.gunner().distanceToSqr(shell.getX(), t.gunner().getY(), shell.getZ())));
            if (!t.gunner().hasDisconnected()) {
               PacketDistributor.sendToPlayer(t.gunner(), new ArtilleryPayloads.ShellLanded(shell.getId(), shell.position()));
            }
         } else if (shell.level().getGameTime() - t.firedAt() > ARC_SHELL_LIFE + 20L) {
            it.remove();
         } else {
            this.loadAhead(shell);
         }
      }
   }
}
