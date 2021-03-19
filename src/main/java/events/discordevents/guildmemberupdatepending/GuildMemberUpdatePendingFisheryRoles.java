package events.discordevents.guildmemberupdatepending;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberUpdatePendingAbstract;
import modules.Fishery;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent;

@DiscordEvent
public class GuildMemberUpdatePendingFisheryRoles extends GuildMemberUpdatePendingAbstract {

    @Override
    public boolean onGuildMemberUpdatePending(GuildMemberUpdatePendingEvent event) throws Throwable {
        if (!event.getMember().isPending()) {
            Fishery.giveRoles(event.getMember());
        }

        return true;
    }

}
