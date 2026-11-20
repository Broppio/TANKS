package rev.rev.warsteel.client.sight;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class SightDataKt {
   private static final String optString(JsonObject o, String key, String fallback) {
      String var10000 = GsonHelper.getAsString(o, key, fallback);
      if (var10000 == null) {
         var10000 = fallback;
      }

      return var10000;
   }

   // $VF: synthetic method
   public static final String access$optString(JsonObject o, String key, String fallback) {
      return optString(o, key, fallback);
   }
}
