package DiscordListener.MessageEdit;

import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerPriority;
import DiscordListener.ListenerTypeAbstracts.MessageCreateAbstract;
import DiscordListener.ListenerTypeAbstracts.MessageEditAbstract;
import Modules.BannedWordsCheck;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageEditEvent;

@DiscordListenerAnnotation(priority = ListenerPriority.HIGH)
public class MessageEditBannedWords extends MessageEditAbstract {

    @Override
    public boolean onMessageEdit(MessageEditEvent event) throws Throwable {
        return BannedWordsCheck.check(event.getMessage().get());
    }

}
