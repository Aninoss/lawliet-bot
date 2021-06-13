package events.discordevents.eventtypeabstracts;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import core.MainLogger;
import core.schedule.MainScheduler;
import core.utils.ExceptionUtil;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public abstract class ButtonClickAbstract extends DiscordEventAbstract {

    public abstract boolean onButtonClick(ButtonClickEvent event) throws Throwable;

    public static void onButtonClickStatic(ButtonClickEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        boolean test = event.isFromGuild() &&
                event.getGuild().getIdLong() == 751650997993996388L &&
                event.getUser().getIdLong() == 738800429931954176L; //TODO

        if (test) {
            MainLogger.get().info("--------------");
            MainLogger.get().info("Interaction received");
            MainLogger.get().info("event.isFromGuild(): {}", event.isFromGuild());
            MainLogger.get().info("event.getMessage() != null: {}", event.getMessage() != null);
            MainLogger.get().info("event.getMessageId(): {}", event.getMessageId());
            MainLogger.get().info("event.getComponentId(): {}", event.getComponentId());
        }

        if (event.isFromGuild() && event.getMessage() != null) {
            Thread t = Thread.currentThread();
            AtomicBoolean pending = new AtomicBoolean(true);

            MainScheduler.getInstance().schedule(1, ChronoUnit.SECONDS, "button_click_stuck", () -> {
                if (pending.get()) {
                    MainLogger.get().error("Interaction \"{}\" of guild {} stuck", event.getComponentId(), event.getGuild().getIdLong(), ExceptionUtil.generateForStack(t));
                    if (!event.isAcknowledged()) {
                        event.deferEdit().queue();
                    }
                }
            });

            execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                    listener -> ((ButtonClickAbstract) listener).onButtonClick(event)
            );

            pending.set(false);
        }

        if (test) {
            MainLogger.get().info("End of interaction");
            MainLogger.get().info("event.isAcknowledged(): {}", event.isAcknowledged());
            MainLogger.get().info("--------------");
        }
    }

}
