package events.discordevents.eventtypeabstracts;

import java.time.Duration;
import java.util.ArrayList;
import core.AsyncTimer;
import core.MainLogger;
import core.utils.ExceptionUtil;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

public abstract class GuildMessageReactionAddAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) throws Throwable;

    public static void onGuildMessageReactionAddStatic(GuildMessageReactionAddEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        try(AsyncTimer timeOutTimer = new AsyncTimer(Duration.ofSeconds(30))) {
            timeOutTimer.setTimeOutListener(t -> {
                MainLogger.get().error("Reaction add \"{}\" of guild {} stuck", event.getReactionEmote().getAsReactionCode(), event.getGuild().getIdLong(), ExceptionUtil.generateForStack(t));
            });

            execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                    listener -> ((GuildMessageReactionAddAbstract) listener).onGuildMessageReactionAdd(event)
            );
        } catch (InterruptedException e) {
            MainLogger.get().error("Interrupted exception", e);
        }
    }

}
