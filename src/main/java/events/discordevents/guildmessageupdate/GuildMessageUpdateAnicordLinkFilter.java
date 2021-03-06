package events.discordevents.guildmessageupdate;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageUpdateAbstract;
import modules.LinkFilter;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class GuildMessageUpdateAnicordLinkFilter extends GuildMessageUpdateAbstract {

    @Override
    public boolean onGuildMessageUpdate(GuildMessageUpdateEvent event) {
        return LinkFilter.check(event.getMessage());
    }

}
