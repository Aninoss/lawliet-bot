package ServerStuff.WebCommunicationServer.Events;

import Constants.FRPanelType;
import Core.PatreonCache;
import MySQL.Modules.BannedUsers.DBBannedUsers;
import MySQL.Modules.FeatureRequests.DBFeatureRequests;
import ServerStuff.WebCommunicationServer.EventAbstract;
import ServerStuff.WebCommunicationServer.WebComServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;

public class OnFRFetch extends EventAbstract {

    public OnFRFetch(WebComServer webComServer, String event) {
        super(webComServer, event);
    }

    @Override
    protected JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception {
        JSONObject responseJSON = new JSONObject();
        responseJSON.put("boosts_total", 0);
        responseJSON.put("boosts_remaining", 0);

        long userId = -1;
        if (requestJSON.has("user_id")) {
            userId = requestJSON.getLong("user_id");

            int boostsTotal = getBoostsTotal(userId);
            responseJSON.put("boosts_total", boostsTotal);

            int boostsUsed = getBoostsUsed(userId);
            if (boostsUsed != -1)
                responseJSON.put("boosts_remaining", Math.max(0, boostsTotal - boostsUsed));
        }

        for(FRPanelType type : FRPanelType.values()) {
            JSONArray jsonEntriesArray = new JSONArray();
            DBFeatureRequests.fetchEntries(userId, type).forEach(frEntry -> {
                JSONObject jsonEntry = new JSONObject();
                jsonEntry.put("id", frEntry.getId());
                jsonEntry.put("description", frEntry.getDescription());
                jsonEntry.put("public", frEntry.isPublicEntry());
                jsonEntry.put("boosts", frEntry.getBoosts());
                jsonEntriesArray.put(jsonEntry);
            });
            responseJSON.put(type.name(), jsonEntriesArray);
        }

        return responseJSON;
    }

    public static int getBoostsTotal(long userId) throws SQLException {
        if (DBBannedUsers.getInstance().getBean().getUserIds().contains(userId))
            return 0;

        return PatreonCache.getInstance().getPatreonLevel(userId);
    }

    public static int getBoostsUsed(long userId) throws SQLException {
        if (DBBannedUsers.getInstance().getBean().getUserIds().contains(userId))
            return 0;

        return DBFeatureRequests.fetchBoostsThisMonth(userId);
    }

}
