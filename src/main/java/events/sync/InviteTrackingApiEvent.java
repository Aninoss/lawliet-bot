package events.sync;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import events.sync.apidata.v1.Invite;
import mysql.modules.invitetracking.InviteTrackingSlot;

public abstract class InviteTrackingApiEvent extends ApiEvent {

    private final ObjectMapper mapper = new ObjectMapper();

    public InviteTrackingApiEvent() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected Invite mapToApiInvite(InviteTrackingSlot slot) {
        Invite invite = new Invite();
        invite.setInviterUserId(slot.getInviterUserId());
        invite.setInvitedUserId(slot.getMemberId());
        invite.setFakeInvite(slot.isFakeInvite());
        invite.setOnServer(slot.getGuild().get().getMemberById(slot.getInviterUserId()) != null);
        invite.setRetained(slot.isRetained());
        invite.setActive(slot.isActive());
        return invite;
    }

}
