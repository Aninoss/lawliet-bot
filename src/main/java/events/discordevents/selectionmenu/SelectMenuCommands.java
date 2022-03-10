package events.discordevents.selectionmenu;

import commands.listeners.OnSelectMenuListener;
import events.discordevents.DiscordEvent;
import events.discordevents.InteractionListenerHandler;
import events.discordevents.eventtypeabstracts.SelectMenuAbstract;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;

@DiscordEvent
public class SelectMenuCommands extends SelectMenuAbstract implements InteractionListenerHandler<SelectMenuInteractionEvent> {

    @Override
    public boolean onSelectMenu(SelectMenuInteractionEvent event) {
        handleInteraction(event, OnSelectMenuListener.class,
                listener -> ((OnSelectMenuListener) listener.getCommand()).processSelectMenu(event)
        );

        return true;
    }

}
