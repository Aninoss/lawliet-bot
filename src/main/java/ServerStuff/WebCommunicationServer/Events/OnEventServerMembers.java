package ServerStuff.WebCommunicationServer.Events;

import Core.DiscordApiCollection;
import Core.Utils.PermissionUtil;
import MySQL.Modules.BannedUsers.DBBannedUsers;
import ServerStuff.WebCommunicationServer.EventAbstract;
import ServerStuff.WebCommunicationServer.WebComServer;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;
import org.json.JSONObject;

import java.util.Optional;

public class OnEventServerMembers extends EventAbstract {

    public OnEventServerMembers(WebComServer webComServer, String event) {
        super(webComServer, event);
    }

    @Override
    protected JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception {
        long userId = requestJSON.getLong("user_id");
        if (DBBannedUsers.getInstance().getBean().getUserIds().contains(userId))
            return null;

        long serverId = requestJSON.getLong("server_id");
        Optional<User> userOptional = DiscordApiCollection.getInstance().getUserById(userId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            Optional<Server> serverOptional = DiscordApiCollection.getInstance().getServerById(serverId);
            if (serverOptional.isPresent()) {
                Server server = serverOptional.get();
                if (PermissionUtil.hasAdminPermissions(server, user)) {
                    return new JSONObject()
                            .put("user_id", userId)
                            .put("members_online", server.getMembers().stream().filter(userCheck -> userCheck.getStatus() != UserStatus.OFFLINE).count())
                            .put("members_total", server.getMemberCount());
                }
            }
        }

        return null;
    }
}

/*public class OnEventServerMembers implements DataListener<JSONObject> {

    @Override
    public void onData(SocketIOClient socketIOClient, JSONObject jsonObject, AckRequest ackRequest) throws Exception {
        long userId = jsonObject.getLong("user_id");
        if (DBBannedUsers.getInstance().getBean().getUserIds().contains(userId)) return;
        long serverId = jsonObject.getLong("server_id");
        Optional<User> userOptional = DiscordApiCollection.getInstance().getUserById(userId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            Optional<Server> serverOptional = DiscordApiCollection.getInstance().getServerById(serverId);
            if (serverOptional.isPresent()) {
                Server server = serverOptional.get();
                if (PermissionUtil.hasAdminPermissions(server, user)) {
                    JSONObject mainJSON = new JSONObject()
                            .put("user_id", userId)
                            .put("members_online", server.getMembers().stream().filter(userCheck -> userCheck.getStatus() != UserStatus.OFFLINE).count())
                            .put("members_total", server.getMemberCount());

                    //Send Data
                    socketIOClient.sendEvent(WebComServer.EVENT_SERVERMEMBERS, mainJSON.toString());
                }
            }
        }
    }

}*/
