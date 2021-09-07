package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;

public abstract class SelectionMenuAbstract extends DiscordEventAbstract {

    public abstract boolean onSelectionMenu(SelectionMenuEvent event) throws Throwable;

    public static void onSelectionMenuStatic(SelectionMenuEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        InteractionAbstract.onInteractionStatic(event, listenerList,
                listener -> ((SelectionMenuAbstract) listener).onSelectionMenu(event)
        );
    }

}
