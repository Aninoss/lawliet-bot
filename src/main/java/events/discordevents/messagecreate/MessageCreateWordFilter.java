package events.discordevents.messagecreate;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.MessageCreateAbstract;
import modules.automod.WordFilter;
import org.javacord.api.event.message.MessageCreateEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageCreateWordFilter extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        return new WordFilter(event.getMessage()).check();
    }

}