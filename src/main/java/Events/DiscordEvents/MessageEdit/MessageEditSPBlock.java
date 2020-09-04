package Events.DiscordEvents.MessageEdit;

import Events.DiscordEvents.DiscordEvent;
import Events.DiscordEvents.EventPriority;
import Events.DiscordEvents.EventTypeAbstracts.MessageEditAbstract;
import Modules.SPCheck;
import org.javacord.api.event.message.MessageEditEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageEditSPBlock extends MessageEditAbstract {

    @Override
    public boolean onMessageEdit(MessageEditEvent event) throws Throwable {
        return SPCheck.check(event.getMessage().get());
    }

}
