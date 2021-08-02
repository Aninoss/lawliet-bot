package events.discordevents.guildmemberupdatepending;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberUpdatePendingAbstract;
import modules.JoinRoles;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberUpdatePendingJoinRoles extends GuildMemberUpdatePendingAbstract {

    @Override
    public boolean onGuildMemberUpdatePending(GuildMemberUpdatePendingEvent event) throws Throwable {
        JoinRoles.process(event.getMember());
        return true;
    }

}
