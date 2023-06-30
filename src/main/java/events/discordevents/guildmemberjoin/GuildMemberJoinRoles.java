package events.discordevents.guildmemberjoin;

import core.ExceptionLogger;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.JoinRoles;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberJoinRoles extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event, EntityManagerWrapper entityManager) throws Throwable {
        JoinRoles.process(event.getMember(), false)
                .exceptionally(ExceptionLogger.get());
        return true;
    }

}
