package events.discordevents.guildinvitecreate;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildInviteCreateAbstract;
import modules.invitetracking.InviteTracking;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.GuildInviteEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.InviteTrackingEntity;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;

@DiscordEvent
public class GuildInviteCreateInviteTracking extends GuildInviteCreateAbstract {

    @Override
    public boolean onGuildInviteCreate(GuildInviteCreateEvent event, EntityManagerWrapper entityManager) {
        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        InviteTrackingEntity inviteTracking = guildEntity.getInviteTracking();
        if (inviteTracking.getActive() && event.getInvite() != null && event.getInvite().getInviter() != null) {
            GuildInviteEntity guildInvite = new GuildInviteEntity(
                    event.getInvite().getInviter().getIdLong(),
                    0,
                    InviteTracking.calculateMaxAgeOfInvite(event.getInvite())
            );
            guildEntity.beginTransaction();
            guildEntity.getGuildInvites().put(event.getCode(), guildInvite);
            guildEntity.commitTransaction();
        }
        return true;
    }

}
