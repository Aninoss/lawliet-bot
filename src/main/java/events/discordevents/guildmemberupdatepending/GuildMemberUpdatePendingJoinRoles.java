package events.discordevents.guildmemberupdatepending;

import core.ExceptionLogger;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberUpdatePendingAbstract;
import modules.JoinRoles;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.GuildEntity;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberUpdatePendingJoinRoles extends GuildMemberUpdatePendingAbstract {

    @Override
    public boolean onGuildMemberUpdatePending(GuildMemberUpdatePendingEvent event, EntityManagerWrapper entityManager) throws Throwable {
        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        JoinRoles.process(event.getMember(), false, guildEntity)
                .exceptionally(ExceptionLogger.get());
        return true;
    }

}
