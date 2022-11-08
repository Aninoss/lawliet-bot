package events.discordevents.guildvoiceupdate;

import core.MemberCacheController;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceUpdateAbstract;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildVoiceUpdatePruneCache extends GuildVoiceUpdateAbstract {

    @Override
    public boolean onGuildVoiceUpdate(GuildVoiceUpdateEvent event)  {
        if (event.getChannelLeft() != null) {
            MemberCacheController.getInstance().cacheGuildIfNotExist(event.getGuild());
        }
        return true;
    }

}
