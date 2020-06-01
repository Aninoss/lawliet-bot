package DiscordEvents.MessageEdit;

import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventPriority;
import DiscordEvents.EventTypeAbstracts.MessageEditAbstract;
import Modules.BannedWordsCheck;
import org.javacord.api.event.message.MessageEditEvent;

@DiscordEventAnnotation(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageEditBannedWords extends MessageEditAbstract {

    @Override
    public boolean onMessageEdit(MessageEditEvent event) throws Throwable {
        return BannedWordsCheck.check(event.getMessage().get());
    }

}
