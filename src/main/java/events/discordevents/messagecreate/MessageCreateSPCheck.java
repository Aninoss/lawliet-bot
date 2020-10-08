package events.discordevents.messagecreate;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.MessageCreateAbstract;
import modules.InviteFilterCheck;
import org.javacord.api.event.message.MessageCreateEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageCreateSPCheck extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        return InviteFilterCheck.check(event.getMessage());
    }

}
