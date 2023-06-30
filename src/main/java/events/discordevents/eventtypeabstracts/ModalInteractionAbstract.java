package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public abstract class ModalInteractionAbstract extends DiscordEventAbstract {

    public abstract boolean onModalInteraction(ModalInteractionEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onModalInteractionStatic(ModalInteractionEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        InteractionAbstract.onInteractionStatic(event, listenerList,
                (listener, entityManager) -> ((ModalInteractionAbstract) listener).onModalInteraction(event, entityManager)
        );
    }

}
