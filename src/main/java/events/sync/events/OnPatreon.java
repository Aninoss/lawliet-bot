package events.sync.events;

import core.MainLogger;
import core.PatreonData;
import core.cache.PatreonCache;
import org.json.JSONObject;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;

@SyncServerEvent(event = "PATREON")
public class OnPatreon implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        PatreonData patreonData = PatreonCache.patreonDataFromJson(jsonObject);
        PatreonCache.getInstance().setValue(patreonData);
        MainLogger.get().info("Received new Patreon list with {} users ({} old) and {} unlocked guilds", patreonData.getUserTierMap().size(), patreonData.getOldUserList().size(), patreonData.getGuildList().size());
        return null;
    }

}
