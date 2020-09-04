package Events.DiscordEvents.MessageCreate;

import Events.DiscordEvents.DiscordEvent;
import Events.DiscordEvents.EventPriority;
import Events.DiscordEvents.EventTypeAbstracts.MessageCreateAbstract;
import Modules.LinkCheck;
import org.javacord.api.event.message.MessageCreateEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageCreateLinkCheck extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        return LinkCheck.check(event.getMessage());
    }

}
