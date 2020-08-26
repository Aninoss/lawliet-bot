package ServerStuff.WebCommunicationServer.Events;

import CommandSupporters.CommandLogger.CommandLogger;
import Core.DiscordApiCollection;
import Core.EmbedFactory;
import ServerStuff.WebCommunicationServer.EventAbstract;
import ServerStuff.WebCommunicationServer.WebComServer;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setTitle(cause)
                .setAuthor("BOT KICK FEEDBACK")
                .setDescription(reason);
        serverIdOpt.ifPresent(serverId -> {
            try {
                CommandLogger.getInstance().saveLog(serverId, false);
            } catch (IOException e) {
                LOGGER.error("Could not save log", e);
            }
            eb.setFooter(String.valueOf(serverId));
        });

        DiscordApiCollection.getInstance().getOwner().sendMessage(eb).get();

        return new JSONObject();
    }

}