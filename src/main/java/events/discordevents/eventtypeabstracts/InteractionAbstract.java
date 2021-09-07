package events.discordevents.eventtypeabstracts;

import java.time.Duration;
import core.AsyncTimer;
import core.MainLogger;
import core.utils.ExceptionUtil;
import net.dv8tion.jda.api.events.interaction.GenericComponentInteractionCreateEvent;

public class InteractionAbstract {

    public static void onInteractionStatic(GenericComponentInteractionCreateEvent event, Runnable onValid) {
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

                    onValid.run();
                }
            } catch (InterruptedException e) {
                MainLogger.get().error("Interrupted", e);
            }
        }
    }

}
