package events.discordevents.eventtypeabstracts;

import core.AsyncTimer;
import core.MainLogger;
import core.utils.ExceptionUtil;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class InteractionAbstract extends DiscordEventAbstract {

    public static void onInteractionStatic(GenericInteractionCreateEvent event,
                                           ArrayList<DiscordEventAbstract> listenerList,
                                           DiscordEventAbstract.EventExecution function
    ) {
        int eventAgeMillis = (int) Duration.between(event.getTimeCreated().toInstant(), Instant.now()).toMillis();
        try(AsyncTimer asyncTimer = new AsyncTimer(Duration.ofMillis(1500 - eventAgeMillis)))  {
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

                execute(listenerList, event.getUser(), event.getGuild() != null ? event.getGuild().getIdLong() : 0L, function);
            }
        }
    }

}
