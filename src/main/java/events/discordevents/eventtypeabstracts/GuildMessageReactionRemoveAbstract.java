package events.discordevents.eventtypeabstracts;

import java.time.Duration;
import java.util.ArrayList;
import core.AsyncTimer;
import core.MainLogger;
import core.utils.ExceptionUtil;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;

public abstract class GuildMessageReactionRemoveAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) throws Throwable;

    public static void onGuildMessageReactionRemoveStatic(GuildMessageReactionRemoveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        try(AsyncTimer timeOutTimer = new AsyncTimer(Duration.ofSeconds(30))) {
            timeOutTimer.setTimeOutListener(t -> {
                MainLogger.get().error("Reaction remove \"{}\" of guild {} stuck", event.getReactionEmote().getAsReactionCode(), event.getGuild().getIdLong(), ExceptionUtil.generateForStack(t));
            });

            execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                    listener -> ((GuildMessageReactionRemoveAbstract) listener).onGuildMessageReactionRemove(event)
            );
        } catch (InterruptedException e) {
            MainLogger.get().error("Interrupted exception", e);
        }
    }

}
