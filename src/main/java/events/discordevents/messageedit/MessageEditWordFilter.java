package events.discordevents.messageedit;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.MessageEditAbstract;
import modules.automod.WordFilter;
import org.javacord.api.event.message.MessageEditEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageEditWordFilter extends MessageEditAbstract {

    @Override
    public boolean onMessageEdit(MessageEditEvent event) throws Throwable {
        return new WordFilter(event.getMessage().get()).check();
    }

}
