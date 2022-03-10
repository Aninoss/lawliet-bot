package events.discordevents.eventtypeabstracts;

import java.time.Instant;
import java.util.ArrayList;
import core.EmbedFactory;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;

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
        if (event.getGuild() == null) {
            if (event.getJDA().getShardInfo().getShardId() == 0) {
                ArrayList<ActionRow> actionRowList = new ArrayList<>();
                EmbedBuilder eb = EmbedFactory.getCommandDMEmbed(actionRowList);
                event.replyEmbeds(eb.build())
                        .addActionRows(actionRowList)
                        .queue();
            }
            return;
        }

        event.deferReply().queue();
        Instant startTime = Instant.now();
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                listener -> {
                    ((SlashCommandAbstract) listener).setStartTime(startTime);
                    return ((SlashCommandAbstract) listener).onSlashCommand(event);
                }
        );
    }

}
