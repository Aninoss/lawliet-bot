package websockets.webcomserver.events;

import core.DiscordApiManager;
import core.utils.PermissionUtil;
import mysql.modules.bannedusers.DBBannedUsers;
import websockets.webcomserver.EventAbstract;
import websockets.webcomserver.WebComServer;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Optional;

public class OnEventServerList extends EventAbstract {

    public OnEventServerList(WebComServer webComServer, String event) {
        super(webComServer, event);
    }

    @Override
    protected JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception {
        long userId = requestJSON.getLong("user_id");
        if (DBBannedUsers.getInstance().getBean().getUserIds().contains(userId))
            return null;

        Optional<User> userOptional = DiscordApiManager.getInstance().fetchUserById(userId).get();

        JSONObject responseJSON = new JSONObject();
        JSONArray serversArray = new JSONArray();

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            ArrayList<Server> mutualServers = new ArrayList<>(DiscordApiManager.getInstance().getLocalMutualServers(user));
            mutualServers.sort((s1, s2) -> s1.getName().compareToIgnoreCase(s2.getName()));
            for (Server server : mutualServers) {
                if (PermissionUtil.hasAdminPermissions(server, user)) {
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
        responseJSON.put("user_id", userId)
                .put("server_list", serversArray);

        return responseJSON;
    }

}