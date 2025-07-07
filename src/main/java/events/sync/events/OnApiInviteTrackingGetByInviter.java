package events.sync.events;

import core.MemberCacheController;
import events.sync.InviteTrackingApiEvent;
import events.sync.SyncServerEvent;
import events.sync.apidata.v1.Invite;
import mysql.modules.invitetracking.DBInviteTracking;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;

@SyncServerEvent(event = "API_INVITE_TRACKING_GET_BY_INVITER")
public class OnApiInviteTrackingGetByInviter extends InviteTrackingApiEvent {

    @Override
    public JSONObject apply(JSONObject requestJson, JSONObject responseJSON, Guild guild) {
        long userId = requestJson.getLong("user_id");
        int page = requestJson.getInt("page");
        int size = Math.max(1, Math.min(100, requestJson.getInt("size")));

        MemberCacheController.getInstance().loadMembersFull(guild).join();
        List<Invite> invites = DBInviteTracking.getInstance().retrieve(guild.getIdLong()).getInviteTrackingSlots().values().stream()
                .filter(slot -> slot.getInviterUserId() == userId)
                .skip((long) page * size)
                .limit(size)
                .map(this::mapToApiInvite)
                .collect(Collectors.toList());

        responseJSON.put("objects", writeListAsJson(invites));
        return responseJSON;
    }

}
