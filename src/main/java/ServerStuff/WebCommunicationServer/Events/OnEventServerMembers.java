package ServerStuff.WebCommunicationServer.Events;

import General.DiscordApiCollection;
import General.Tools;
import ServerStuff.WebCommunicationServer.WebComServer;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Optional;

public class OnEventServerMembers implements DataListener<JSONObject> {

    private WebComServer webComServer;

    public OnEventServerMembers(WebComServer webComServer) {
        this.webComServer = webComServer;
    }

    @Override
    public void onData(SocketIOClient socketIOClient, JSONObject jsonObject, AckRequest ackRequest) throws Exception {
        long userId = jsonObject.getLong("user_id");
        long serverId = jsonObject.getLong("server_id");
        Optional<User> userOptional = DiscordApiCollection.getInstance().getUserById(userId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            JSONObject mainJSON = new JSONObject()
                    .put("success", false)
                    .put("user_id", userId);

            Optional<Server> serverOptional = DiscordApiCollection.getInstance().getServerById(serverId);
            if (serverOptional.isPresent()) {
                Server server = serverOptional.get();
                if (Tools.userHasAdminPermissions(server, user)) {
                    mainJSON
                            .put("success", true)
                            .put("members_online", server.getMembers().stream().filter(userCheck -> userCheck.getStatus() != UserStatus.OFFLINE).count())
                            .put("members_total", server.getMembers().size());
                }
            }

            socketIOClient.sendEvent(WebComServer.EVENT_SERVERMEMBERS, mainJSON.toString());
        }
    }

}
