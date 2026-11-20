package rev.rev.warsteel.client.replay;

import rev.rev.warsteel.network.ShotReplayPayload;

/** Only ever loaded on the client, when a replay packet arrives. */
public final class ClientReplayHandler {
   private ClientReplayHandler() {
   }

   public static void handle(ShotReplayPayload payload) {
      ShotReplayHud.INSTANCE.play(payload);
   }
}
