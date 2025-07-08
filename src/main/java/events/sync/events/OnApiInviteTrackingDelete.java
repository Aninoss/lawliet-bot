package events.sync.events;

import core.CustomObservableMap;
import events.sync.InviteTrackingApiEvent;
import events.sync.SyncServerEvent;
import mysql.modules.invitetracking.DBInviteTracking;
import mysql.modules.invitetracking.InviteTrackingSlot;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;

@SyncServerEvent(event = "API_INVITE_TRACKING_DELETE")
public class OnApiInviteTrackingDelete extends InviteTrackingApiEvent {

    @Override
    public JSONObject apply(JSONObject requestJson, JSONObject responseJSON, Guild guild) {
        long userId = requestJson.getLong("user_id");
        CustomObservableMap<Long, InviteTrackingSlot> inviteTrackingSlots = DBInviteTracking.getInstance().retrieve(guild.getIdLong()).getInviteTrackingSlots();
        InviteTrackingSlot slot = inviteTrackingSlots.get(userId);
        if (slot == null) {
            return responseJSON;
        }

        inviteTrackingSlots.remove(userId);
        responseJSON.put("found", true);
        return responseJSON;
    }

}
