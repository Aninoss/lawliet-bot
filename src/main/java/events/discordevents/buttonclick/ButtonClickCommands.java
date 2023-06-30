package events.discordevents.buttonclick;

import commands.listeners.OnButtonListener;
import events.discordevents.DiscordEvent;
import events.discordevents.InteractionListenerHandler;
import events.discordevents.eventtypeabstracts.ButtonClickAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

@DiscordEvent
public class ButtonClickCommands extends ButtonClickAbstract implements InteractionListenerHandler<ButtonInteractionEvent> {

    @Override
    public boolean onButtonClick(ButtonInteractionEvent event, EntityManagerWrapper entityManager) {
        return handleInteraction(event, OnButtonListener.class,
                listener -> ((OnButtonListener) listener.getCommand()).processButton(event, entityManager)
        );
    }

}
