package rev.rev.warsteel.client.sight.template;

import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.RangesKt;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
@SourceDebugExtension({"SMAP\nTemplates.kt\nKotlin\n*S Kotlin\n*F\n+ 1 Templates.kt\nrev/rev/warsteel/client/sight/template/Templates\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n+ 3 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,39:1\n1#2:40\n1202#3,2:41\n1230#3,4:43\n*S KotlinDebug\n*F\n+ 1 Templates.kt\nrev/rev/warsteel/client/sight/template/Templates\n*L\n26#1:41,2\n26#1:43,4\n*E\n"})
public final class Templates {
   @NotNull
   public static final Templates INSTANCE = new Templates();
   private static final Logger LOG = LogUtils.getLogger();
   @NotNull
   private static final Map<String, ReticleTemplate> all;
   @NotNull
   private static final HashSet<String> warned;

   private Templates() {
   }

   @NotNull
   public final ReticleTemplate get(@NotNull String id) {
      ReticleTemplate var2 = all.get(id);
      if (var2 != null) {
         return var2;
      } else {
         if (warned.add(id)) {
            LOG.warn("unknown warsteel reticle template \"{}\", falling back to binocular. known: {}", id, all.keySet());
         }

         return BinocularTemplate.INSTANCE;
      }
   }

   static {
      ReticleTemplate[] $this$associateBy$iv = new ReticleTemplate[]{
         ShermanStadiaTemplate.INSTANCE,
         TigerArcTemplate.INSTANCE,
         KvQuadrantTemplate.INSTANCE,
         T26TripleTemplate.INSTANCE,
         MgCrossTemplate.INSTANCE,
         BinocularTemplate.INSTANCE
      };
      Iterable var11 = CollectionsKt.listOf($this$associateBy$iv);
      int capacity$iv = RangesKt.coerceAtLeast(MapsKt.mapCapacity(CollectionsKt.collectionSizeOrDefault(var11, 10)), 16);
      Map destination$iv$iv = new LinkedHashMap(capacity$iv);

      for (Object element$iv$iv : var11) {
         ReticleTemplate it = (ReticleTemplate)element$iv$iv;
         destination$iv$iv.put(it.getId(), element$iv$iv);
      }

      all = destination$iv$iv;
      warned = new HashSet<>();
   }
}
