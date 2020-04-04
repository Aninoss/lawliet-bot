package Commands.ModerationCategory;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.Permission;
import General.EmbedFactory;
import General.TextManager;
import General.Tools.StringTools;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.event.message.MessageCreateEvent;
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
    thumbnail = "http://icons.iconarchive.com/icons/graphicloads/colorful-long-shadow/128/Recyclebin-icon.png",
    executable = false
)
public class ClearCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        if (followedString.length() > 0 && StringTools.stringIsLong(followedString) && Long.parseLong(followedString) >= 2 && Long.parseLong(followedString) <= 500) {
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
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }

                count -= messageSet.size();
            }

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
            t.setName("clear_countdown");
            t.start();
            return true;
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    getString("wrong_args", "2", "500"))).get();
            return false;
        }
    }
}
