package DiscordEvents.MessageEdit;

import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventPriority;
import DiscordEvents.EventTypeAbstracts.MessageEditAbstract;
import Modules.LinkCheck;
import org.javacord.api.event.message.MessageEditEvent;

@DiscordEventAnnotation(priority = EventPriority.HIGH)
public class MessageEditLinkCheck extends MessageEditAbstract {

    @Override
    public boolean onMessageEdit(MessageEditEvent event) throws Throwable {
        return LinkCheck.check(event.getMessage().get());
    }

}
