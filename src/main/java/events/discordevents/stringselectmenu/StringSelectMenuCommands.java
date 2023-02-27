package events.discordevents.stringselectmenu;

import commands.listeners.OnSelectMenuListener;
import events.discordevents.DiscordEvent;
import events.discordevents.InteractionListenerHandler;
import events.discordevents.eventtypeabstracts.StringSelectMenuAbstract;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

@DiscordEvent
public class StringSelectMenuCommands extends StringSelectMenuAbstract implements InteractionListenerHandler<StringSelectInteractionEvent> {

    @Override
    public boolean onStringSelectMenu(StringSelectInteractionEvent event) {
        return handleInteraction(event, OnSelectMenuListener.class,
                listener -> ((OnSelectMenuListener) listener.getCommand()).processSelectMenu(event)
        );
    }

}
