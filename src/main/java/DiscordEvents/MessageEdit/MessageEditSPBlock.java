package DiscordEvents.MessageEdit;

import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventPriority;
import DiscordEvents.EventTypeAbstracts.MessageEditAbstract;
import Modules.SPCheck;
import org.javacord.api.event.message.MessageEditEvent;

@DiscordEventAnnotation(priority = EventPriority.HIGH)
public class MessageEditSPBlock extends MessageEditAbstract {

    @Override
    public boolean onMessageEdit(MessageEditEvent event) throws Throwable {
        return SPCheck.check(event.getMessage().get());
    }

}
