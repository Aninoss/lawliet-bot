package events.discordevents.messagecreate;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.MessageCreateAbstract;
import modules.LinkCheck;
import org.javacord.api.event.message.MessageCreateEvent;

@DiscordEvent(priority = EventPriority.MEDIUM, allowBannedUser = true)
public class MessageCreateSpamCheck extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        return LinkCheck.check(event.getMessage());
    }

}
