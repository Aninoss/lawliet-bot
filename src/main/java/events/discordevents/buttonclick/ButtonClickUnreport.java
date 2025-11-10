package events.discordevents.buttonclick;

import core.components.ActionRows;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ButtonClickAbstract;
import events.sync.SendEvent;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.List;
import java.util.stream.Collectors;

@DiscordEvent
public class ButtonClickUnreport extends ButtonClickAbstract {

    @Override
    public boolean onButtonClick(ButtonInteractionEvent event, EntityManagerWrapper entityManager) {
        if (event.getChannel().getIdLong() == 896872855248183316L) {
            event.deferEdit().queue();
            if (event.getComponentId().equals("allow")) {
                SendEvent.sendUnreport(event.getMessage().getContentRaw().split("\n")[0]).join();
                event.getMessage().delete().queue();
            } else if (event.getComponentId().equals("lock")) {
                ActionRow actionRow = event.getMessage().getComponents().get(0).asActionRow();
                List<ActionRowChildComponent> newComponents = actionRow.getComponents().stream()
                        .map(component -> {
                            Button button = component.asButton();
                            if (button.getCustomId() != null && button.getCustomId().equals("allow")) {
                                Button newButton = Button.of(button.getStyle(), button.getCustomId(), button.getLabel());
                                if (!button.isDisabled()) {
                                    newButton = newButton.asDisabled();
                                }
                                return newButton;
                            } else {
                                return button;
                            }
                        }).collect(Collectors.toList());
                event.getMessage().editMessageComponents(ActionRows.of(newComponents)).queue();
            }
            return false;
        }

        return true;
    }

}
