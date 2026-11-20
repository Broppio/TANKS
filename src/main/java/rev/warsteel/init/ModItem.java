package rev.rev.warsteel.init;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import rev.rev.warsteel.item.AmmoItem;
import rev.rev.warsteel.item.AmmoSpec;
import rev.rev.warsteel.item.AmmoSpecs;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItem {
   public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("warsteel");
   public static final DeferredItem<Item> WARSTEEL_TANK_ICON = ITEMS.registerSimpleItem("warsteel-tank-item");
   public static final DeferredItem<Item> T26_HULL = ITEMS.registerSimpleItem("t26_hull");
   public static final DeferredItem<Item> T26_TURRET = ITEMS.registerSimpleItem("t26_turret");
   public static final DeferredItem<Item> T26_BARREL = ITEMS.registerSimpleItem("t26_barrel");
   public static final DeferredItem<Item> T26_TRACK = ITEMS.registerSimpleItem("t26_track");
   public static final DeferredItem<Item> SHERMAN_HULL = ITEMS.registerSimpleItem("sherman_hull");
   public static final DeferredItem<Item> SHERMAN_TURRET = ITEMS.registerSimpleItem("sherman_turret");
   public static final DeferredItem<Item> SHERMAN_BARREL = ITEMS.registerSimpleItem("sherman_barrel");
   public static final DeferredItem<Item> SHERMAN_TRACK = ITEMS.registerSimpleItem("sherman_track");
   public static final DeferredItem<Item> KV1_HULL = ITEMS.registerSimpleItem("kv1_hull");
   public static final DeferredItem<Item> KV1_TURRET = ITEMS.registerSimpleItem("kv1_turret");
   public static final DeferredItem<Item> KV1_BARREL = ITEMS.registerSimpleItem("kv1_barrel");
   public static final DeferredItem<Item> KV1_TRACK = ITEMS.registerSimpleItem("kv1_track");
   public static final DeferredItem<Item> KV2_HULL = ITEMS.registerSimpleItem("kv2_hull");
   public static final DeferredItem<Item> KV2_TURRET = ITEMS.registerSimpleItem("kv2_turret");
   public static final DeferredItem<Item> KV2_BARREL = ITEMS.registerSimpleItem("kv2_barrel");
   public static final DeferredItem<Item> KV2_TRACK = ITEMS.registerSimpleItem("kv2_track");
   public static final DeferredItem<Item> TIGER_HULL = ITEMS.registerSimpleItem("tiger_hull");
   public static final DeferredItem<Item> TIGER_TURRET = ITEMS.registerSimpleItem("tiger_turret");
   public static final DeferredItem<Item> TIGER_BARREL = ITEMS.registerSimpleItem("tiger_barrel");
   public static final DeferredItem<Item> TIGER_TRACK = ITEMS.registerSimpleItem("tiger_track");

   /** Historical tank rounds, keyed by id (see tools/gen_ammo.py). */
   public static final Map<String, DeferredItem<AmmoItem>> AMMO = new LinkedHashMap<>();

   static {
      for (AmmoSpec spec : AmmoSpecs.ALL) {
         AMMO.put(spec.id(), ITEMS.register(spec.id(), () -> new AmmoItem(spec)));
      }
   }
}
