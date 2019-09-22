package Commands.Moderation;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.Permission;
import General.EmbedFactory;
import General.TextManager;
import General.Tools;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@CommandProperties(
    trigger = "clear",
    botPermissions = Permission.MANAGE_MASSAGES_IN_TEXT_CHANNEL | Permission.READ_MESSAGE_HISTORY_OF_TEXT_CHANNEL,
    userPermissions = Permission.MANAGE_MASSAGES_IN_TEXT_CHANNEL | Permission.READ_MESSAGE_HISTORY_OF_TEXT_CHANNEL,
    withLoadingBar = true,
    emoji = "\uD83D\uDDD1\uFE0F",
    thumbnail = "http://icons.iconarchive.com/icons/graphicloads/colorful-long-shadow/128/Recyclebin-icon.png",
    executable = false
)
public class ClearCommand extends Command implements onRecievedListener {

    public ClearCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        if (followedString.length() > 0 && Tools.stringIsNumeric(followedString) && Long.parseLong(followedString) >= 1 && Long.parseLong(followedString) <= 500) {
            int n = Integer.parseInt(followedString);
            int count = n;
            int deleted = 0;

            while(count > 0) {
                //Check for message date and therefore permissions
                MessageSet messageSet = event.getMessage().getMessagesBefore(Math.min(100, count)).get();
                if (messageSet.size() < 100) count = messageSet.size();
                ArrayList<Message> messagesDelete = new ArrayList<>();
                for (Message message : messageSet.descendingSet()) {
                    if (message.getCreationTimestamp().isBefore(Instant.now().minus(14, ChronoUnit.DAYS))) {
                        try {
                            message.delete().get();
                            deleted++;
                        } catch (InterruptedException | ExecutionException e) {
                            //Ignore
                        }
                    } else {
                        messagesDelete.add(message);
                    }
                }

                if (messagesDelete.size() >= 2) {
                    try {
                        event.getChannel().bulkDelete(messagesDelete).get();
                        deleted += messagesDelete.size();
                    } catch (InterruptedException | ExecutionException e) {
                        //Ignore
                    }
                }
                count -= messageSet.size();
            }

            Message m = event.getChannel().sendMessage(EmbedFactory.getCommandEmbedSuccess(this, getString("finished_description", deleted != 1, String.valueOf(deleted)))
                    .setFooter(TextManager.getString(getLocale(), TextManager.GENERAL, "deleteTime", "10"))).get();
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Message[] messagesArray = new Message[]{m,event.getMessage()};
                event.getChannel().bulkDelete(messagesArray);
            }).start();
            return true;
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    getString("wrong_args"))).get();
            return false;
        }
    }
}
