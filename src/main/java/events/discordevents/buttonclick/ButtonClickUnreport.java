package events.discordevents.buttonclick;

import events.discordevents.DiscordEvent;
import events.discordevents.InteractionListenerHandler;
import events.discordevents.eventtypeabstracts.ButtonClickAbstract;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import websockets.syncserver.SendEvent;

@DiscordEvent
public class ButtonClickUnreport extends ButtonClickAbstract implements InteractionListenerHandler<ButtonClickEvent> {

    @Override
    public boolean onButtonClick(ButtonClickEvent event) {
        if (event.getComponentId().equals("allow") && event.getChannel().getIdLong() == 896872855248183316L) {
            SendEvent.sendUnreport(event.getMessage().getContentRaw()).join();
            event.getMessage().delete().queue();
        }

        return true;
    }

}
