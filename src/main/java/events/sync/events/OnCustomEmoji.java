package events.sync.events;

import core.ShardManager;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import org.json.JSONObject;

@SyncServerEvent(event = "CUSTOM_EMOJI")
public class OnCustomEmoji implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long emojiId = jsonObject.getLong("emoji_id");
        JSONObject responseJson = new JSONObject();

        ShardManager.getLocalCustomEmojiById(emojiId)
                .map(CustomEmoji::getAsMention)
                .ifPresent(tag -> responseJson.put("tag", tag));

        return responseJson;
    }

}
