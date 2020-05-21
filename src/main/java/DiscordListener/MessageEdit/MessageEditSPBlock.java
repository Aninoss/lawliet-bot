package DiscordListener.MessageEdit;

import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerTypeAbstracts.MessageEditAbstract;
import Modules.BannedWordsCheck;
import Modules.SPCheck;
import org.javacord.api.event.message.MessageEditEvent;

@DiscordListenerAnnotation(highPriority = true)
public class MessageEditSPBlock extends MessageEditAbstract {

    @Override
    public boolean onMessageEdit(MessageEditEvent event) throws Throwable {
        return SPCheck.check(event.getMessage().get());
    }

}
