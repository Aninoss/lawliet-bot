package websockets.syncserver.events;

import core.ShardManager;
import core.EmbedFactory;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.util.logging.ExceptionLogger;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "NOTIFY")
public class OnNotifyOwner implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        EmbedBuilder eb = EmbedFactory.getEmbedDefault();

        if (jsonObject.has("title"))
            eb.setTitle(jsonObject.getString("title"));
        if (jsonObject.has("description"))
            eb.setDescription(jsonObject.getString("description"));
        if (jsonObject.has("author"))
            eb.setAuthor(jsonObject.getString("author"));
        if (jsonObject.has("thumbnail"))
            eb.setThumbnail(jsonObject.getString("thumbnail"));
        if (jsonObject.has("image"))
            eb.setImage(jsonObject.getString("image"));
        if (jsonObject.has("footer"))
            eb.setFooter(jsonObject.getString("footer"));

        ShardManager.getInstance().fetchUserById(userId)
                .exceptionally(ExceptionLogger.get())
                .thenAccept(userOpt -> {
                    userOpt.ifPresent(user -> user.sendMessage(eb)
                            .exceptionally(ExceptionLogger.get())
                    );
                });

        return null;
    }

}
