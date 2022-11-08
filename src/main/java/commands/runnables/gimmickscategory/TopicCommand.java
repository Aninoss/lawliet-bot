package commands.runnables.gimmickscategory;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import core.*;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.schedulers.AlertResponse;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "topic",
        emoji = "\uD83E\uDD60",
        executableWithoutArgs = true,
        aliases = { "topics" }
)
public class TopicCommand extends Command implements OnAlertListener {

    public TopicCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws IOException, ExecutionException, InterruptedException {
        drawMessage(getEmbed(event.getTextChannel())).exceptionally(ExceptionLogger.get());
        return true;
    }

    private EmbedBuilder getEmbed(StandardGuildMessageChannel channel) throws IOException, ExecutionException, InterruptedException {
        List<String> topicList = FileManager.readInList(new LocalFile(LocalFile.Directory.RESOURCES, "topics_" + getLocale().getDisplayName() + ".txt"));
        int n = RandomPicker.pick(getTrigger(), channel.getGuild().getIdLong(), topicList.size()).get();
        String topic = topicList.get(n);

        return EmbedFactory.getEmbedDefault(this, topic);
    }

    @Override
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        final int MIN_MINUTES = 10;
        final int MAX_MINUTES = 10080;
        String key = slot.getCommandKey();

        long minutes = StringUtil.filterLongFromString(key);
        if (minutes > MAX_MINUTES || minutes < MIN_MINUTES) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(
                    this,
                    TextManager.getString(getLocale(), TextManager.GENERAL, "number", StringUtil.numToString(MIN_MINUTES), StringUtil.numToString(MAX_MINUTES))
            );
            EmbedUtil.addTrackerRemoveLog(eb, getLocale());

            slot.sendMessage(false, eb.build());
            return AlertResponse.STOP_AND_DELETE;
        }

        slot.sendMessage(true, getEmbed(slot.getStandardGuildMessageChannel().get()).build());
        slot.setNextRequest(Instant.now().plus(minutes, ChronoUnit.MINUTES));
        return AlertResponse.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

}