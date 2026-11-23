package rev.rev.warsteel.damage;

import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import rev.rev.warsteel.combat.ArmorData;
import rev.rev.warsteel.combat.ArmorPlate;
import rev.rev.warsteel.combat.ArmorRegistry;
import rev.rev.warsteel.combat.ShellData;
import rev.rev.warsteel.combat.ShellRegistry;

/**
 * /warsteel xray [seconds]  - outline armour plates and modules of nearby tanks with particles
 * /warsteel status          - module report for the nearest tank
 * /warsteel reset           - restore the nearest tank
 * /warsteel fire <shell>    - fire a real SBW cannon shell using a WarSteel shell profile
 */
public final class WarSteelCommands {
   public static final WarSteelCommands INSTANCE = new WarSteelCommands();
   /** Shells spawned by /warsteel fire carry their profile here instead of being resolved by vehicle. */
   private static final Map<net.minecraft.world.entity.Entity, ShellData> FORCED = Collections.synchronizedMap(new WeakHashMap<>());
   private final List<Xray> xrays = new ArrayList<>();
   /** Shells from /warsteel fire: their hit reports are logged at INFO for testing. */
   private static final java.util.Set<net.minecraft.world.entity.Entity> TEST_SHELLS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

   public static boolean isTestShell(net.minecraft.world.entity.Entity e) {
      return TEST_SHELLS.contains(e);
   }

   private record Xray(ServerPlayer player, long until, boolean hide) {
   }

   /** Tanks hidden while an x-ray is running, so outlines inside the hull can be seen. */
   private final java.util.Set<WarSteelTank> hidden = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

   private WarSteelCommands() {
   }

   @Nullable
   public static ShellData forcedProfile(net.minecraft.world.entity.Entity shell) {
      if (shell instanceof rev.rev.warsteel.entity.projectile.PanzerfaustWarheadEntity warhead) {
         return warhead.profile();
      }

      return FORCED.get(shell);
   }

   @SubscribeEvent
   public void onRegisterCommands(RegisterCommandsEvent event) {
      register(event.getDispatcher());
   }

   private static void register(CommandDispatcher<CommandSourceStack> d) {
      d.register(
         Commands.literal("warsteel")
            .requires(s -> s.hasPermission(2))
            .then(
               Commands.literal("xray")
                  .executes(c -> INSTANCE.xray(c, 15, true))
                  .then(
                     Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                        .executes(c -> INSTANCE.xray(c, IntegerArgumentType.getInteger(c, "seconds"), true))
                        .then(
                           Commands.argument("hideTanks", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                              .executes(c -> INSTANCE.xray(c, IntegerArgumentType.getInteger(c, "seconds"), com.mojang.brigadier.arguments.BoolArgumentType.getBool(c, "hideTanks")))
                        )
                  )
            )
            .then(Commands.literal("status").executes(WarSteelCommands::status))
            .then(Commands.literal("obbs").executes(WarSteelCommands::obbs))
            .then(Commands.literal("reset").executes(WarSteelCommands::reset))
            .then(Commands.literal("vshoot").executes(WarSteelCommands::vshoot))
            .then(Commands.literal("ship").executes(WarSteelCommands::shipStatus))
            .then(Commands.literal("torpedo").executes(WarSteelCommands::torpedo))
            .then(Commands.literal("use").executes(WarSteelCommands::useGun))
            .then(
               Commands.literal("damage")
                  .then(
                     Commands.argument("module", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((c, b) -> {
                           WarSteelTank t = nearest(c.getSource(), 32.0);
                           DamageModel m = t != null ? t.damageModel() : null;
                           return SharedSuggestionProvider.suggest(m == null ? List.of() : m.modules().stream().map(ModuleDef::name).toList(), b);
                        })
                        .then(
                           Commands.argument("amount", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0F))
                              .executes(WarSteelCommands::damage)
                        )
                  )
            )
            .then(
               Commands.literal("fire")
                  .then(
                     Commands.argument("shell", ResourceLocationArgument.id())
                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(ShellRegistry.INSTANCE.ids(), b))
                        .executes(WarSteelCommands::fire)
                  )
            )
      );
   }

