package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;

public abstract class EntitySelectMenuAbstract extends DiscordEventAbstract {

    public abstract boolean onEntitySelectMenu(EntitySelectInteractionEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onEntitySelectMenuStatic(EntitySelectInteractionEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        InteractionAbstract.onInteractionStatic(event, listenerList,
                (listener, entityManager) -> ((EntitySelectMenuAbstract) listener).onEntitySelectMenu(event, entityManager)
        );
    }

}
