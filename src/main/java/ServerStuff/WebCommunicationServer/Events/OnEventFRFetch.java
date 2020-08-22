package ServerStuff.WebCommunicationServer.Events;

import Core.PatreonCache;
import MySQL.Modules.BannedUsers.DBBannedUsers;
import ServerStuff.WebCommunicationServer.EventAbstract;
import ServerStuff.WebCommunicationServer.WebComServer;
import org.json.JSONObject;

public class OnEventFRFetch extends EventAbstract {

    public OnEventFRFetch(WebComServer webComServer, String event) {
        super(webComServer, event);
    }

    @Override
    protected JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception {
        JSONObject responseJSON = new JSONObject();
        responseJSON.put("boosts_total", 0);

        if (requestJSON.has("user_id")) {
            long userId = requestJSON.getLong("user_id");
            if (DBBannedUsers.getInstance().getBean().getUserIds().contains(userId))
                return null;

            int boostsTotal = PatreonCache.getInstance().getPatreonLevel(userId);
            responseJSON.put("boosts_total", boostsTotal);
        }

        return responseJSON;
    }

}
