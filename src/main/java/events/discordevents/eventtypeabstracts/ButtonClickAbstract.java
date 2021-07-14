package events.discordevents.eventtypeabstracts;

import java.time.Duration;
import java.util.ArrayList;
import core.MainLogger;
import core.AsyncTimer;
import core.utils.ExceptionUtil;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public abstract class ButtonClickAbstract extends DiscordEventAbstract {

    public abstract boolean onButtonClick(ButtonClickEvent event) throws Throwable;

    public static void onButtonClickStatic(ButtonClickEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.isFromGuild() && event.getMessage() != null) {
            try(AsyncTimer asyncTimer = new AsyncTimer(Duration.ofSeconds(1)))  {
                asyncTimer.setTimeOutListener(t -> {
                    MainLogger.get().error("Interaction \"{}\" of guild {} stuck", event.getComponentId(), event.getGuild().getIdLong(), ExceptionUtil.generateForStack(t));
                    if (!event.isAcknowledged()) {
                        event.deferEdit().queue();
                    }
                });

                execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                        listener -> ((ButtonClickAbstract) listener).onButtonClick(event)
                );
            } catch (InterruptedException e) {
                MainLogger.get().error("Interrupted", e);
            }
        }
    }

}
