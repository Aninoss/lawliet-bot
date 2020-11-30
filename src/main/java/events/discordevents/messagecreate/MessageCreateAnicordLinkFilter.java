package events.discordevents.messagecreate;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.MessageCreateAbstract;
import modules.LinkFilter;
import org.javacord.api.event.message.MessageCreateEvent;

@DiscordEvent(priority = EventPriority.MEDIUM, allowBannedUser = true)
public class MessageCreateAnicordLinkFilter extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        return LinkFilter.check(event.getMessage());
    }

}
