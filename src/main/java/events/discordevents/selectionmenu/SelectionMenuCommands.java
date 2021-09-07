package events.discordevents.selectionmenu;

import commands.listeners.OnSelectionMenuListener;
import events.discordevents.DiscordEvent;
import events.discordevents.InteractionListenerHandler;
import events.discordevents.eventtypeabstracts.SelectionMenuAbstract;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;

@DiscordEvent
public class SelectionMenuCommands extends SelectionMenuAbstract implements InteractionListenerHandler<SelectionMenuEvent> {

    @Override
    public boolean onSelectionMenu(SelectionMenuEvent event) {
        handleInteraction(event, OnSelectionMenuListener.class,
                listener -> ((OnSelectionMenuListener) listener.getCommand()).processSelectionMenu(event)
        );

        return true;
    }

}
