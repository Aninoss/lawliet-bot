package ServerStuff.WebCommunicationServer.Events;

import General.DiscordApiCollection;
import General.EmbedFactory;
import General.ExceptionHandler;
import MySQL.DBUser;
import ServerStuff.WebCommunicationServer.WebComServer;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.Optional;

public class OnFeedback implements DataListener<JSONObject> {

    @Override
    public void onData(SocketIOClient socketIOClient, JSONObject jsonObject, AckRequest ackRequest) throws Exception {
        String cause = jsonObject.getString("cause");
        String reason = jsonObject.getString("reason");
        Optional<String> usernameDiscriminated = Optional.ofNullable(
                jsonObject.has("username_discriminated") ? jsonObject.getString("username_discriminated") : null
        );

        ExceptionHandler.showInfoLog("New Feedback! ### " + cause + " ###\n" + reason);

        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setTitle(cause)
                .setDescription(reason);
        usernameDiscriminated.ifPresent(eb::setAuthor);

        DiscordApiCollection.getInstance().getOwner().sendMessage(eb).get();

        //Send data
        socketIOClient.sendEvent(WebComServer.EVENT_FEEDBACK);
    }

}
