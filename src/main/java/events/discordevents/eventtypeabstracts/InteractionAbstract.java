package events.discordevents.eventtypeabstracts;

import java.time.Duration;
import java.util.ArrayList;
import core.AsyncTimer;
import core.MainLogger;
import core.utils.ExceptionUtil;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

public class InteractionAbstract extends DiscordEventAbstract {

    public static void onInteractionStatic(GenericComponentInteractionCreateEvent event,
                                           ArrayList<DiscordEventAbstract> listenerList,
                                           DiscordEventAbstract.EventExecution function
    ) {
        if (event.isFromGuild()) {
            try(AsyncTimer asyncTimer = new AsyncTimer(Duration.ofSeconds(1)))  {
                asyncTimer.setTimeOutListener(t -> {
                    if (!event.isAcknowledged()) {
                        event.deferEdit().queue();
                    }
                });

                try(AsyncTimer timeOutTimer = new AsyncTimer(Duration.ofSeconds(30))) {
                    timeOutTimer.setTimeOutListener(t -> {
                        MainLogger.get().error("Interaction \"{}\" of guild {} stuck", event.getComponentId(), event.getGuild().getIdLong(), ExceptionUtil.generateForStack(t));
                    });

                    execute(listenerList, event.getUser(), event.getGuild().getIdLong(), function);
                }
            } catch (InterruptedException e) {
                MainLogger.get().error("Interrupted", e);
            }
        }
    }

}
