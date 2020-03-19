package Commands.ModerationCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandListeners.onTrackerRequestListener;
import CommandSupporters.Command;
import Constants.Permission;
import General.*;
import General.Tracker.TrackerData;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.event.message.MessageCreateEvent;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "fullclear",
        botPermissions = Permission.MANAGE_MASSAGES_IN_TEXT_CHANNEL | Permission.READ_MESSAGE_HISTORY_OF_TEXT_CHANNEL,
        userPermissions = Permission.MANAGE_MASSAGES_IN_TEXT_CHANNEL | Permission.READ_MESSAGE_HISTORY_OF_TEXT_CHANNEL,
        withLoadingBar = true,
        emoji = "\uD83E\uDDF9",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/colorful-long-shadow/128/Recyclebin-icon.png",
        executable = false,
        aliases = {"fclear"}
)
public class FullClearCommand extends Command implements onRecievedListener, onTrackerRequestListener {

    public FullClearCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        Pair<Integer, Boolean> pair = fullClear(event.getServerTextChannel().get(), followedString, event.getMessage());
        if (pair == null) return false;
        boolean skipped = pair.getValue();
        int deleted = pair.getKey();

        String key = skipped ? "finished_too_old" : "finished_description";
        Message m = event.getChannel().sendMessage(EmbedFactory.getCommandEmbedSuccess(this, getString(key, deleted != 1, String.valueOf(deleted)))
                .setFooter(TextManager.getString(getLocale(), TextManager.GENERAL, "deleteTime", "8"))).get();
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(8000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Message[] messagesArray = new Message[]{m, event.getMessage()};
            event.getChannel().bulkDelete(messagesArray);
        });
        t.setName("fullclear_countdown");
        t.start();
        return true;
    }

    private Pair<Integer, Boolean> fullClear(ServerTextChannel channel, String str, Message messageBefore) throws ExecutionException, InterruptedException, IOException {
        int hours = 0;
        if (str.length() > 0) {
            if (Tools.stringIsLong(str) && Long.parseLong(str) >= 0 && Long.parseLong(str) <= 20159) {
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
                if (message.getCreationTimestamp().isBefore(Instant.now().minus(14, ChronoUnit.DAYS))) {
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
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            if (messageSet.size() > 0) messageSet = messageSet.getOldestMessage().get().getMessagesBefore(100).get();
        }

        return new Pair<>(deleted, skipped);
    }

    @Override
    public TrackerData onTrackerRequest(TrackerData trackerData) throws Throwable {
        Optional<ServerTextChannel> channelOptional = trackerData.getChannel();
        if (channelOptional.isPresent()) {
            if (PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getTrigger(), channelOptional.get(), Permission.MANAGE_MASSAGES_IN_TEXT_CHANNEL)) {
                Pair<Integer, Boolean> pair = fullClear(trackerData.getChannel().get(), trackerData.getKey(), null);
                if (pair == null) return null;
            }
        }
        trackerData.setInstant(Instant.now().plus(1, ChronoUnit.HOURS));
        return trackerData;
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

    @Override
    public boolean needsPrefix() {
        return false;
    }
}
