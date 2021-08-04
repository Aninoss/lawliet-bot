package events.discordevents.guildvoiceleave;

import core.MemberCacheController;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceLeaveAbstract;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildVoiceLeavePruneCache extends GuildVoiceLeaveAbstract {

    @Override
    public boolean onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        MemberCacheController.getInstance().cacheGuildIfNotExist(event.getGuild());
        return true;
    }

}
