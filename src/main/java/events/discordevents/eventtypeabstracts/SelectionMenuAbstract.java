package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;

public abstract class SelectionMenuAbstract extends DiscordEventAbstract {

    public abstract boolean onSelectionMenu(SelectMenuInteractionEvent event) throws Throwable;

    public static void onSelectionMenuStatic(SelectMenuInteractionEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        InteractionAbstract.onInteractionStatic(event, listenerList,
                listener -> ((SelectionMenuAbstract) listener).onSelectionMenu(event)
        );
    }

}
