package websockets.syncserver.events;

import core.ShardManager;
import org.json.JSONArray;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "MUTUAL_SERVERS")
public class OnMutualServers implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        JSONObject jsonResponse = new JSONObject();
        JSONArray jsonGuilds = new JSONArray();

        ShardManager.getInstance().getLocalMutualGuilds(userId).forEach(guild -> {
            String iconUrl = guild.getIconUrl();
            if (iconUrl == null) {
                iconUrl = "";
            }

            JSONObject jsonGuild = new JSONObject();
            jsonGuild.put("guild_id", guild.getIdLong());
            jsonGuild.put("name", guild.getName());
            jsonGuild.put("icon_url", iconUrl);
            jsonGuilds.put(jsonGuild);
        });

        jsonResponse.put("guilds", jsonGuilds);
        return jsonResponse;
    }

}
