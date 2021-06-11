package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import core.MainLogger;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public abstract class ButtonClickAbstract extends DiscordEventAbstract {

    public abstract boolean onButtonClick(ButtonClickEvent event) throws Throwable;

    public static void onButtonClickStatic(ButtonClickEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        boolean test = true; //TODO
        if (test) {
            MainLogger.get().info("--------------");
            MainLogger.get().info("Interaction received");
            MainLogger.get().info("event.isFromGuild(): {}", event.isFromGuild());
            MainLogger.get().info("event.getMessage() != null: {}", event.getMessage() != null);
            MainLogger.get().info("event.getMessageId(): {}", event.getMessageId());
            MainLogger.get().info("event.getComponentId(): {}", event.getComponentId());
        }

        if (event.isFromGuild() && event.getMessage() != null) {
            execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                    listener -> ((ButtonClickAbstract) listener).onButtonClick(event)
            );
        }

        if (test) {
            MainLogger.get().info("End of interaction");
            MainLogger.get().info("event.isAcknowledged(): {}", event.isAcknowledged());
            MainLogger.get().info("--------------");
        }
    }

}
