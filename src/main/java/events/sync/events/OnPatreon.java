package events.sync.events;

import core.MainLogger;
import core.patreon.PatreonData;
import core.patreon.PatreonCache;
import org.json.JSONObject;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;

@SyncServerEvent(event = "PATREON")
public class OnPatreon implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        PatreonData patreonData = PatreonCache.patreonDataFromJson(jsonObject);
        PatreonCache.getInstance().setValue(patreonData);
        MainLogger.get().info("Premium data received ({} users, {} old users, {} unlocked guilds, {} unlocked guilds plus)",
                patreonData.getUserTierMap().size(),
                patreonData.getOldUserList().size(),
                patreonData.getGuildList().size(),
                patreonData.getGuildPlusList().size()
        );
        return null;
    }

}
