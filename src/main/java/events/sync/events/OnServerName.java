package events.sync.events;

import core.ShardManager;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;

@SyncServerEvent(event = "SERVER_NAME")
public class OnServerName implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long serverId = jsonObject.getLong("server_id");
        JSONObject responseJson = new JSONObject();

        ShardManager.getLocalGuildById(serverId)
                .map(Guild::getName)
                .ifPresent(name -> responseJson.put("name", name));

        return responseJson;
    }

}
