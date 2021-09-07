package events.discordevents.buttonclick;

import commands.listeners.OnButtonListener;
import events.discordevents.DiscordEvent;
import events.discordevents.InteractionListenerHandler;
import events.discordevents.eventtypeabstracts.ButtonClickAbstract;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

@DiscordEvent
public class ButtonClickCommands extends ButtonClickAbstract implements InteractionListenerHandler<ButtonClickEvent> {

    @Override
    public boolean onButtonClick(ButtonClickEvent event) {
        handleInteraction(event, OnButtonListener.class,
                listener -> ((OnButtonListener) listener.getCommand()).processButton(event)
        );

        return true;
    }

}
