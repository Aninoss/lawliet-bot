package events.discordevents.messageedit;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.MessageEditAbstract;
import modules.automod.InviteFilter;
import org.javacord.api.event.message.MessageEditEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageEditInviteFilter extends MessageEditAbstract {

    @Override
    public boolean onMessageEdit(MessageEditEvent event) throws Throwable {
        return new InviteFilter(event.getMessage().get()).check();
    }

}
