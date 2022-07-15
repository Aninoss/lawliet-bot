package events.sync.events;

import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import mysql.modules.bannedusers.DBBannedUsers;
import org.json.JSONObject;

@SyncServerEvent(event = "USER_CHECK_BANNED")
public class OnUserCheckBanned implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");

        JSONObject responseJson = new JSONObject();
        responseJson.put("banned", DBBannedUsers.getInstance().retrieve().getUserIds().contains(userId));
        return responseJson;
    }

}
