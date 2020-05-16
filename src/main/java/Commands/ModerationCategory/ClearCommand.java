package Commands.ModerationCategory;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.Permission;
import Core.CustomThread;
import Core.EmbedFactory;
import Core.TextManager;
import Core.Utils.StringUtil;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "clear",
        botPermissions = Permission.MANAGE_MESSAGES | Permission.READ_MESSAGE_HISTORY,
        userPermissions = Permission.MANAGE_MESSAGES | Permission.READ_MESSAGE_HISTORY,
        withLoadingBar = true,
        emoji = "\uD83D\uDDD1\uFE0F",
        maxCalculationTimeSec = 5 * 60,
        executable = false
)
public class ClearCommand extends Command {

    final static Logger LOGGER = LoggerFactory.getLogger(ClearCommand.class);

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        if (followedString.length() > 0 && StringUtil.stringIsLong(followedString) && Long.parseLong(followedString) >= 2 && Long.parseLong(followedString) <= 500) {
            int count = Integer.parseInt(followedString);
            int deleted = 0;
            boolean skipped = false;

            while(count > 0) {
                //Check for message date and therefore permissions
                MessageSet messageSet = event.getMessage().getMessagesBefore(Math.min(100, count)).get();
                if (messageSet.size() < 100) count = messageSet.size();
                ArrayList<Message> messagesDelete = new ArrayList<>();
                for (Message message : messageSet.descendingSet()) {
                    if (message.getCreationTimestamp().isBefore(Instant.now().minus(14, ChronoUnit.DAYS))) {
                        skipped = true;
                    } else {
                        messagesDelete.add(message);
                    }
                }

                if (messagesDelete.size() >= 1) {
                    try {
                        if (messagesDelete.size() == 1) messagesDelete.get(0).delete().get();
                        else event.getChannel().bulkDelete(messagesDelete).get();
                        deleted += messagesDelete.size();
                    } catch (ExecutionException e) {
                        LOGGER.error("Could not delete message", e);
                    }
                }

                count -= messageSet.size();
            }

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
            }, "clear_countdown", 1);
            t.start();
            return true;
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    getString("wrong_args", "2", "500"))).get();
            return false;
        }
    }
}
