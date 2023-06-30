package events.discordevents.guildmemberupdatepending;

import core.ExceptionLogger;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberUpdatePendingAbstract;
import modules.JoinRoles;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberUpdatePendingJoinRoles extends GuildMemberUpdatePendingAbstract {

    @Override
    public boolean onGuildMemberUpdatePending(GuildMemberUpdatePendingEvent event, EntityManagerWrapper entityManager) throws Throwable {
        JoinRoles.process(event.getMember(), false)
                .exceptionally(ExceptionLogger.get());
        return true;
    }

}
