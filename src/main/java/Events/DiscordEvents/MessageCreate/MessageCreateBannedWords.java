package Events.DiscordEvents.MessageCreate;

import Events.DiscordEvents.DiscordEvent;
import Events.DiscordEvents.EventPriority;
import Events.DiscordEvents.EventTypeAbstracts.MessageCreateAbstract;
import Modules.BannedWordsCheck;
import org.javacord.api.event.message.MessageCreateEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageCreateBannedWords extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        return BannedWordsCheck.check(event.getMessage());
    }

}