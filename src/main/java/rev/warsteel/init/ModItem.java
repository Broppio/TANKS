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

   public static final DeferredItem<rev.rev.warsteel.item.gun.PanzerfaustItem> PANZERFAUST_60 =
      ITEMS.register("panzerfaust_60", rev.rev.warsteel.item.gun.PanzerfaustItem::new);

   /**
    * Factory-built vehicles (see VehicleCrateItem and compat.NationsAtWarFactories). Production
    * time in real days scales with how much the vehicle is worth: a T-26 or a towed gun comes off
    * the line in about a week or two; a KV-2 or a Tiger takes over a month.
    */
   public static final DeferredItem<rev.rev.warsteel.item.VehicleCrateItem> PAK38_CRATE = crate("pak38_crate", ModEntities.PAK_38, 8);
   public static final DeferredItem<rev.rev.warsteel.item.VehicleCrateItem> T26_CRATE = crate("t26_crate", ModEntities.T_26, 10);
   public static final DeferredItem<rev.rev.warsteel.item.VehicleCrateItem> SHERMAN_CRATE = crate("sherman_crate", ModEntities.M4A3_SHERMAN, 21);
   public static final DeferredItem<rev.rev.warsteel.item.VehicleCrateItem> FLAK88_CRATE = crate("flak88_crate", ModEntities.FLAK_88, 24);
   public static final DeferredItem<rev.rev.warsteel.item.VehicleCrateItem> KV1_CRATE = crate("kv1_crate", ModEntities.KV_1, 26);
   public static final DeferredItem<rev.rev.warsteel.item.VehicleCrateItem> KV2_CRATE = crate("kv2_crate", ModEntities.KV_2, 34);
   public static final DeferredItem<rev.rev.warsteel.item.VehicleCrateItem> TIGER_CRATE = crate("tiger_crate", ModEntities.TigerH1, 42);
   /** A whole warship: two months on the slipway. */
   public static final DeferredItem<rev.rev.warsteel.item.VehicleCrateItem> T22_CRATE = crate("t22_crate", ModEntities.T_22, 60);
   /** G7a torpedo, for the T-22's tubes. */
   public static final DeferredItem<Item> TORPEDO_G7A = ITEMS.register("torpedo_g7a", () -> new Item(new Item.Properties().stacksTo(3)));

   private static DeferredItem<rev.rev.warsteel.item.VehicleCrateItem> crate(String id, java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<?>> type, double days) {
      return ITEMS.register(id, () -> new rev.rev.warsteel.item.VehicleCrateItem(type, days));
   }

   /** Historical tank rounds, keyed by id (see tools/gen_ammo.py). */
   public static final Map<String, DeferredItem<AmmoItem>> AMMO = new LinkedHashMap<>();

   static {
      for (AmmoSpec spec : AmmoSpecs.ALL) {
         AMMO.put(spec.id(), ITEMS.register(spec.id(), () -> new AmmoItem(spec)));
      }
   }
}
