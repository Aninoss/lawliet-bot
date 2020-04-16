package Commands.ModerationCategory;

import CommandListeners.CommandProperties;

import CommandListeners.OnTrackerRequestListener;
import CommandSupporters.Command;
import Constants.Permission;
import Constants.TrackerResult;
import Core.*;
import Core.Tools.StringTools;
import MySQL.Modules.Tracker.TrackerBeanSlot;
import javafx.util.Pair;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "fullclear",
        botPermissions = Permission.MANAGE_MESSAGES | Permission.READ_MESSAGE_HISTORY,
        userPermissions = Permission.MANAGE_MESSAGES | Permission.READ_MESSAGE_HISTORY,
        withLoadingBar = true,
        emoji = "\uD83E\uDDF9",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/colorful-long-shadow/128/Recyclebin-icon.png",
        executable = false,
        aliases = {"fclear"}
)
public class FullClearCommand extends Command implements OnTrackerRequestListener {

    final static Logger LOGGER = LoggerFactory.getLogger(FullClearCommand.class);

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Pair<Integer, Boolean> pair = fullClear(event.getServerTextChannel().get(), followedString, event.getMessage());
        if (pair == null) return false;
        boolean skipped = pair.getValue();
        int deleted = pair.getKey();

        String key = skipped ? "finished_too_old" : "finished_description";
        Message m = event.getChannel().sendMessage(EmbedFactory.getCommandEmbedSuccess(this, getString(key, deleted != 1, String.valueOf(deleted)))
                .setFooter(TextManager.getString(getLocale(), TextManager.GENERAL, "deleteTime", "8"))).get();
        Thread t = new CustomThread(() -> {
            try {
                Thread.sleep(8000);
                Message[] messagesArray = new Message[]{m, event.getMessage()};
                event.getChannel().bulkDelete(messagesArray);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted", e);
            }
        }, "fullclear_countdown", 1);
        t.start();
        return true;
    }

    private Pair<Integer, Boolean> fullClear(ServerTextChannel channel, String str, Message messageBefore) throws ExecutionException, InterruptedException, IOException {
        int hours = 0;
        if (str.length() > 0) {
            if (StringTools.stringIsLong(str) && Long.parseLong(str) >= 0 && Long.parseLong(str) <= 20159) {
                hours = Integer.parseInt(str);
            } else {
                channel.sendMessage(EmbedFactory.getCommandEmbedError(this,
                        getString("wrong_args", "0", "20159"))).get();
                return null;
            }
        }

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
                } else {
                    if (!message.getCreationTimestamp().isAfter(Instant.now().minus(hours, ChronoUnit.HOURS))) {
                        messagesDelete.add(message);
                    }
                }
            }

            if (messagesDelete.size() >= 1) {
                try {
                    if (messagesDelete.size() == 1) messagesDelete.get(0).delete().get();
                    else channel.bulkDelete(messagesDelete).get();
                    deleted += messagesDelete.size();
                } catch (ExecutionException e) {
                    LOGGER.error("Could not remove message bulk", e);
                }
            }

            if (messageSet.size() > 0) messageSet = messageSet.getOldestMessage().get().getMessagesBefore(100).get();
        }

        return new Pair<>(deleted, skipped);
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        Optional<ServerTextChannel> channelOptional = slot.getChannel();
        if (channelOptional.isPresent()) {
            if (PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getClass(), channelOptional.get(), Permission.READ_MESSAGE_HISTORY | Permission.MANAGE_MESSAGES)) {
                try {
                    Pair<Integer, Boolean> pair = fullClear(channelOptional.get(), slot.getCommandKey().get(), null);
                    if (pair == null) return TrackerResult.STOP_AND_DELETE;
                } catch (ExecutionException e) {
                    return TrackerResult.STOP_AND_DELETE;
                }
            }
        }

        slot.setNextRequest(Instant.now().plus(1, ChronoUnit.HOURS));
        return TrackerResult.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

}
