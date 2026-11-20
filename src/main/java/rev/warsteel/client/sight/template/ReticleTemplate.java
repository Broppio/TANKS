package rev.rev.warsteel.client.sight.template;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public interface ReticleTemplate {
   @NotNull
   String getId();

   void draw(@NotNull SightContext var1);
}
