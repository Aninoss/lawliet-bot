package commands.runnables.moderationcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import constants.TrackerResult;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.schedule.MainScheduler;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.ClearResults;
import mysql.modules.tracker.TrackerBeanSlot;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "fullclear",
        botPermissions = PermissionDeprecated.MANAGE_MESSAGES | PermissionDeprecated.READ_MESSAGE_HISTORY,
        userPermissions = PermissionDeprecated.MANAGE_MESSAGES | PermissionDeprecated.READ_MESSAGE_HISTORY,
        withLoadingBar = true,
        emoji = "\uD83E\uDDF9",
        executableWithoutArgs = true,
        maxCalculationTimeSec = 10 * 60,
        aliases = { "fclear", "allclear", "clearall" }
)
public class FullClearCommand extends Command implements OnTrackerRequestListener {

    public FullClearCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Optional<Integer> hoursMin = extractHoursMin(event.getServerTextChannel().get(), followedString);
        if (hoursMin.isPresent()) {
            ClearResults clearResults = fullClear(event.getServerTextChannel().get(), hoursMin.get(), event.getMessage());

            String key = clearResults.getRemaining() > 0 ? "finished_too_old" : "finished_description";
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(key, clearResults.getDeleted() != 1, String.valueOf(clearResults.getDeleted())));
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "deleteTime", "8"));
            if (event.getChannel().getCurrentCachedInstance().isPresent() && event.getChannel().canYouSee() && event.getChannel().canYouWrite() && event.getChannel().canYouEmbedLinks()) {
                Message m = event.getChannel().sendMessage(eb).get();
                MainScheduler.getInstance().schedule(8, ChronoUnit.SECONDS, "fullclear_confirmation_autoremove", () -> event.getChannel().bulkDelete(m, event.getMessage()));
            }
            return true;
        } else {
            return false;
        }
    }

    private Optional<Integer> extractHoursMin(ServerTextChannel channel, String str) throws ExecutionException, InterruptedException {
        if (str.length() > 0) {
            if (StringUtil.stringIsLong(str) && Long.parseLong(str) >= 0 && Long.parseLong(str) <= 20159) {
                return Optional.of(Integer.parseInt(str));
            } else {
                channel.sendMessage(EmbedFactory.getEmbedError(
                        this,
                        getString("wrong_args", "0", "20159")
                )).get();
                return Optional.empty();
            }
        } else {
            return Optional.of(0);
        }
    }

    private ClearResults fullClear(ServerTextChannel channel, int hours) throws ExecutionException, InterruptedException {
        return fullClear(channel, hours, null);
    }

    private ClearResults fullClear(ServerTextChannel channel, int hours, Message messageBefore) throws ExecutionException, InterruptedException {
        int deleted = 0;
        boolean skipped = false;

        MessageSet messageSet;
        if (messageBefore != null) messageSet = messageBefore.getMessagesBefore(100).get();
        else messageSet = channel.getMessages(100).get();

        while (messageSet.size() > 0 && !skipped) {
            //Check for message date and therefore permissions
            ArrayList<Message> messagesDelete = new ArrayList<>();
            for (Message message : messageSet.descendingSet()) {
                if (!message.getCreationTimestamp().isAfter(Instant.now().minus(14, ChronoUnit.DAYS))) {
                    skipped = true;
                    break;
                } else if (!message.isPinned()) {
                    if (!message.getCreationTimestamp().isAfter(Instant.now().minus(hours, ChronoUnit.HOURS))) {
                        messagesDelete.add(message);
                        deleted++;
                    }
                }
            }

            if (messagesDelete.size() >= 1) {
                if (messagesDelete.size() == 1) messagesDelete.get(0).delete().get();
                else channel.bulkDelete(messagesDelete).get();
            }

            if (messageSet.size() > 0)
                messageSet = messageSet.getOldestMessage().get().getMessagesBefore(100).get();
        }

        return new ClearResults(deleted, skipped ? 1 : 0);
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        Optional<ServerTextChannel> channelOptional = slot.getChannel();
        if (channelOptional.isPresent()) {
            if (PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getClass(), channelOptional.get(), PermissionDeprecated.READ_MESSAGE_HISTORY | PermissionDeprecated.MANAGE_MESSAGES)) {
                Optional<Integer> hoursMin = extractHoursMin(channelOptional.get(), slot.getCommandKey());
                if (hoursMin.isPresent()) {
                    fullClear(channelOptional.get(), hoursMin.get());
                    slot.setNextRequest(Instant.now().plus(1, ChronoUnit.HOURS));
                    return TrackerResult.CONTINUE_AND_SAVE;
                }
            }
        }

        return TrackerResult.STOP_AND_DELETE;
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

}
