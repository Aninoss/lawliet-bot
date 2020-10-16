package websockets.webcomserver.events;

import core.DiscordApiCollection;
import core.EmbedFactory;
import websockets.webcomserver.EventAbstract;
import websockets.webcomserver.WebComServer;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class OnFeedback extends EventAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnFeedback.class);

    public OnFeedback(WebComServer webComServer, String event) {
        super(webComServer, event);
    }

    @Override
    protected JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception {
        String cause = requestJSON.getString("cause");
        String reason = requestJSON.getString("reason");
        Optional<Long> serverIdOpt = Optional.ofNullable(
                requestJSON.has("server_id") ? requestJSON.getLong("server_id") : null
        );

        LOGGER.info("New Feedback! ### " + cause + " ###\n" + reason);

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(cause)
                .setAuthor("BOT KICK FEEDBACK")
                .setDescription(reason);

        DiscordApiCollection.getInstance().getOwner().sendMessage(eb).get();

        return new JSONObject();
    }

}