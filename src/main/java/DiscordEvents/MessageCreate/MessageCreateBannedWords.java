package DiscordEvents.MessageCreate;

import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventPriority;
import DiscordEvents.EventTypeAbstracts.MessageCreateAbstract;
import Modules.BannedWordsCheck;
import org.javacord.api.event.message.MessageCreateEvent;

@DiscordEventAnnotation(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageCreateBannedWords extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        return BannedWordsCheck.check(event.getMessage());
    }

}
