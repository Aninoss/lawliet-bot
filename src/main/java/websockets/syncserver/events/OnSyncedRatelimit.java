package websockets.syncserver.events;

import core.CustomSessionController;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "SYNCED_RATELIMIT")
public class OnSyncedRatelimit implements SyncServerFunction {

    @Override
    public synchronized JSONObject apply(JSONObject jsonObject) {
        CustomSessionController.getInstance().loadGlobalRatelimit(jsonObject.getLong("ratelimit"));
        return null;
    }

}
