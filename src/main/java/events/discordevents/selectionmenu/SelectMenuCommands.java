package events.discordevents.selectionmenu;

import commands.listeners.OnSelectMenuListener;
import events.discordevents.DiscordEvent;
import events.discordevents.InteractionListenerHandler;
import events.discordevents.eventtypeabstracts.SelectMenuAbstract;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

@DiscordEvent
public class SelectMenuCommands extends SelectMenuAbstract implements InteractionListenerHandler<StringSelectInteractionEvent> {

    @Override
    public boolean onSelectMenu(StringSelectInteractionEvent event) {
        return handleInteraction(event, OnSelectMenuListener.class,
                listener -> ((OnSelectMenuListener) listener.getCommand()).processSelectMenu(event)
        );
    }

}
