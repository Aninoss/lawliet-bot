package websockets.syncserver.events;

import core.DiscordApiManager;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "CUSTOM_EMOJI")
public class OnCustomEmoji implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long emojiId = jsonObject.getLong("emoji_id");
        JSONObject responseJson = new JSONObject();

        DiscordApiManager.getInstance().getLocalCustomEmojiById(emojiId)
                .map(CustomEmoji::getMentionTag)
                .ifPresent(tag ->  responseJson.put("tag", tag));

        return responseJson;
    }

}
