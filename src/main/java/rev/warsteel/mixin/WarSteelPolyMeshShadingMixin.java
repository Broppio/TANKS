package rev.rev.warsteel.mixin;

import com.atsuishio.superbwarfare.resource.model.BedrockModelReloadListener;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BonesItem;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.GeometryModelLegacy;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.GeometryModelNew;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.PolyMeshItem;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {BedrockModelReloadListener.class},
   remap = false
)
public class WarSteelPolyMeshShadingMixin {
   private static final String WARSTEEL_NAMESPACE = "warsteel";

   @Inject(
      method = {"prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/Map;"},
      at = {@At("RETURN")},
      require = 0
   )
   private void warsteel$flattenVehiclePolyMeshNormals(
      ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfoReturnable<Map<ResourceLocation, BedrockModelPOJO>> cir
   ) {
      Map<ResourceLocation, BedrockModelPOJO> models = (Map<ResourceLocation, BedrockModelPOJO>)cir.getReturnValue();
      if (models != null) {
         for (Entry<ResourceLocation, BedrockModelPOJO> entry : models.entrySet()) {
            ResourceLocation location = entry.getKey();
            if (location != null && "warsteel".equals(location.getNamespace())) {
               stripPolyMeshNormals(entry.getValue());
            }
         }
      }
   }

   private static void stripPolyMeshNormals(BedrockModelPOJO pojo) {
      if (pojo != null) {
         GeometryModelNew modern = pojo.getGeometryModelNew();
         if (modern != null) {
            stripBones(modern.getBones());
         }

         GeometryModelLegacy legacy = pojo.getGeometryModelLegacy();
         if (legacy != null) {
            stripBones(legacy.getBones());
         }
      }
   }

   private static void stripBones(BonesItem[] bones) {
      if (bones != null) {
         for (BonesItem bone : bones) {
            if (bone != null) {
               PolyMeshItem polyMesh = bone.getPolyMesh();
               if (polyMesh != null) {
                  clearNormals(polyMesh);
               }
            }
         }
      }
   }

   private static void clearNormals(PolyMeshItem polyMesh) {
      try {
         Field field = PolyMeshItem.class.getDeclaredField("normals");
         field.setAccessible(true);
         field.set(polyMesh, null);
      } catch (SecurityException | ReflectiveOperationException var2) {
      }
   }
}
