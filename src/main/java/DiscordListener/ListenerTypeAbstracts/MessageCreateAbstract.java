package DiscordListener.ListenerTypeAbstracts;

import Constants.Settings;
import Core.EmbedFactory;
import DiscordListener.DiscordListenerAbstract;
import DiscordListener.DiscordListenerManager;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class MessageCreateAbstract extends DiscordListenerAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(MessageCreateAbstract.class);

    public abstract boolean onMessageCreate(MessageCreateEvent event) throws Throwable;

    public static void onMessageCreateStatic(MessageCreateEvent event, ArrayList<DiscordListenerAbstract> listenerList) {
        if (!event.getMessage().getUserAuthor().isPresent() ||
                event.getMessage().getAuthor().isYourself() ||
                event.getMessage().getUserAuthor().get().isBot()
        ) return;

        if (!event.getServer().isPresent()) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError()
                    .setTitle("❌ Not Supported!".toUpperCase())
                    .setDescription(String.format("Commands via dm aren't supported, you need to [\uD83D\uDD17 invite](%s) Lawliet into a server!", Settings.BOT_INVITE_URL)));
            return;
        }

        for(DiscordListenerAbstract listener : listenerList) {
            if (listener instanceof MessageCreateAbstract) {
                MessageCreateAbstract messageCreateAbstract = (MessageCreateAbstract) listener;

                try {
                    if (!messageCreateAbstract.onMessageCreate(event)) return;
                } catch (InterruptedException interrupted) {
                    LOGGER.error("Interrupted", interrupted);
                    return;
                } catch (Throwable throwable) {
                    LOGGER.error("Uncaught Excecption", throwable);
                }
            }
        }
    }

}
