package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;

public abstract class EntitySelectMenuAbstract extends DiscordEventAbstract {

    public abstract boolean onEntitySelectMenu(EntitySelectInteractionEvent event) throws Throwable;

    public static void onEntitySelectMenuStatic(EntitySelectInteractionEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        InteractionAbstract.onInteractionStatic(event, listenerList,
                listener -> ((EntitySelectMenuAbstract) listener).onEntitySelectMenu(event)
        );
    }

}
