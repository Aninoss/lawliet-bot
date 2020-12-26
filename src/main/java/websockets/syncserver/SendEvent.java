package websockets.syncserver;

import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;

public class SendEvent {

    private SendEvent() {}

    public static CompletableFuture<JSONObject> sendFullyConnected() {
        return SyncManager.getInstance().getClient().send("CLUSTER_FULLY_CONNECTED", new JSONObject());
    }

}
