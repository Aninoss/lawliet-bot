package events.discordevents.guildinvitedelete;

import core.schedule.MainScheduler;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildInviteDeleteAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.modules.invitetracking.DBInviteTracking;
import mysql.modules.invitetracking.InviteTrackingData;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;

import java.time.Duration;

@DiscordEvent
public class GuildInviteDeleteInviteTracking extends GuildInviteDeleteAbstract {

    @Override
    public boolean onGuildInviteDelete(GuildInviteDeleteEvent event, EntityManagerWrapper entityManager) {
        InviteTrackingData inviteTrackingData = DBInviteTracking.getInstance().retrieve(event.getGuild().getIdLong());
        if (inviteTrackingData.isActive()) {
            MainScheduler.schedule(Duration.ofSeconds(1),
                    () -> inviteTrackingData.getGuildInvites().remove(event.getCode())
            );
        }
        return true;
    }

}
