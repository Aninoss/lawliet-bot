package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public abstract class ButtonClickAbstract extends DiscordEventAbstract {

    public abstract boolean onButtonClick(ButtonInteractionEvent event) throws Throwable;

    public static void onButtonClickStatic(ButtonInteractionEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        InteractionAbstract.onInteractionStatic(event, listenerList,
                listener -> ((ButtonClickAbstract) listener).onButtonClick(event)
        );
    }

}
