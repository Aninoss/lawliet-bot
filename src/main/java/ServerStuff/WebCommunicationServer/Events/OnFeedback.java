package ServerStuff.WebCommunicationServer.Events;

import CommandSupporters.CommandLogger.CommandLogger;
import Core.DiscordApiCollection;
import Core.EmbedFactory;
import Core.ExceptionHandler;
import ServerStuff.WebCommunicationServer.WebComServer;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class OnFeedback implements DataListener<JSONObject> {

    final static Logger LOGGER = LoggerFactory.getLogger(OnFeedback.class);

    @Override
    public void onData(SocketIOClient socketIOClient, JSONObject jsonObject, AckRequest ackRequest) throws Exception {
        String cause = jsonObject.getString("cause");
        String reason = jsonObject.getString("reason");
        Optional<String> usernameDiscriminatedOpt = Optional.ofNullable(
                jsonObject.has("username_discriminated") ? jsonObject.getString("username_discriminated") : null
        );
        Optional<Long> serverIdOpt = Optional.ofNullable(
                jsonObject.has("server_id") ? jsonObject.getLong("server_id") : null
        );

        LOGGER.info("New Feedback! ### " + cause + " ###\n" + reason);

        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setTitle(cause)
                .setDescription(reason);
        usernameDiscriminatedOpt.ifPresent(eb::setAuthor);
        serverIdOpt.ifPresent(serverId -> {
            try {
                CommandLogger.getInstance().saveLog(serverId);
            } catch (IOException e) {
                LOGGER.error("Could not save log", e);
            }
            eb.setFooter(String.valueOf(serverId));
        });

        DiscordApiCollection.getInstance().getOwner().sendMessage(eb).get();

        //Send data
        socketIOClient.sendEvent(WebComServer.EVENT_FEEDBACK);
    }

}
