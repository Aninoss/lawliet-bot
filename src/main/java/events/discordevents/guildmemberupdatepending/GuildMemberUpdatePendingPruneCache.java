package events.discordevents.guildmemberupdatepending;

import core.MemberCacheController;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberUpdatePendingAbstract;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberUpdatePendingPruneCache extends GuildMemberUpdatePendingAbstract {

    @Override
    public boolean onGuildMemberUpdatePending(GuildMemberUpdatePendingEvent event) throws Throwable {
        MemberCacheController.getInstance().cacheGuildIfNotExist(event.getGuild());
        return true;
    }

}
