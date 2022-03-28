package events.discordevents.guildinvitedelete;

import java.time.temporal.ChronoUnit;
import core.schedule.MainScheduler;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildInviteDeleteAbstract;
import mysql.modules.invitetracking.DBInviteTracking;
import mysql.modules.invitetracking.InviteTrackingData;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;

@DiscordEvent
public class GuildInviteDeleteInviteTracking extends GuildInviteDeleteAbstract {

    @Override
    public boolean onGuildInviteDelete(GuildInviteDeleteEvent event) {
        InviteTrackingData inviteTrackingData = DBInviteTracking.getInstance().retrieve(event.getGuild().getIdLong());
        if (inviteTrackingData.isActive()) {
            MainScheduler.schedule(1, ChronoUnit.SECONDS, "guild_invite_delete",
                    () -> inviteTrackingData.getGuildInvites().remove(event.getCode())
            );
        }
        return true;
    }

}
