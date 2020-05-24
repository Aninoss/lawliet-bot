package DiscordListener.MessageEdit;

import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerPriority;
import DiscordListener.ListenerTypeAbstracts.MessageEditAbstract;
import Modules.LinkCheck;
import org.javacord.api.event.message.MessageEditEvent;

@DiscordListenerAnnotation(priority = ListenerPriority.HIGH)
public class MessageEditLinkCheck extends MessageEditAbstract {

    @Override
    public boolean onMessageEdit(MessageEditEvent event) throws Throwable {
        return LinkCheck.check(event.getMessage().get());
    }

}
