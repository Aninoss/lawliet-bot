package DiscordListener.ListenerTypeAbstracts;

import DiscordListener.DiscordListenerAbstract;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageEditEvent;

public abstract class MessageEditAbstract extends DiscordListenerAbstract {

    public abstract boolean onMessageEdit(MessageEditEvent event) throws Throwable;

}
