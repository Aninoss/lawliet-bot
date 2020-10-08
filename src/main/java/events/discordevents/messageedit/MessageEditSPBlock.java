package events.discordevents.messageedit;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.MessageEditAbstract;
import modules.InviteFilterCheck;
import org.javacord.api.event.message.MessageEditEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageEditSPBlock extends MessageEditAbstract {

    @Override
    public boolean onMessageEdit(MessageEditEvent event) throws Throwable {
        return InviteFilterCheck.check(event.getMessage().get());
    }

}
