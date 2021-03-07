package commands.runnables.gimmickscategory;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import constants.TrackerResult;
import core.*;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import mysql.modules.tracker.TrackerBeanSlot;
import net.dv8tion.jda.api.EmbedBuilder;

@CommandProperties(
        trigger = "topic",
        emoji = "\uD83E\uDD60",
        executableWithoutArgs = true,
        aliases = {"topics"}
)
public class TopicCommand extends Command implements OnTrackerRequestListener {

    public TopicCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        send(event.getServerTextChannel().get());
        return true;
    }

    private void send(ServerTextChannel channel) throws IOException, ExecutionException, InterruptedException {
        List<String> topicList = FileManager.readInList(ResourceHandler.getFileResource("data/resources/topics_" + getLocale().getDisplayName() + ".txt"));
        int n = RandomPicker.getInstance().pick(getTrigger(), channel.getServer().getId(), topicList.size());
        String topic = topicList.get(n);

        channel.sendMessage(EmbedFactory.getEmbedDefault(this, topic)).get();
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        final int MIN_MINUTES = 10;
        final int MAX_MINUTES = 10080;
        String key = slot.getCommandKey();

        long minutes = StringUtil.filterLongFromString(key);
        if (minutes > MAX_MINUTES || minutes < MIN_MINUTES) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL, "number", StringUtil.numToString(MIN_MINUTES), StringUtil.numToString(MAX_MINUTES)));
            EmbedUtil.addTrackerRemoveLog(eb, getLocale());

            slot.getChannel().get().sendMessage(eb).get();
            return TrackerResult.STOP_AND_DELETE;
        }

        send(slot.getChannel().get());
        slot.setNextRequest(Instant.now().plus(minutes, ChronoUnit.MINUTES));

        return TrackerResult.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

}