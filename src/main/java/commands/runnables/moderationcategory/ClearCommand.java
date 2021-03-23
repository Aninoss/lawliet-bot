package commands.runnables.moderationcategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.TextManager;
import core.cache.PatreonCache;
import core.schedule.MainScheduler;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.ClearResults;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "clear",
        botChannelPermissions = { Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY },
        userChannelPermissions = { Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY },
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
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws InterruptedException {
        if (args.length() > 0 && StringUtil.stringIsLong(args) && Long.parseLong(args) >= 2 && Long.parseLong(args) <= 500) {
            boolean patreon = PatreonCache.getInstance().getUserTier(event.getMember().getIdLong(), true) >= 3 ||
                    PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong());

            addLoadingReactionInstantly();
            ClearResults clearResults = clear(event.getChannel(), patreon, event.getMessage().getIdLong(), Integer.parseInt(args));

            String key = clearResults.getRemaining() > 0 ? "finished_too_old" : "finished_description";
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(key, clearResults.getDeleted() != 1, String.valueOf(clearResults.getDeleted())));
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "deleteTime", "8"));

            if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
                event.getChannel().sendMessage(eb.build())
                        .queue(m -> {
                            if (BotPermissionUtil.can(event.getGuild(), Permission.MESSAGE_MANAGE)) {
                                MainScheduler.getInstance().schedule(8, ChronoUnit.SECONDS, "clear_confirmation_autoremove", () -> event.getChannel().purgeMessages(m, event.getMessage()));
                            }
                        });
            }
            return true;
        } else {
            event.getChannel().sendMessage(
                    EmbedFactory.getEmbedError(this, getString("wrong_args", "2", "500")).build()
            ).queue();
            return false;
        }
    }

    private ClearResults clear(TextChannel channel, boolean patreon, long messageIdIgnore, int count) throws InterruptedException {
        int deleted = 0;
        boolean skipped = false;
        MessageHistory messageHistory = channel.getHistory();

        while (count > 0 && !skipped) {
            /* Check for message date and therefore permissions */
            List<Message> messageList = messageHistory.retrievePast(100).complete();
            if (messageList.size() < 100 && messageList.size() < count) {
                count = messageList.size();
            }

            ArrayList<Message> messagesDelete = new ArrayList<>();
            for (Message message : messageList) {
                if (message.getTimeCreated().toInstant().isBefore(Instant.now().minus(14, ChronoUnit.DAYS))) {
                    skipped = true;
                    break;
                } else if (!message.isPinned() && message.getIdLong() != messageIdIgnore) {
                    messagesDelete.add(message);
                    deleted++;
                    count--;
                    if (count <= 0) {
                        break;
                    }
                }
            }

            if (messagesDelete.size() >= 1) {
                if (messagesDelete.size() == 1) {
                    messagesDelete.get(0).delete().complete();
                } else {
                    channel.deleteMessages(messagesDelete).complete();
                }
            }
        }

        messageHistory = channel.getHistory();
        while (count > 0 && patreon) {
            List<Message> messageList = messageHistory.retrievePast(100).complete();
            if (messageList.size() < 100 && messageList.size() < count) {
                count = messageList.size();
            }

            for (Message message : messageList) {
                if (!message.isPinned() && message.getIdLong() != messageIdIgnore) {
                    message.delete().complete();
                    deleted++;
                    count--;
                    if (count > 0) {
                        TimeUnit.SECONDS.sleep(1);
                    }
                    if (count <= 0) {
                        break;
                    }
                }
            }
        }

        return new ClearResults(deleted, count);
    }

}
