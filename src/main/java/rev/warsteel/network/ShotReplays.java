package rev.rev.warsteel.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;
import org.joml.Vector3f;
import org.joml.Vector4d;
import rev.rev.warsteel.combat.ArmorData;
import rev.rev.warsteel.combat.ArmorPlate;
import rev.rev.warsteel.damage.DamageModel;
import rev.rev.warsteel.damage.ModuleDef;
import rev.rev.warsteel.damage.Obb;
import rev.rev.warsteel.damage.TankDamage;
import rev.rev.warsteel.damage.TankState;
import rev.rev.warsteel.damage.WarSteelTank;

/** Builds a {@link ShotReplayPayload} from a resolved hit and sends it to the shooter. */
public final class ShotReplays {
   private ShotReplays() {
   }

   public static void send(
      ServerPlayer shooter,
      WarSteelTank tank,
      ArmorData armour,
      int entryPlate,
      Vec3 hit,
      Vec3 dir,
      @Nullable Vec3 outDir,
      Component headline,
      @Nullable TankDamage.Report report
   ) {
      Matrix4d toLocal = new Matrix4d(tank.getTransformFromString("Vehicle")).invert();
      List<ShotReplayPayload.Box> boxes = new ArrayList<>();
      List<ArmorPlate> plates = armour.getPlates();

      for (int i = 0; i < plates.size(); i++) {
         ArmorPlate p = plates.get(i);
         boxes.add(new ShotReplayPayload.Box(corners(Obb.of(tank, p.getTransform(), p.getPosition(), p.getSize()), toLocal), (byte)0, (byte)0, -1.0F, i == entryPlate));
      }

      TankState st = tank.damageState();
      DamageModel model = st.model();
      if (model != null) {
         for (ModuleDef d : model.modules()) {
            byte state;
            float time = -1.0F;
            if (report != null && report.outBefore.contains(d.index()) || report == null && st.destroyed(d.index())) {
               state = ShotReplayPayload.Box.WAS_OUT;
            } else if (report != null && report.hitTime.containsKey(d.index())) {
               time = report.hitTime.get(d.index());
               state = st.destroyed(d.index()) ? ShotReplayPayload.Box.DESTROYED : ShotReplayPayload.Box.DAMAGED;
            } else {
               state = ShotReplayPayload.Box.INTACT;
            }

            boxes.add(new ShotReplayPayload.Box(corners(Obb.of(tank, d.transform(), d.position(), d.size()), toLocal), kind(d), state, time, false));
         }
      }

      List<ShotReplayPayload.Ray> rays = new ArrayList<>();
      if (report != null) {
         for (TankDamage.ShotRay r : report.rays) {
            if (rays.size() >= 260) {
               break;
            }

            rays.add(new ShotReplayPayload.Ray(local(r.from(), toLocal), local(r.to(), toLocal), (byte)r.kind(), r.t0()));
         }
      }

      List<ShotReplayPayload.Line> lines = new ArrayList<>();
      lines.add(new ShotReplayPayload.Line(headline.getString(), colour(headline, 0xFFFFFF)));
      if (report != null) {
         for (Component c : report.eventLines()) {
            lines.add(new ShotReplayPayload.Line(c.getString(), colour(c, 0xDDDDDD)));
         }
      }

      ShotReplayPayload payload = new ShotReplayPayload(
         tank.getDisplayName().getString(),
         lines,
         boxes,
         rays,
         local(hit.subtract(dir.scale(7.0)), toLocal),
         local(hit, toLocal),
         outDir != null ? local(hit.add(outDir.normalize().scale(6.0)), toLocal) : null,
         report != null && report.burstPoint != null ? local(report.burstPoint, toLocal) : null,
         report != null && report.burstPoint != null ? (float)report.burstPoint.distanceTo(hit) : 0.0F
      );
      PacketDistributor.sendToPlayer(shooter, payload);
   }

   private static byte kind(ModuleDef d) {
      return switch (d.type()) {
         case CREW -> 1;
         case AMMO_RACK -> 2;
         case ENGINE -> 3;
         case FUEL_TANK -> 4;
         case BREECH, BARREL -> 5;
         case TRANSMISSION, TURRET_DRIVE -> 6;
         case TRACK -> 7;
      };
   }

   private static int colour(Component c, int fallback) {
      TextColor col = c.getStyle().getColor();
      if (col == null && !c.getSiblings().isEmpty()) {
         col = c.getSiblings().get(0).getStyle().getColor();
      }

      return col != null ? col.getValue() : fallback;
   }

   /** Corner i has x = +h when bit 2 set, y when bit 1, z when bit 0. */
   private static float[] corners(Obb box, Matrix4d toLocal) {
      float[] out = new float[24];
      int n = 0;

      for (int x = -1; x <= 1; x += 2) {
         for (int y = -1; y <= 1; y += 2) {
            for (int z = -1; z <= 1; z += 2) {
               Vector3f v = local(box.toWorld(x * box.hx, y * box.hy, z * box.hz), toLocal);
               out[n++] = v.x;
               out[n++] = v.y;
               out[n++] = v.z;
            }
         }
      }

      return out;
   }

   private static Vector3f local(Vec3 world, Matrix4d toLocal) {
      Vector4d v = toLocal.transform(new Vector4d(world.x, world.y, world.z, 1.0));
      return new Vector3f((float)v.x, (float)v.y, (float)v.z);
   }
}
