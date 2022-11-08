package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

public abstract class SelectMenuAbstract extends DiscordEventAbstract {

    public abstract boolean onSelectMenu(StringSelectInteractionEvent event) throws Throwable;

    public static void onSelectMenuStatic(StringSelectInteractionEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        InteractionAbstract.onInteractionStatic(event, listenerList,
                listener -> ((SelectMenuAbstract) listener).onSelectMenu(event)
        );
    }

}
