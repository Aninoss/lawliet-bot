package websockets.syncserver.events;

import core.MainLogger;
import core.PatreonData;
import core.cache.PatreonCache;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "PATREON")
public class OnPatreon implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        PatreonData patreonData = PatreonCache.patreonDataFromJson(jsonObject);
        PatreonCache.getInstance().setValue(patreonData);
        MainLogger.get().info("Received new Patreon list with {} users ({} old) and {} unlocked guilds", patreonData.getUserMap().size(), patreonData.getOldUsersList().size(), patreonData.getGuildList().size());
        return null;
    }

}
