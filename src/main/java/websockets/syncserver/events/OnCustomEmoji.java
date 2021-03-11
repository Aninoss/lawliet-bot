package websockets.syncserver.events;

import core.ShardManager;
import net.dv8tion.jda.api.entities.Emote;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "CUSTOM_EMOJI")
public class OnCustomEmoji implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long emojiId = jsonObject.getLong("emoji_id");
        JSONObject responseJson = new JSONObject();

        ShardManager.getInstance().getLocalEmoteById(emojiId)
                .map(Emote::getAsMention)
                .ifPresent(tag -> responseJson.put("tag", tag));

        return responseJson;
    }

}
