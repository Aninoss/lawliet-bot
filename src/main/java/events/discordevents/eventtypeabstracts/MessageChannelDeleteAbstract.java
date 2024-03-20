package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;

import java.util.ArrayList;

public abstract class MessageChannelDeleteAbstract extends DiscordEventAbstract {

    public abstract boolean onMessageChannelDelete(ChannelDeleteEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onMessageChannelDeleteStatic(ChannelDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                (listener, entityManager) -> ((MessageChannelDeleteAbstract) listener).onMessageChannelDelete(event, entityManager)
        );
    }

}
