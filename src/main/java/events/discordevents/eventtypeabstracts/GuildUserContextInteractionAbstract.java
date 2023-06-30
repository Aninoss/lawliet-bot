package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;

public abstract class GuildUserContextInteractionAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildUserContextInteraction(UserContextInteractionEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onGuildUserContextInteractionStatic(UserContextInteractionEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                (listener, entityManager) -> ((GuildUserContextInteractionAbstract) listener).onGuildUserContextInteraction(event, entityManager)
        );
    }

}
