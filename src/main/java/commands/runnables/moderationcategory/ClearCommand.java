package commands.runnables.moderationcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import constants.Permission;
import core.EmbedFactory;
import core.TextManager;
import core.schedule.MainScheduler;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "clear",
        botPermissions = Permission.MANAGE_MESSAGES | Permission.READ_MESSAGE_HISTORY,
        userPermissions = Permission.MANAGE_MESSAGES | Permission.READ_MESSAGE_HISTORY,
        withLoadingBar = true,
        emoji = "\uD83D\uDDD1\uFE0F",
        maxCalculationTimeSec = 3 * 60,
        executableWithoutArgs = false
)
public class ClearCommand extends Command {

    private final static Logger LOGGER = LoggerFactory.getLogger(ClearCommand.class);

    public ClearCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        if (followedString.length() > 0 && StringUtil.stringIsLong(followedString) && Long.parseLong(followedString) >= 2 && Long.parseLong(followedString) <= 500) {
            int count = Integer.parseInt(followedString);
            int deleted = 0;
            boolean skipped = false;

            while(count > 0 && !skipped) {
                //Check for message date and therefore permissions
                MessageSet messageSet = event.getMessage().getMessagesBefore(Math.min(100, count)).get();
                if (messageSet.size() < 100)
                    count = messageSet.size();

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
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(key, deleted != 1, String.valueOf(deleted)));
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "deleteTime", "8"));
            event.getChannel().sendMessage(eb)
                    .exceptionally(ExceptionLogger.get())
                    .thenAccept(message -> startCountdown(event.getServerTextChannel().get(), new Message[]{ message, event.getMessage() }));
            return true;
        } else {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    getString("wrong_args", "2", "500"))).get();
            return false;
        }
    }

    private void startCountdown(ServerTextChannel channel, Message[] messagesArray) {
        MainScheduler.getInstance().schedule(8, ChronoUnit.SECONDS, "clear_confirmation_autoremove", () -> channel.bulkDelete(messagesArray));
    }

}
