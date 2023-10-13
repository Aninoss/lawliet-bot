package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

import java.util.ArrayList;

public abstract class StringSelectMenuAbstract extends DiscordEventAbstract {

    public abstract boolean onStringSelectMenu(StringSelectInteractionEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onStringSelectMenuStatic(StringSelectInteractionEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        InteractionAbstract.onInteractionStatic(event, listenerList,
                (listener, entityManager) -> ((StringSelectMenuAbstract) listener).onStringSelectMenu(event, entityManager)
        );
    }

}
