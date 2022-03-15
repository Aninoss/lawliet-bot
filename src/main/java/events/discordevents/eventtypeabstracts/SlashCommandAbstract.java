package events.discordevents.eventtypeabstracts;

import java.time.Instant;
import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public abstract class SlashCommandAbstract extends DiscordEventAbstract {

    private Instant startTime;

    public abstract boolean onSlashCommand(SlashCommandInteractionEvent event) throws Throwable;

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public static void onSlashCommandStatic(SlashCommandInteractionEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        Instant startTime = Instant.now();
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                listener -> {
                    ((SlashCommandAbstract) listener).setStartTime(startTime);
                    return ((SlashCommandAbstract) listener).onSlashCommand(event);
                }
        );
    }

}
