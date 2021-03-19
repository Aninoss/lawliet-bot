package events.discordevents.guildmemberjoin;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.Fishery;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent
public class GuildMemberJoinFisheryRoles extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event) throws Throwable {
        if (!event.getMember().isPending()) {
            Fishery.giveRoles(event.getMember());
        }

        return true;
    }

}
