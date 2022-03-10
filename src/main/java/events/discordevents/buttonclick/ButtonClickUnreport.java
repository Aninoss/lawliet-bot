package events.discordevents.buttonclick;

import java.util.List;
import java.util.stream.Collectors;
import core.components.ActionRows;
import events.discordevents.DiscordEvent;
import events.discordevents.InteractionListenerHandler;
import events.discordevents.eventtypeabstracts.ButtonClickAbstract;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.Component;
import websockets.syncserver.SendEvent;

@DiscordEvent
public class ButtonClickUnreport extends ButtonClickAbstract implements InteractionListenerHandler<ButtonClickEvent> {

    @Override
    public boolean onButtonClick(ButtonClickEvent event) {
        if (event.getChannel().getIdLong() == 896872855248183316L) {
            event.deferEdit().queue();
            if (event.getComponentId().equals("allow")) {
                SendEvent.sendUnreport(event.getMessage().getContentRaw().split("\n")[0]).join();
                event.getMessage().delete().queue();
            } else if (event.getComponentId().equals("lock")) {
                List<Component> components = event.getMessage().getActionRows().get(0).getComponents();
                List<Component> newComponents = components.stream()
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
        }

        return true;
    }

}
