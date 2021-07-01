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
        boolean test = event.getGuild() != null &&
                (event.getUser().getIdLong() == 738800429931954176L || event.getUser().getIdLong() == 707100505062768651L); //TODO

        if (test) {
            MainLogger.get().info("--------------");
            MainLogger.get().info("Interaction received");
            MainLogger.get().info("event.isFromGuild(): {}", event.isFromGuild());
            MainLogger.get().info("event.getMessage() != null: {}", event.getMessage() != null);
            MainLogger.get().info("event.getMessageId(): {}", event.getMessageId());
            MainLogger.get().info("event.getGuild().getIdLong(): {}", event.getGuild().getIdLong());
            MainLogger.get().info("event.getUser().getIdLong(): {}", event.getUser().getIdLong());
            MainLogger.get().info("event.getComponentId(): {}", event.getComponentId());
        }

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
            } catch (Exception exception) {
                MainLogger.get().error("Interrupted", exception);
            }
        }

        if (test) {
            MainLogger.get().info("End of interaction");
            MainLogger.get().info("event.isAcknowledged(): {}", event.isAcknowledged());
            MainLogger.get().info("--------------");
        }
    }

}
