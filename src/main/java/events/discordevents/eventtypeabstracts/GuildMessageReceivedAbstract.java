package events.discordevents.eventtypeabstracts;

import java.time.Instant;
import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public abstract class GuildMessageReceivedAbstract extends DiscordEventAbstract {

    private Instant startTime;

    public abstract boolean onGuildMessageReceived(GuildMessageReceivedEvent event) throws Throwable;

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }


    public static void onGuildMessageReceivedStatic(GuildMessageReceivedEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.isWebhookMessage()) {
            return;
        }

        Instant startTime = Instant.now();
        execute(listenerList, event.getMember().getUser(), event.getGuild().getIdLong(),
                listener -> {
                    ((GuildMessageReceivedAbstract) listener).setStartTime(startTime);
                    return ((GuildMessageReceivedAbstract) listener).onGuildMessageReceived(event);
                }
        );
    }

}