   @Nullable
   private static WarSteelTank nearest(CommandSourceStack src, double range) {
      Vec3 p = src.getPosition();
      List<WarSteelTank> tanks = src.getLevel().getEntitiesOfClass(WarSteelTank.class, new AABB(p, p).inflate(range), t -> t.isAlive());
      return tanks.stream().min(Comparator.comparingDouble(t -> t.distanceToSqr(p))).orElse(null);
   }

   private int xray(CommandContext<CommandSourceStack> c, int seconds, boolean hide) {
      ServerPlayer player = c.getSource().getPlayer();
      if (player == null) {
         c.getSource().sendFailure(Component.literal("Players only"));
         return 0;
      }

      this.xrays.removeIf(x -> x.player == player);
      this.xrays.add(new Xray(player, player.serverLevel().getGameTime() + seconds * 20L, hide));
      c.getSource()
         .sendSuccess(
            () -> Component.literal("X-ray for " + seconds + "s: ")
               .append(Component.literal("plates ").withStyle(ChatFormatting.GOLD))
               .append(Component.literal("crew ").withStyle(ChatFormatting.BLUE))
               .append(Component.literal("ammo ").withStyle(ChatFormatting.RED))
               .append(Component.literal("engine ").withStyle(ChatFormatting.YELLOW))
               .append(Component.literal("fuel ").withStyle(ChatFormatting.LIGHT_PURPLE))
               .append(Component.literal("gun ").withStyle(ChatFormatting.WHITE))
               .append(Component.literal("drive/transmission ").withStyle(ChatFormatting.AQUA))
               .append(Component.literal("tracks").withStyle(ChatFormatting.DARK_GREEN)),
            false
         );
      return 1;
   }

   @SubscribeEvent
   public void onServerTick(ServerTickEvent.Post event) {
      if (this.xrays.isEmpty()) {
         if (!this.hidden.isEmpty()) {
            for (WarSteelTank t : this.hidden) {
               t.setInvisible(false);
            }

            this.hidden.clear();
         }

         return;
      }

      Iterator<Xray> it = this.xrays.iterator();

      while (it.hasNext()) {
         Xray x = it.next();
         ServerLevel level = x.player.serverLevel();
         if (x.player.isRemoved() || level.getGameTime() > x.until) {
            it.remove();
         } else if (level.getGameTime() % 10L == 0L) {
            // Only the nearest tank: several tanks' worth of outlines exceeds the client's particle limit.
            WarSteelTank nearestTank = level.getEntitiesOfClass(WarSteelTank.class, x.player.getBoundingBox().inflate(32.0))
               .stream()
               .min(Comparator.comparingDouble(t -> t.distanceToSqr(x.player)))
               .orElse(null);

            for (WarSteelTank tank : nearestTank == null ? List.<WarSteelTank>of() : List.of(nearestTank)) {
               if (x.hide && this.hidden.add(tank)) {
                  tank.setInvisible(true);
               } else if (!x.hide && this.hidden.remove(tank)) {
                  tank.setInvisible(false);
               }

               drawTank(x.player, level, tank);
            }
         }
      }
   }

   private static void drawTank(ServerPlayer player, ServerLevel level, WarSteelTank tank) {
      ArmorData armour = ArmorRegistry.INSTANCE.get(BuiltInRegistries.ENTITY_TYPE.getKey(tank.getType()));
      if (armour != null) {
         for (ArmorPlate p : armour.getPlates()) {
            draw(player, level, Obb.of(tank, p.getTransform(), p.getPosition(), p.getSize()), new Vector3f(1.0F, 0.55F, 0.0F), 0.6F);
         }
      }

      for (com.atsuishio.superbwarfare.tools.OBB o : tank.getOBBs()) {
         Vec3 c = new Vec3(o.center.x, o.center.y, o.center.z);
         draw(player, level, new Obb(c, o.rotation(), o.extents().x, o.extents().y, o.extents().z), new Vector3f(0.75F, 0.75F, 0.75F), 0.45F);
      }

      TankState st = tank.damageState();
      DamageModel model = st.model();
      if (model != null) {
         for (ModuleDef d : model.modules()) {
            Vector3f colour = st.destroyed(d.index()) ? new Vector3f(0.05F, 0.05F, 0.05F) : colour(d.type());
            draw(player, level, Obb.of(tank, d.transform(), d.position(), d.size()), colour, 1.0F);
         }
      }
   }

