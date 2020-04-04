package ServerStuff.WebCommunicationServer.Events;

import General.DiscordApiCollection;
import General.PermissionCheck;
import ServerStuff.WebCommunicationServer.WebComServer;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Optional;

public class OnEventServerList implements DataListener<JSONObject> {

    @Override
    public void onData(SocketIOClient socketIOClient, JSONObject jsonObject, AckRequest ackRequest) throws Exception {
        long userId = jsonObject.getLong("user_id");
        Optional<User> userOptional = DiscordApiCollection.getInstance().getUserById(userId);

        JSONObject mainJSON = new JSONObject();
        JSONArray serversArray = new JSONArray();

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            ArrayList<Server> mutualServers = DiscordApiCollection.getInstance().getMutualServers(user);
            mutualServers.sort((s1, s2) -> s1.getName().compareToIgnoreCase(s2.getName()));
            for(Server server: mutualServers) {
                if (PermissionCheck.hasAdminPermissions(server, user)) {
                    JSONObject serverObject = new JSONObject();
                    serverObject
                            .put("server_id", server.getId())
                            .put("name", server.getName());

                    if (server.getIcon().isPresent())
                        serverObject.put("icon", server.getIcon().get().getUrl().toString());

                    serversArray.put(serverObject);
                }
            }
        }

        //Send data
        mainJSON.put("user_id", userId)
                .put("server_list", serversArray);
        socketIOClient.sendEvent(WebComServer.EVENT_SERVERLIST, mainJSON.toString());
    }

}
