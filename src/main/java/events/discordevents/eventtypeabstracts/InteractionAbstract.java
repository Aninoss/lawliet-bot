package events.discordevents.eventtypeabstracts;

import core.AsyncTimer;
import core.MainLogger;
import core.utils.ExceptionUtil;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.time.Duration;
import java.util.ArrayList;

public class InteractionAbstract extends DiscordEventAbstract {

    public static void onInteractionStatic(GenericInteractionCreateEvent event,
                                           ArrayList<DiscordEventAbstract> listenerList,
                                           DiscordEventAbstract.EventExecution function
    ) {
        if (event.isFromGuild()) {
            try(AsyncTimer asyncTimer = new AsyncTimer(Duration.ofSeconds(1)))  {
                asyncTimer.setTimeOutListener(t -> {
                    if (!event.isAcknowledged()) {
                        if (event instanceof GenericComponentInteractionCreateEvent) {
                            ((GenericComponentInteractionCreateEvent) event).deferEdit().queue();
                        } else if (event instanceof ModalInteractionEvent) {
                            ((ModalInteractionEvent) event).deferEdit().queue();
                        }
                    }
                });

                try(AsyncTimer timeOutTimer = new AsyncTimer(Duration.ofSeconds(30))) {
                    timeOutTimer.setTimeOutListener(t -> {
                        MainLogger.get().error("Interaction \"{}\" of guild {} stuck", event.getIdLong(), event.getGuild().getIdLong(), ExceptionUtil.generateForStack(t));
                    });

                    execute(listenerList, event.getUser(), event.getGuild().getIdLong(), function);
                }
            } catch (InterruptedException e) {
                MainLogger.get().error("Interrupted", e);
            }
        }
    }

}
