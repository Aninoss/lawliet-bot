package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;

public abstract class GuildMessageDeleteAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMessageDelete(MessageDeleteEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onGuildMessageDeleteStatic(MessageDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                (listener, entityManager) -> ((GuildMessageDeleteAbstract) listener).onGuildMessageDelete(event, entityManager)
        );
    }

}
