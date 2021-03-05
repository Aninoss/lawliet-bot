package events.discordevents.guildmessagereceived;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.LinkFilter;
import org.javacord.api.event.message.MessageCreateEvent;

@DiscordEvent(priority = EventPriority.MEDIUM, allowBannedUser = true)
public class GuildMessageReceivedAnicordLinkFilter extends GuildMessageReceivedAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        return LinkFilter.check(event.getMessage());
    }

}
