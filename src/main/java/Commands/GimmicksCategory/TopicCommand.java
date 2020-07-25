package Commands.GimmicksCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnTrackerRequestListener;
import CommandSupporters.Command;
import Constants.TrackerResult;
import Core.EmbedFactory;
import Core.FileManager;
import Core.RandomPicker;
import Core.TextManager;
import Core.Utils.StringUtil;
import MySQL.Modules.Tracker.TrackerBeanSlot;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.event.message.MessageCreateEvent;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "topic",
        emoji = "\uD83E\uDD60",
        executable = true,
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
        List<String> topicList = FileManager.readInList(new File("recourses/topics_" + getLocale().getDisplayName() + ".txt"));
        int n = RandomPicker.getInstance().pick(getTrigger(), channel.getServer().getId(), topicList.size());
        String topic = topicList.get(n);

        channel.sendMessage(EmbedFactory.getCommandEmbedStandard(this, topic)).get();
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        final int MAX_MINUTES = 10080;
        String key = slot.getCommandKey().get();

        long minutes = StringUtil.filterLongFromString(key);
        if (minutes > MAX_MINUTES || minutes < 1) {
            slot.getChannel().get().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", StringUtil.numToString(getLocale(), MAX_MINUTES)))).get();
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