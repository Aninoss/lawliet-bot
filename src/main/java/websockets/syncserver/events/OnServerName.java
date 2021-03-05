package websockets.syncserver.events;

import core.ShardManager;
import org.javacord.api.entity.Nameable;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "SERVER_NAME")
public class OnServerName implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long serverId = jsonObject.getLong("server_id");
        JSONObject responseJson = new JSONObject();

        ShardManager.getInstance().getLocalGuildById(serverId)
                .map(Nameable::getName)
                .ifPresent(name ->  responseJson.put("name", name));

        return responseJson;
    }

}
