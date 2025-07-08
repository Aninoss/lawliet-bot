package events.sync.events;

import events.sync.FisheryApiEvent;
import events.sync.SyncServerEvent;
import mysql.redis.fisheryusers.FisheryGuildData;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;

@SyncServerEvent(event = "API_FISHERY_RESET_USER")
public class OnApiFisheryResetUser extends FisheryApiEvent {

    @Override
    public JSONObject apply(JSONObject requestJson, JSONObject responseJSON, Guild guild) {
        long userId = requestJson.getLong("user_id");
        FisheryGuildData fisheryGuildData = FisheryUserManager.getGuildData(guild.getIdLong());
        FisheryMemberData fisheryMemberData = fisheryGuildData.getMemberData(userId);
        if (!fisheryMemberData.exists()) {
            return responseJSON;
        }

        fisheryMemberData.remove();
        responseJSON.put("found", true);
        return responseJSON;
    }

}
