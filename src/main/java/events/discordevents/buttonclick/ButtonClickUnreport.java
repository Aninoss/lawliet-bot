package events.discordevents.buttonclick;

import java.util.List;
import java.util.stream.Collectors;
import core.components.ActionRows;
import events.discordevents.DiscordEvent;
import events.discordevents.InteractionListenerHandler;
import events.discordevents.eventtypeabstracts.ButtonClickAbstract;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import events.sync.SendEvent;

@DiscordEvent
public class ButtonClickUnreport extends ButtonClickAbstract implements InteractionListenerHandler<ButtonInteractionEvent> {

    @Override
    public boolean onButtonClick(ButtonInteractionEvent event) {
        if (event.getChannel().getIdLong() == 896872855248183316L) {
            event.deferEdit().queue();
            if (event.getComponentId().equals("allow")) {
                SendEvent.sendUnreport(event.getMessage().getContentRaw().split("\n")[0]).join();
                event.getMessage().delete().queue();
            } else if (event.getComponentId().equals("lock")) {
                List<ActionComponent> components = event.getMessage().getActionRows().get(0).getActionComponents();
                List<ActionComponent> newComponents = components.stream()
                        .map(c -> {
                            if (c.getId() != null && c.getId().equals("allow")) {
                                Button previousButton = (Button) c;
                                Button newButton = Button.of(previousButton.getStyle(), previousButton.getId(), previousButton.getLabel());
                                if (!previousButton.isDisabled()) {
                                    newButton = newButton.asDisabled();
                                }
                                return newButton;
                            } else {
                                return c;
                            }
                        }).collect(Collectors.toList());
                event.getMessage().editMessageComponents(ActionRows.of(newComponents)).queue();
            }
            return false;
        }

        return true;
    }

}
