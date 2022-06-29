package events.discordevents.eventtypeabstracts;

import java.time.Duration;
import java.util.ArrayList;
import core.AsyncTimer;
import core.MainLogger;
import core.utils.ExceptionUtil;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;

public abstract class GuildMessageReactionRemoveAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMessageReactionRemove(MessageReactionRemoveEvent event) throws Throwable;

    public static void onGuildMessageReactionRemoveStatic(MessageReactionRemoveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        try(AsyncTimer timeOutTimer = new AsyncTimer(Duration.ofSeconds(30))) {
            timeOutTimer.setTimeOutListener(t -> {
                MainLogger.get().error("Reaction remove \"{}\" of guild {} stuck", event.getEmoji().getAsReactionCode(), event.getGuild().getIdLong(), ExceptionUtil.generateForStack(t));
            });

            execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                    listener -> ((GuildMessageReactionRemoveAbstract) listener).onGuildMessageReactionRemove(event)
            );
        } catch (InterruptedException e) {
            MainLogger.get().error("Interrupted exception", e);
        }
    }

}
