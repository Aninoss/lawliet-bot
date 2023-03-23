package events.sync.events;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import core.ShardManager;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONArray;
import org.json.JSONObject;

@SyncServerEvent(event = "PADDLE_PASSTHROUGH")
public class OnPaddlePassthrough implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        User user = ShardManager.fetchUserById(userId).join();

        JSONObject passthroughJson = new JSONObject();
        passthroughJson.put("discord_id", userId);
        passthroughJson.put("discord_tag", Base64.getEncoder().encodeToString(user.getAsTag().getBytes(StandardCharsets.UTF_8)));
        passthroughJson.put("discord_avatar", user.getAvatarUrl());
        passthroughJson.put("preset_guilds", new JSONArray());

        JSONObject responseJson = new JSONObject();
        responseJson.put("passthrough", passthroughJson.toString());
        return responseJson;
    }

}
