package events.sync.events;

import events.sync.InviteTrackingApiEvent;
import events.sync.SyncServerEvent;
import modules.invitetracking.InviteMetrics;
import modules.invitetracking.InviteTracking;
import mysql.modules.invitetracking.DBInviteTracking;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;

@SyncServerEvent(event = "API_INVITE_TRACKING_STATS_GET")
public class OnApiInviteTrackingStatsGet extends InviteTrackingApiEvent {

    @Override
    public JSONObject apply(JSONObject requestJson, JSONObject responseJSON, Guild guild) {
        long userId = requestJson.getLong("user_id");
        boolean found = DBInviteTracking.getInstance().retrieve(guild.getIdLong()).getInviteTrackingSlots().values().stream()
                .anyMatch(slot -> slot.getInviterUserId() == userId);
        if (!found) {
            return responseJSON;
        }

        InviteMetrics inviteMetrics = InviteTracking.generateInviteMetricsForInviterUser(guild, userId);
        responseJSON.put("object", writeObjectAsJson(mapToApiInviteStats(inviteMetrics)));
        return responseJSON;
    }

}
