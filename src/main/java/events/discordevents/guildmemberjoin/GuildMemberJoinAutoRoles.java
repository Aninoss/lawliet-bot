package events.discordevents.guildmemberjoin;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.AutoRoles;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent(allowBots = true)
public class GuildMemberJoinAutoRoles extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event) throws Throwable {
        if (!event.getMember().isPending()) {
            AutoRoles.giveRoles(event.getMember());
        }

        return true;
    }

}
