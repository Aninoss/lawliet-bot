package events.discordevents.guildmemberjoin;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.JoinRoles;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberJoinRoles extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event) throws Throwable {
        JoinRoles.process(event.getMember());
        return true;
    }

}
