package websockets.syncserver.events;

import core.cache.PatreonCache;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

import java.util.HashMap;

@SyncServerEvent(event = "PATREON")
public class OnPatreon implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnPatreon.class);

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        HashMap<Long, Integer> userPatreonMap = PatreonCache.userPatreonMapFromJson(jsonObject);
        PatreonCache.getInstance().setValue(userPatreonMap);
        LOGGER.info("Received new Patreon list with {} entries", userPatreonMap.size());
        return null;
    }

}
