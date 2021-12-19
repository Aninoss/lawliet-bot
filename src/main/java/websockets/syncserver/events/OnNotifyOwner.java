package websockets.syncserver.events;

import java.util.concurrent.TimeUnit;
import core.EmbedFactory;
import core.utils.JDAUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "NOTIFY")
public class OnNotifyOwner implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        int delay = jsonObject.getInt("delay");
        EmbedBuilder eb = EmbedFactory.getEmbedDefault();

        if (jsonObject.has("title")) eb.setTitle(jsonObject.getString("title"));
        if (jsonObject.has("description")) eb.setDescription(jsonObject.getString("description"));
        if (jsonObject.has("author")) eb.setAuthor(jsonObject.getString("author"));
        if (jsonObject.has("thumbnail")) eb.setThumbnail(jsonObject.getString("thumbnail"));
        if (jsonObject.has("image")) eb.setImage(jsonObject.getString("image"));
        if (jsonObject.has("footer")) eb.setFooter(jsonObject.getString("footer"));

        JDAUtil.sendPrivateMessage(userId, eb.build()).queueAfter(delay, TimeUnit.MILLISECONDS);
        return null;
    }

}
