package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import constants.Emojis;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public abstract class ButtonClickAbstract extends DiscordEventAbstract {

    public abstract boolean onButtonClick(ButtonClickEvent event) throws Throwable;

    public static void onButtonClickStatic(ButtonClickEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getGuild() != null && event.getMessage() != null) {
            String content = event.getMessage().getContentRaw();
            if (content.isEmpty() || content.equals(Emojis.ZERO_WIDTH_SPACE)) {
                event.editMessage(Emojis.ZERO_WIDTH_SPACE).queue(); //TODO: temporary fix for android devices
            } else {
                event.deferEdit().queue();
            }
            execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                    listener -> ((ButtonClickAbstract) listener).onButtonClick(event)
            );
        }
    }

}
