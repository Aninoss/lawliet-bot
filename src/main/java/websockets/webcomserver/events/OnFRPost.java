package websockets.webcomserver.events;

import core.EmbedFactory;
import core.DiscordApiManager;
import mysql.modules.featurerequests.DBFeatureRequests;
import websockets.webcomserver.EventAbstract;
import websockets.webcomserver.WebComServer;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.json.JSONObject;

public class OnFRPost extends EventAbstract {

    public OnFRPost(WebComServer webComServer, String event) {
        super(webComServer, event);
    }

    @Override
    protected JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception {
        long userId = requestJSON.getLong("user_id");
        String title = requestJSON.getString("title");
        String desc = requestJSON.getString("description");
        boolean notify = requestJSON.getBoolean("notify");

        DBFeatureRequests.postFeatureRequest(userId, title, desc);

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(title)
                .setAuthor("FEATURE REQUEST")
                .setDescription(desc);
        if (notify) eb.setFooter("Notify: " + userId);
        DiscordApiManager.getInstance().fetchOwner().get().sendMessage(eb).get();

        return new JSONObject();
    }

}
