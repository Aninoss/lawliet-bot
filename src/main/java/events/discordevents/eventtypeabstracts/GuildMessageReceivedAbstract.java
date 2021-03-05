package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import java.time.Instant;
import java.util.ArrayList;

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
        Member member = event.getMember();
        if (member == null)
            return;

        Instant startTime = Instant.now();
        execute(listenerList, member.getUser(), event.getGuild().getIdLong(),
                listener -> {
                    ((GuildMessageReceivedAbstract) listener).setStartTime(startTime);
                    return ((GuildMessageReceivedAbstract) listener).onGuildMessageReceived(event);
                }
        );
    }

}
