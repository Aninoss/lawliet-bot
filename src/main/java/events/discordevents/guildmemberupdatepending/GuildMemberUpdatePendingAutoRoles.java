package events.discordevents.guildmemberupdatepending;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberUpdatePendingAbstract;
import modules.AutoRoles;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent;

@DiscordEvent(allowBots = true)
public class GuildMemberUpdatePendingAutoRoles extends GuildMemberUpdatePendingAbstract {

    @Override
    public boolean onGuildMemberUpdatePending(GuildMemberUpdatePendingEvent event) throws Throwable {
        if (!event.getMember().isPending()) {
            AutoRoles.giveRoles(event.getMember());
        }

        return true;
    }

}
