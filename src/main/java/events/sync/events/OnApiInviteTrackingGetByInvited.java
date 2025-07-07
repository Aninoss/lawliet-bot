package events.sync.events;

import core.MemberCacheController;
import events.sync.InviteTrackingApiEvent;
import events.sync.SyncServerEvent;
import mysql.modules.invitetracking.DBInviteTracking;
import mysql.modules.invitetracking.InviteTrackingSlot;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;

@SyncServerEvent(event = "API_INVITE_TRACKING_GET_BY_INVITED")
public class OnApiInviteTrackingGetByInvited extends InviteTrackingApiEvent {

    @Override
    public JSONObject apply(JSONObject requestJson, JSONObject responseJSON, Guild guild) {
        long userId = requestJson.getLong("user_id");
        InviteTrackingSlot slot = DBInviteTracking.getInstance().retrieve(guild.getIdLong()).getInviteTrackingSlots().get(userId);
        if (slot == null) {
            return responseJSON;
        }

        MemberCacheController.getInstance().loadMember(guild, userId);
        responseJSON.put("object", writeObjectAsJson(mapToApiInvite(slot)));
        return responseJSON;
    }

}
