package DiscordListener.ListenerTypeAbstracts;

import DiscordListener.DiscordListenerAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

public abstract class MessageCreateAbstract extends DiscordListenerAbstract {

    public abstract boolean onMessageCreate(MessageCreateEvent event) throws Throwable;

}
