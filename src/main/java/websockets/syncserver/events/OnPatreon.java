package websockets.syncserver.events;

import java.util.HashMap;
import core.cache.PatreonCache;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "PATREON")
public class OnPatreon implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        HashMap<Long, Integer> userPatreonMap = PatreonCache.userPatreonMapFromJson(jsonObject);
        PatreonCache.getInstance().setValue(userPatreonMap);
        MainLogger.get().info("Received new Patreon list with {} entries", userPatreonMap.size());
        return null;
    }

}
