package ServerStuff.WebCommunicationServer.Events;

import General.DiscordApiCollection;
import General.EmbedFactory;
import General.ExceptionHandler;
import MySQL.DBUser;
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
        String reason = jsonObject.getString("reason");
        String explanation = jsonObject.getString("explanation");

        ExceptionHandler.showInfoLog("New Feedback! ### " + reason + " ###\n" + explanation);

        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setTitle(reason)
                .setDescription(explanation);

        DiscordApiCollection.getInstance().getOwner().sendMessage(eb).get();
    }

}
