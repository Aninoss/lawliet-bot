package DiscordEvents.MessageCreate;

import DiscordEvents.*;
import DiscordEvents.EventTypeAbstracts.MessageCreateAbstract;
import Modules.SPCheck;
import org.javacord.api.event.message.MessageCreateEvent;

@DiscordEventAnnotation(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageCreateSPCheck extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        return SPCheck.check(event.getMessage());
    }

}
