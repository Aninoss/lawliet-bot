package events.discordevents.eventtypeabstracts;

import java.time.Instant;
import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class GuildMessageReceivedAbstract extends DiscordEventAbstract {

    private Instant startTime;

    public abstract boolean onGuildMessageReceived(MessageReceivedEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }


    public static void onGuildMessageReceivedStatic(MessageReceivedEvent event, ArrayList<DiscordEventAbstract> listenerList, Instant startTime) {
        if (event.getMessage().getType().isSystem()) {
            return;
        }

        execute(listenerList, event.getAuthor(), event.getGuild().getIdLong(),
                (listener, entityManager) -> {
                    if (!event.isWebhookMessage() || listener.isAllowingBots()) {
                        ((GuildMessageReceivedAbstract) listener).setStartTime(startTime);
                        return ((GuildMessageReceivedAbstract) listener).onGuildMessageReceived(event, entityManager);
                    } else {
                        return true;
                    }
                }
        );
    }

}
