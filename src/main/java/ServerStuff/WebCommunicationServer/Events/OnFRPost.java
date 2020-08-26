package ServerStuff.WebCommunicationServer.Events;

import Core.DiscordApiCollection;
import Core.EmbedFactory;
import MySQL.Modules.FeatureRequests.DBFeatureRequests;
import ServerStuff.WebCommunicationServer.EventAbstract;
import ServerStuff.WebCommunicationServer.WebComServer;
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

        DBFeatureRequests.postFeatureRequest(userId, title, desc);

        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setTitle(title)
                .setAuthor("FEATURE REQUEST")
                .setDescription(desc);
        DiscordApiCollection.getInstance().getOwner().sendMessage(eb).get();

        return new JSONObject();
    }

}
