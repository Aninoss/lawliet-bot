package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public abstract class ModalInteractionAbstract extends DiscordEventAbstract {

    public abstract boolean onModalInteraction(ModalInteractionEvent event) throws Throwable;

    public static void onModalInteractionStatic(ModalInteractionEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        InteractionAbstract.onInteractionStatic(event, listenerList,
                listener -> ((ModalInteractionAbstract) listener).onModalInteraction(event)
        );
    }

}
