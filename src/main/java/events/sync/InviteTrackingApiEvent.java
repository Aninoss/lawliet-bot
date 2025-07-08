package events.sync;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import events.sync.apidata.v1.Invite;
import events.sync.apidata.v1.InviteStats;
import modules.invitetracking.InviteMetrics;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.modules.invitetracking.DBInviteTracking;
import mysql.modules.invitetracking.InviteTrackingSlot;
import net.dv8tion.jda.api.entities.Guild;

public abstract class InviteTrackingApiEvent extends ApiEvent {

    private final ObjectMapper mapper = new ObjectMapper();

    public InviteTrackingApiEvent() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    protected boolean functionIsEnabled(Guild guild, GuildEntity guildEntity) {
        return DBInviteTracking.getInstance().retrieve(guild.getIdLong()).isActive();
    }

    protected Invite mapToApiInvite(InviteTrackingSlot slot) {
        Invite invite = new Invite();
        invite.setInviterUserId(slot.getInviterUserId());
        invite.setInvitedUserId(slot.getMemberId());
        invite.setFakeInvite(slot.isFakeInvite());
        invite.setOnServer(slot.getGuild().get().getMemberById(slot.getMemberId()) != null);
        invite.setRetained(slot.isRetained());
        invite.setActive(slot.isActive());
        return invite;
    }

    protected InviteStats mapToApiInviteStats(InviteMetrics metrics) {
        InviteStats inviteStats = new InviteStats();
        inviteStats.setInviterUserId(metrics.getMemberId());
        inviteStats.setTotal(metrics.getTotalInvites());
        inviteStats.setOnServer(metrics.getOnServer());
        inviteStats.setRetained(metrics.getRetained());
        inviteStats.setActive(metrics.getActive());
        return inviteStats;
    }

}
