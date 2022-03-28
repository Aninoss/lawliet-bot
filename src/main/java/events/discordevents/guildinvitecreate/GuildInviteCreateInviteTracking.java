package events.discordevents.guildinvitecreate;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildInviteCreateAbstract;
import mysql.modules.invitetracking.DBInviteTracking;
import mysql.modules.invitetracking.GuildInvite;
import mysql.modules.invitetracking.InviteTrackingData;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;

@DiscordEvent
public class GuildInviteCreateInviteTracking extends GuildInviteCreateAbstract {

    @Override
    public boolean onGuildInviteCreate(GuildInviteCreateEvent event) {
        InviteTrackingData inviteTrackingData = DBInviteTracking.getInstance().retrieve(event.getGuild().getIdLong());
        if (inviteTrackingData.isActive()) {
            GuildInvite guildInvite = new GuildInvite(event.getGuild().getIdLong(), event.getCode(), event.getInvite().getInviter().getIdLong(), 0);
            inviteTrackingData.getGuildInvites().put(guildInvite.getCode(), guildInvite);
        }
        return true;
    }

}
