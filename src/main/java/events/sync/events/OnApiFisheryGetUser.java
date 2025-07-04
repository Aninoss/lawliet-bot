package events.sync.events;

import events.sync.FisheryApiEvent;
import events.sync.SyncServerEvent;
import events.sync.apidata.v1.FisheryUser;
import mysql.redis.fisheryusers.FisheryGuildData;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import org.json.JSONObject;

@SyncServerEvent(event = "API_FISHERY_GET_USER")
public class OnApiFisheryGetUser extends FisheryApiEvent {

    @Override
    public JSONObject apply(JSONObject requestJson) {
        long guildId = requestJson.getLong("guild_id");
        long userId = requestJson.getLong("user_id");

        JSONObject responseJSON = new JSONObject();
        if (authIsInvalid(requestJson, responseJSON)) {
            return responseJSON;
        }

        FisheryGuildData fisheryGuildData = FisheryUserManager.getGuildData(guildId);
        FisheryMemberData fisheryMemberData = fisheryGuildData.getMemberData(userId);
        if (!fisheryMemberData.exists()) {
            return responseJSON;
        }

        FisheryUser fisheryUser = mapToApiUser(fisheryGuildData, fisheryMemberData);
        responseJSON.put("object", writeObjectAsJson(fisheryUser));
        return responseJSON;
    }

}
