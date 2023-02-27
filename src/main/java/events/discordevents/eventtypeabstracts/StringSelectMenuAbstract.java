package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

public abstract class StringSelectMenuAbstract extends DiscordEventAbstract {

    public abstract boolean onStringSelectMenu(StringSelectInteractionEvent event) throws Throwable;

    public static void onStringSelectMenuStatic(StringSelectInteractionEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        InteractionAbstract.onInteractionStatic(event, listenerList,
                listener -> ((StringSelectMenuAbstract) listener).onStringSelectMenu(event)
        );
    }

}
