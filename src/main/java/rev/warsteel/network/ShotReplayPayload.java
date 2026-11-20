package rev.rev.warsteel.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Everything the shooter's client needs to replay one hit, in the target's own frame
 * (Vehicle transform), so the replay is independent of where the tank was.
 */
public record ShotReplayPayload(
   String title, List<Line> lines, List<Box> boxes, List<Ray> rays, Vector3f shellFrom, Vector3f shellHit, @Nullable Vector3f shellOut, @Nullable Vector3f burst,
   float burstTime
) implements CustomPacketPayload {
   public static final Type<ShotReplayPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("warsteel", "shot_replay"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ShotReplayPayload> CODEC = StreamCodec.of((buf, p) -> p.write(buf), ShotReplayPayload::read);

   /** Kinds: 0 armour plate, 1 crew, 2 ammo, 3 engine, 4 fuel, 5 gun, 6 drive/transmission, 7 track. */
   public record Box(float[] corners, byte kind, byte state, float hitTime, boolean entry) {
      /** States: 0 intact, 1 damaged by this shot, 2 destroyed by this shot, 3 already out. */
      public static final byte INTACT = 0;
      public static final byte DAMAGED = 1;
      public static final byte DESTROYED = 2;
      public static final byte WAS_OUT = 3;
   }

   /** Kinds: 0 shell core, 1 spall, 2 burst fragment. t0 = distance along the shot (blocks). */
   public record Ray(Vector3f from, Vector3f to, byte kind, float t0) {
   }

   public record Line(String text, int colour) {
   }

   /** Never let an over-long string fail the packet (that disconnects the player). */
   private static String clip(String s, int max) {
      return s.length() <= max ? s : s.substring(0, max - 1) + "…";
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   private void write(RegistryFriendlyByteBuf buf) {
      buf.writeUtf(clip(this.title, 64), 64);
      buf.writeVarInt(this.lines.size());

      for (Line l : this.lines) {
         buf.writeUtf(clip(l.text, 256), 256);
         buf.writeInt(l.colour);
      }

      buf.writeVarInt(this.boxes.size());

      for (Box b : this.boxes) {
         for (float f : b.corners) {
            buf.writeFloat(f);
         }

         buf.writeByte(b.kind);
         buf.writeByte(b.state);
         buf.writeFloat(b.hitTime);
         buf.writeBoolean(b.entry);
      }

      buf.writeVarInt(this.rays.size());

      for (Ray r : this.rays) {
         buf.writeVector3f(r.from);
         buf.writeVector3f(r.to);
         buf.writeByte(r.kind);
         buf.writeFloat(r.t0);
      }

      buf.writeVector3f(this.shellFrom);
      buf.writeVector3f(this.shellHit);
      buf.writeBoolean(this.shellOut != null);
      if (this.shellOut != null) {
         buf.writeVector3f(this.shellOut);
      }

      buf.writeBoolean(this.burst != null);
      if (this.burst != null) {
         buf.writeVector3f(this.burst);
      }

      buf.writeFloat(this.burstTime);
   }

   private static ShotReplayPayload read(RegistryFriendlyByteBuf buf) {
      String title = buf.readUtf(64);
      int n = buf.readVarInt();
      List<Line> lines = new ArrayList<>(n);

      for (int i = 0; i < n; i++) {
         lines.add(new Line(buf.readUtf(256), buf.readInt()));
      }

      n = buf.readVarInt();
      List<Box> boxes = new ArrayList<>(n);

      for (int i = 0; i < n; i++) {
         float[] c = new float[24];

         for (int j = 0; j < 24; j++) {
            c[j] = buf.readFloat();
         }

         boxes.add(new Box(c, buf.readByte(), buf.readByte(), buf.readFloat(), buf.readBoolean()));
      }

      n = buf.readVarInt();
      List<Ray> rays = new ArrayList<>(n);

      for (int i = 0; i < n; i++) {
         rays.add(new Ray(buf.readVector3f(), buf.readVector3f(), buf.readByte(), buf.readFloat()));
      }

      Vector3f from = buf.readVector3f();
      Vector3f hit = buf.readVector3f();
      Vector3f out = buf.readBoolean() ? buf.readVector3f() : null;
      Vector3f burst = buf.readBoolean() ? buf.readVector3f() : null;
      return new ShotReplayPayload(title, lines, boxes, rays, from, hit, out, burst, buf.readFloat());
   }
}
