package Events.DiscordEvents.MessageCreate;

import Events.DiscordEvents.*;
import Events.DiscordEvents.EventTypeAbstracts.MessageCreateAbstract;
import Modules.SPCheck;
import org.javacord.api.event.message.MessageCreateEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageCreateSPCheck extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        return SPCheck.check(event.getMessage());
    }

}
