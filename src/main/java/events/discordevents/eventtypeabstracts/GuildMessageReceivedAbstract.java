package events.discordevents.eventtypeabstracts;

import constants.AssetIds;
import core.MainLogger;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.Instant;
import java.util.ArrayList;

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
                        if (event.getAuthor().getIdLong() == AssetIds.OWNER_USER_ID && event.getMessage().getContentRaw().equals("processing_time_test")) {
                            MainLogger.get().info("{}: {}", listener.getClass().getSimpleName(), Instant.now());
                        }

                        return ((GuildMessageReceivedAbstract) listener).onGuildMessageReceived(event, entityManager);
                    } else {
                        return true;
                    }
                }
        );
    }

}
