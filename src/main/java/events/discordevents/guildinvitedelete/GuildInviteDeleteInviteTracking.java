package events.discordevents.guildinvitedelete;

import core.schedule.MainScheduler;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildInviteDeleteAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;

import java.time.Duration;

@DiscordEvent
public class GuildInviteDeleteInviteTracking extends GuildInviteDeleteAbstract {

    @Override
    public boolean onGuildInviteDelete(GuildInviteDeleteEvent event, EntityManagerWrapper entityManager) {
        if (entityManager.findGuildEntity(event.getGuild().getIdLong()).getInviteTracking().getActive()) {
            MainScheduler.schedule(Duration.ofSeconds(1),
                    () -> {
                        try (GuildEntity guildEntity = HibernateManager.findGuildEntity(event.getGuild().getIdLong(), GuildInviteDeleteInviteTracking.class)) {
                            guildEntity.beginTransaction();
                            guildEntity.getGuildInvites().remove(event.getCode());
                            guildEntity.commitTransaction();
                        }
                    }
            );
        }
        return true;
    }

}