   private static Vector3f colour(ModuleType t) {
      return switch (t) {
         case CREW -> new Vector3f(0.2F, 0.4F, 1.0F);
         case AMMO_RACK -> new Vector3f(1.0F, 0.1F, 0.1F);
         case ENGINE -> new Vector3f(1.0F, 0.95F, 0.1F);
         case FUEL_TANK -> new Vector3f(0.9F, 0.2F, 0.9F);
         case BREECH, BARREL -> new Vector3f(1.0F, 1.0F, 1.0F);
         case TRANSMISSION, TURRET_DRIVE -> new Vector3f(0.1F, 0.9F, 0.9F);
         case TRACK -> new Vector3f(0.1F, 0.5F, 0.1F);
      };
   }

   private static void draw(ServerPlayer player, ServerLevel level, Obb box, Vector3f colour, float size) {
      DustParticleOptions dust = new DustParticleOptions(colour, size);

      for (Vec3[] e : box.edges()) {
         double len = e[0].distanceTo(e[1]);
         int steps = Math.max(1, (int)(len / 0.1));

         for (int i = 0; i <= steps; i++) {
            Vec3 p = e[0].lerp(e[1], (double)i / steps);
            level.sendParticles(player, dust, true, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private static int status(CommandContext<CommandSourceStack> c) {
      WarSteelTank tank = nearest(c.getSource(), 32.0);
      if (tank == null || tank.damageModel() == null) {
         c.getSource().sendFailure(Component.literal("No WarSteel tank with a damage model within 32 blocks"));
         return 0;
      }

      TankState st = tank.damageState();
      DamageModel model = st.model();
      MutableComponent msg = Component.literal(tank.getDisplayName().getString() + " - crew " + st.crewAlive() + "/" + st.crewTotal())
         .withStyle(ChatFormatting.BOLD);
      if (st.knockedOut()) {
         msg.append(Component.literal(" KNOCKED OUT").withStyle(ChatFormatting.DARK_RED));
      }

      if (st.burning()) {
         msg.append(Component.literal(" ON FIRE").withStyle(ChatFormatting.GOLD));
      }

      for (ModuleDef d : model.modules()) {
         float pct = 100.0F * st.health(d.index()) / d.maxHealth();
         ChatFormatting col = pct <= 0.0F ? ChatFormatting.DARK_GRAY : (pct < 60.0F ? ChatFormatting.GOLD : ChatFormatting.GREEN);
         msg.append(Component.literal("\n  " + d.name() + " (" + d.label() + "): " + (pct <= 0.0F ? "OUT" : Math.round(pct) + "%")).withStyle(col));
      }

      c.getSource().sendSuccess(() -> msg, false);
      rev.rev.warsteel.WarSteel.LOG_OBBS.info("status: {} || sync={}", msg.getString().replace('\n', '|'), st.encode());
      return 1;
   }

   /** Prints SBW's hit boxes in the tank's own Vehicle / Turret frames, for authoring plates. */
   private static int obbs(CommandContext<CommandSourceStack> c) {
      WarSteelTank tank = nearest(c.getSource(), 32.0);
      if (tank == null) {
         c.getSource().sendFailure(Component.literal("No WarSteel tank within 32 blocks"));
         return 0;
      }

      StringBuilder sb = new StringBuilder(BuiltInRegistries.ENTITY_TYPE.getKey(tank.getType()).toString());
      for (String frame : new String[]{"Vehicle", "Turret"}) {
         org.joml.Matrix4d inv = new org.joml.Matrix4d(tank.getTransformFromString(frame)).invert();
         for (com.atsuishio.superbwarfare.tools.OBB o : tank.getOBBs()) {
            org.joml.Vector4d l = inv.transform(new org.joml.Vector4d(o.center.x, o.center.y, o.center.z, 1.0));
            sb.append(String.format(java.util.Locale.ROOT, "%n[%s] part=%s centre=(%.3f, %.3f, %.3f) half=(%.3f, %.3f, %.3f)",
               frame, o.part, l.x, l.y, l.z, o.extents().x, o.extents().y, o.extents().z));
         }
      }

      rev.rev.warsteel.WarSteel.LOG_OBBS.info(sb.toString());
      c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
      return 1;
   }

   private static int damage(CommandContext<CommandSourceStack> c) {
      WarSteelTank tank = nearest(c.getSource(), 32.0);
      if (tank == null || tank.damageModel() == null) {
         c.getSource().sendFailure(Component.literal("No WarSteel tank with a damage model within 32 blocks"));
         return 0;
      }

      String module = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "module");
      float amount = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(c, "amount");
      try {
         TankDamage.Report r = TankDamage.damageModule(tank, c.getSource().getLevel(), module, amount, c.getSource().getEntity());
         c.getSource().sendSuccess(() -> Component.literal(module + ": ").append(r.summary()), false);
         rev.rev.warsteel.WarSteel.LOG_OBBS.info("damage {} {}: {}", module, amount, r.summary().getString());
         return 1;
      } catch (IllegalArgumentException e) {
         c.getSource().sendFailure(Component.literal(e.getMessage()));
         return 0;
      }
   }

   private static int reset(CommandContext<CommandSourceStack> c) {
      WarSteelTank tank = nearest(c.getSource(), 32.0);
      if (tank == null) {
         c.getSource().sendFailure(Component.literal("No WarSteel tank within 32 blocks"));
         return 0;
      }

      tank.resetDamage();
      tank.setHealth(tank.getMaxHealth());
      c.getSource().sendSuccess(() -> Component.literal("Restored " + tank.getDisplayName().getString()), false);
      return 1;
   }

   private static int fire(CommandContext<CommandSourceStack> c) {
      CommandSourceStack src = c.getSource();
      ResourceLocation id = ResourceLocationArgument.getId(c, "shell");
      ShellData data = ShellRegistry.INSTANCE.byId(id);
      if (data == null) {
         c.getSource().sendFailure(Component.literal("Unknown shell " + id));
         return 0;
      }

      ServerLevel level = src.getLevel();
      CannonShellEntity shell = new CannonShellEntity(ModEntities.CANNON_SHELL.get(), level);
      shell.setOwner(src.getEntity());
      shell.setType(data.isContactFuzed() ? CannonShellEntity.Type.HE : CannonShellEntity.Type.AP);
      shell.setDamage((float)data.getDamage());
      shell.setExplosionDamage(data.isContactFuzed() ? 40.0F : 0.0F);
      shell.setExplosionRadius(data.isContactFuzed() ? 2.0F : 0.0F);
      // Works from players, command blocks and functions (use execute positioned/facing).
      Vec3 eye = src.getEntity() != null ? src.getEntity().getEyePosition() : src.getPosition();
      Vec3 look = Vec3.directionFromRotation(src.getRotation());
      if (src.getEntity() != null && src.getAnchor() == net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.FEET) {
         eye = src.getPosition().add(0.0, src.getEntity().getEyeHeight(), 0.0);
      }
      shell.setPos(eye.add(look.scale(1.5)));
      shell.shoot(look.x, look.y, look.z, (float)data.getVelocity(), 0.0F);
      FORCED.put(shell, data);
      TEST_SHELLS.add(shell);
      level.addFreshEntity(shell);
      c.getSource().sendSuccess(() -> Component.literal("Fired " + id + " (" + data.getShellClass() + ", " + data.getPenetration().getMuzzle() + " mm)"), false);
      return 1;
   }

   /** Testing: the player's vehicle fires its main gun as if they had clicked. */
   private static int vshoot(CommandContext<CommandSourceStack> c) {
      if (!(c.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity living)
         || !(living.getVehicle() instanceof com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity vehicle)) {
         c.getSource().sendFailure(Component.literal("Not in a vehicle"));
         return 0;
      }

      // Testing aid: chamber a round so the shot isn't refused for want of ammunition.
      com.atsuishio.superbwarfare.data.gun.GunData gun = vehicle.getGunData("Cannon");
      if (gun != null && gun.ammo.get() <= 0) {
         gun.ammo.set(1);
         gun.save();
      }

      vehicle.vehicleShoot(living, "Cannon", null);
      return 1;
   }

   /** Testing: fire the gun in the player's main hand as if they had clicked. */
   private static int useGun(CommandContext<CommandSourceStack> c) {
      if (!(c.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player player)) {
         return 0;
      }

      if (!(player.getMainHandItem().getItem() instanceof com.atsuishio.superbwarfare.item.gun.GunItem gun)) {
         // Anything else: use it as if right-clicked in the air.
         net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> r =
            player.getMainHandItem().use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
         player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, r.getObject());
         c.getSource().sendSuccess(() -> Component.literal("Used: now holding " + player.getMainHandItem()), false);
         return 1;
      }

      com.atsuishio.superbwarfare.data.gun.GunData data = com.atsuishio.superbwarfare.data.gun.GunData.from(player.getMainHandItem());
      gun.shoot(data, player, 0.0, false, null);
      data.save();
      return 1;
   }

   /** Flooding report for the nearest ship (also logged). */
   private static int shipStatus(CommandContext<CommandSourceStack> c) {
      net.minecraft.world.phys.Vec3 at = c.getSource().getPosition();
      rev.rev.warsteel.entity.vehicle.T22Entity ship = c.getSource().getLevel()
         .getEntitiesOfClass(rev.rev.warsteel.entity.vehicle.T22Entity.class, new net.minecraft.world.phys.AABB(at, at).inflate(256))
         .stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(at))).orElse(null);
      if (ship == null) {
         c.getSource().sendFailure(Component.literal("No ship within 256 blocks"));
         return 0;
      }

      rev.rev.warsteel.naval.ShipFlooding f = ship.flooding();
      StringBuilder comps = new StringBuilder();
      for (int i = 0; i < f.compartments(); i++) {
         comps.append(String.format(java.util.Locale.ROOT, "%s%.0f%%", i == 0 ? "" : " ", f.fill(i) * 100));
      }

      String msg = String.format(java.util.Locale.ROOT,
         "%s: keel y %.2f, flooded %.0f m3 (%.0f%% of reserve), trim %.1f deg, list %.1f deg, speed %.2f b/t, %d holes (%.2f m2), compartments aft->bow [%s]%s",
         ship.getDisplayName().getString(), ship.getY(), f.totalWater(), f.floodRatio() * 100, ship.getXRot(), ship.getRoll(),
         ship.getDeltaMovement().horizontalDistance(), f.holes.size(), f.holes.stream().mapToDouble(h -> h.area()).sum(), comps,
         ship.isWreck() ? ", WRECK" : "");
      org.slf4j.LoggerFactory.getLogger("warsteel/ship").info(msg);
      c.getSource().sendSuccess(() -> Component.literal(msg), false);
      return 1;
   }

   /** Testing: launch a G7a from where the source stands, along its facing. */
   private static int torpedo(CommandContext<CommandSourceStack> c) {
      rev.rev.warsteel.naval.TorpedoEntity t = rev.rev.warsteel.init.ModEntities.TORPEDO.get().create(c.getSource().getLevel());
      if (t == null) {
         return 0;
      }

      t.launch(c.getSource().getEntity(), null, c.getSource().getPosition(), c.getSource().getRotation().y);
      c.getSource().getLevel().addFreshEntity(t);
      return 1;
   }
}
