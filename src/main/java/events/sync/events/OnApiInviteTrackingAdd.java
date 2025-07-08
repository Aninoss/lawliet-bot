package events.sync.events;

import core.MemberCacheController;
import events.sync.InviteTrackingApiEvent;
import events.sync.SyncServerEvent;
import mysql.modules.invitetracking.DBInviteTracking;
import mysql.modules.invitetracking.InviteTrackingSlot;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;

import java.time.LocalDate;

@SyncServerEvent(event = "API_INVITE_TRACKING_ADD")
public class OnApiInviteTrackingAdd extends InviteTrackingApiEvent {

    @Override
    public JSONObject apply(JSONObject requestJson, JSONObject responseJSON, Guild guild) {
        long inviterUserId = requestJson.getLong("inviter_user_id");
        long invitedUserId = requestJson.getLong("invited_user_id");

        InviteTrackingSlot inviteTrackingSlot = new InviteTrackingSlot(guild.getIdLong(), invitedUserId, inviterUserId,
                LocalDate.now(), LocalDate.now(), true
        );
        DBInviteTracking.getInstance().retrieve(guild.getIdLong()).getInviteTrackingSlots()
                .put(inviteTrackingSlot.getMemberId(), inviteTrackingSlot);

        MemberCacheController.getInstance().loadMember(guild, invitedUserId);
        responseJSON.put("object", writeObjectAsJson(mapToApiInvite(inviteTrackingSlot)));
        return responseJSON;
    }

}
