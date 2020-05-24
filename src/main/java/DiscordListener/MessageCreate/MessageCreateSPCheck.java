package DiscordListener.MessageCreate;

import DiscordListener.*;
import DiscordListener.ListenerTypeAbstracts.MessageCreateAbstract;
import Modules.SPCheck;
import org.javacord.api.event.message.MessageCreateEvent;

@DiscordListenerAnnotation(priority = ListenerPriority.HIGH)
public class MessageCreateSPCheck extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        return SPCheck.check(event.getMessage());
    }

}
