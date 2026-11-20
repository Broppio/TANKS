package rev.rev.warsteel.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Packets for the Flak 88's ARC (indirect fire) mode and its shell camera. */
public final class ArtilleryPayloads {
   private ArtilleryPayloads() {
   }

   private static Vec3 readVec(RegistryFriendlyByteBuf buf) {
      return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
   }

   private static void writeVec(RegistryFriendlyByteBuf buf, Vec3 v) {
      buf.writeDouble(v.x);
      buf.writeDouble(v.y);
      buf.writeDouble(v.z);
   }

   /** Client -> server: 0 toggle ARC mode, 1 next charge, 2 previous charge. */
   public record ArcControl(byte action) implements CustomPacketPayload {
      public static final byte TOGGLE = 0;
      public static final byte CHARGE_UP = 1;
      public static final byte CHARGE_DOWN = 2;
      public static final Type<ArcControl> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("warsteel", "arc_control"));
      public static final StreamCodec<RegistryFriendlyByteBuf, ArcControl> CODEC =
         StreamCodec.of((buf, p) -> buf.writeByte(p.action), buf -> new ArcControl(buf.readByte()));

      @Override
      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   /** Server -> shooter: a shell was fired in ARC mode; its launch state, for the shell camera. */
   public record ShellLaunched(int shellId, Vec3 pos, Vec3 velocity, float gravity) implements CustomPacketPayload {
      public static final Type<ShellLaunched> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("warsteel", "shell_launched"));
      public static final StreamCodec<RegistryFriendlyByteBuf, ShellLaunched> CODEC = StreamCodec.of((buf, p) -> {
         buf.writeVarInt(p.shellId);
         writeVec(buf, p.pos);
         writeVec(buf, p.velocity);
         buf.writeFloat(p.gravity);
      }, buf -> new ShellLaunched(buf.readVarInt(), readVec(buf), readVec(buf), buf.readFloat()));

      @Override
      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   /** Server -> shooter: the shell hit something (or burst, or expired) here. */
   public record ShellLanded(int shellId, Vec3 pos) implements CustomPacketPayload {
      public static final Type<ShellLanded> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("warsteel", "shell_landed"));
      public static final StreamCodec<RegistryFriendlyByteBuf, ShellLanded> CODEC = StreamCodec.of((buf, p) -> {
         buf.writeVarInt(p.shellId);
         writeVec(buf, p.pos);
      }, buf -> new ShellLanded(buf.readVarInt(), readVec(buf)));

      @Override
      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
