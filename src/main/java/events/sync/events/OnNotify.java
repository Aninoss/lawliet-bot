package events.sync.events;

import core.EmbedFactory;
import core.ShardManager;
import core.utils.JDAUtil;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import net.dv8tion.jda.api.EmbedBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SyncServerEvent(event = "NOTIFY")
public class OnNotify implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnNotify.class);

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        EmbedBuilder eb = EmbedFactory.getEmbedDefault();

        if (jsonObject.has("title")) eb.setTitle(jsonObject.getString("title"));
        if (jsonObject.has("description")) eb.setDescription(jsonObject.getString("description"));
        if (jsonObject.has("author")) eb.setAuthor(jsonObject.getString("author"));
        if (jsonObject.has("thumbnail")) eb.setThumbnail(jsonObject.getString("thumbnail"));
        if (jsonObject.has("image")) eb.setImage(jsonObject.getString("image"));
        if (jsonObject.has("footer")) eb.setFooter(jsonObject.getString("footer"));

        JSONObject responseJson = new JSONObject();
        try {
            JDAUtil.openPrivateChannel(ShardManager.getAnyJDA().get(), userId)
                    .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                    .complete();
            responseJson.put("success", true);
        } catch (Throwable e) {
            LOGGER.error("Error while sending notification", e);
            responseJson.put("success", false);
        }
        return responseJson;
    }

}
