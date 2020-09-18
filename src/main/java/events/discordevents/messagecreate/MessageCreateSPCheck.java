package events.discordevents.messagecreate;

import events.discordevents.*;
import events.discordevents.eventtypeabstracts.MessageCreateAbstract;
import modules.SPCheck;
import org.javacord.api.event.message.MessageCreateEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageCreateSPCheck extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        return SPCheck.check(event.getMessage());
    }

}
