package websockets.webcomserver.events;

import constants.FRPanelType;
import core.cache.PatreonCache;
import mysql.modules.bannedusers.DBBannedUsers;
import mysql.modules.featurerequests.DBFeatureRequests;
import org.json.JSONArray;
import org.json.JSONObject;
import websockets.webcomserver.EventAbstract;
import websockets.webcomserver.WebComServer;

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


        JSONArray jsonEntriesArray = new JSONArray();
        FRPanelType[] types = new FRPanelType[]{FRPanelType.PENDING, FRPanelType.REJECTED};
        for(FRPanelType type : types) {
            DBFeatureRequests.fetchEntries(userId, type).forEach(frEntry -> {
                JSONObject jsonEntry = new JSONObject()
                        .put("id", frEntry.getId())
                        .put("title", frEntry.getTitle())
                        .put("description", frEntry.getDescription())
                        .put("public", frEntry.isPublicEntry())
                        .put("boosts", frEntry.getBoosts())
                        .put("type", type.name())
                        .put("date", frEntry.getDate().toEpochDay());
                jsonEntriesArray.put(jsonEntry);
            });
        }

        responseJSON.put("data", jsonEntriesArray);
        return responseJSON;
    }

    public static int getBoostsTotal(long userId) throws SQLException {
        if (DBBannedUsers.getInstance().getBean().getUserIds().contains(userId))
            return 0;

        return Math.max(1, PatreonCache.getInstance().getPatreonLevel(userId));
    }

    public static int getBoostsUsed(long userId) throws SQLException {
        if (DBBannedUsers.getInstance().getBean().getUserIds().contains(userId))
            return 0;

        return DBFeatureRequests.fetchBoostsThisWeek(userId);
    }

}
