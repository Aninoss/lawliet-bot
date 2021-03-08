package commands.runnables.moderationcategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.TextManager;
import core.cache.PatreonCache;
import core.schedule.MainScheduler;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.ClearResults;
import net.dv8tion.jda.api.EmbedBuilder;

@CommandProperties(
        trigger = "clear",
        botPermissions = PermissionDeprecated.MANAGE_MESSAGES | PermissionDeprecated.READ_MESSAGE_HISTORY,
        userPermissions = PermissionDeprecated.MANAGE_MESSAGES | PermissionDeprecated.READ_MESSAGE_HISTORY,
        withLoadingBar = true,
        emoji = "\uD83D\uDDD1\uFE0F",
        maxCalculationTimeSec = 10 * 60,
        executableWithoutArgs = false,
        aliases = { "clean", "purge" }
)
public class ClearCommand extends Command {

    public ClearCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        if (args.length() > 0 && StringUtil.stringIsLong(args) && Long.parseLong(args) >= 2 && Long.parseLong(args) <= 500) {
            boolean patreon = PatreonCache.getInstance().getUserTier(event.getMessageAuthor().getId()) >= 3;
            ClearResults clearResults = clear(event.getServerTextChannel().get(), patreon, event.getMessage(), Integer.parseInt(args));

            String key = clearResults.getRemaining() > 0 ? "finished_too_old" : "finished_description";
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(key, clearResults.getDeleted() != 1, String.valueOf(clearResults.getDeleted())));
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "deleteTime", "8"));
            if (event.getChannel().getCurrentCachedInstance().isPresent() && event.getChannel().canYouSee() && event.getChannel().canYouWrite() && event.getChannel().canYouEmbedLinks()) {
                Message m = event.getChannel().sendMessage(eb).get();
                MainScheduler.getInstance().schedule(8, ChronoUnit.SECONDS, "clear_confirmation_autoremove", () -> event.getChannel().bulkDelete(m, event.getMessage()));
            }
            return true;
        } else {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(
                    this,
                    getString("wrong_args", "2", "500")
            )).get();
            return false;
        }
    }

    private ClearResults clear(ServerTextChannel channel, boolean patreon, Message messageBefore, int count) throws ExecutionException, InterruptedException {
        int deleted = 0;
        boolean skipped = false;
        while (count > 0 && !skipped) {
            //Check for message date and therefore permissions
            MessageSet messageSet = messageBefore.getMessagesBefore(100).get();
            if (messageSet.size() < 100 && messageSet.size() < count)
                count = messageSet.size();

            ArrayList<Message> messagesDelete = new ArrayList<>();
            for (Message message : messageSet.descendingSet()) {
                if (message.getCreationTimestamp().isBefore(Instant.now().minus(14, ChronoUnit.DAYS))) {
                    skipped = true;
                    break;
                } else if (!message.isPinned()) {
                    messagesDelete.add(message);
                    deleted++;
                    count--;
                    if (count <= 0)
                        break;
                }
            }

            if (messagesDelete.size() >= 1) {
                if (messagesDelete.size() == 1)
                    messagesDelete.get(0).delete().get();
                else channel.bulkDelete(messagesDelete).get();
            }
        }

        while (count > 0 && patreon) {
            MessageSet messageSet = messageBefore.getMessagesBefore(100).get();
            if (messageSet.size() < 100 && messageSet.size() < count)
                count = messageSet.size();

            ArrayList<Message> messagesDelete = new ArrayList<>(messageSet.descendingSet());
            for (Message message : messagesDelete) {
                if (!message.isPinned()) {
                    message.delete().get();
                    deleted++;
                    count--;
                    if (count > 0)
                        TimeUnit.SECONDS.sleep(1);
                    if (count <= 0)
                        break;
                }
            }
        }

        return new ClearResults(deleted, count);
    }

    private void startCountdown(ServerTextChannel channel, Message[] messagesArray) {
        MainScheduler.getInstance().schedule(8, ChronoUnit.SECONDS, "clear_confirmation_autoremove", () -> channel.bulkDelete(messagesArray));
    }

}
