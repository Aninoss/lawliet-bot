package commands.runnables.moderationcategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import core.EmbedFactory;
import core.TextManager;
import core.cache.PatreonCache;
import core.utils.EmbedUtil;
import core.utils.EmojiUtil;
import core.utils.StringUtil;
import modules.ClearResults;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

@CommandProperties(
        trigger = "clear",
        botChannelPermissions = { Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY },
        userChannelPermissions = { Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY },
        emoji = "\uD83D\uDDD1\uFE0F",
        maxCalculationTimeSec = 20 * 60,
        executableWithoutArgs = false,
        turnOffLoadingReaction = true,
        aliases = { "clean", "purge" }
)
public class ClearCommand extends Command implements OnButtonListener {

    private boolean interrupt = false;

    public ClearCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws InterruptedException, ExecutionException {
        if (args.length() > 0 && StringUtil.stringIsLong(args) && Long.parseLong(args) >= 2 && Long.parseLong(args) <= 500) {
            boolean patreon = PatreonCache.getInstance().getUserTier(event.getMember().getIdLong(), true) >= 3 ||
                    PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong());

            long messageId = registerButtonListener(event.getMember()).get();
            TimeUnit.SECONDS.sleep(1);
            ClearResults clearResults = clear(event.getChannel(), patreon, Integer.parseInt(args), event.getMessage().getIdLong(), messageId);

            String key = clearResults.getRemaining() > 0 ? "finished_too_old" : "finished_description";
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(key, clearResults.getDeleted() != 1, String.valueOf(clearResults.getDeleted())));
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "deleteTime", "8"));

            if (!interrupt) {
                deregisterListenersWithComponents();
                drawMessage(eb);
            }

            event.getChannel().deleteMessagesByIds(List.of(String.valueOf(messageId), event.getMessage().getId()))
                    .queueAfter(8, TimeUnit.SECONDS);
            return true;
        } else {
            event.getChannel().sendMessageEmbeds(
                    EmbedFactory.getEmbedError(this, getString("wrong_args", "2", "500")).build()
            ).queue();
            return false;
        }
    }

    private ClearResults clear(TextChannel channel, boolean patreon, int count, long... messageIdsIgnore) throws InterruptedException {
        int deleted = 0;
        boolean skipped = false;
        MessageHistory messageHistory = channel.getHistory();

        while (count > 0 && !skipped && !interrupt) {
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
                } else if (!message.isPinned() && Arrays.stream(messageIdsIgnore).noneMatch(mId -> message.getIdLong() == mId)) {
                    messagesDelete.add(message);
                    deleted++;
                    count--;
                    if (count <= 0) {
                        break;
                    }
                }
            }

            if (messagesDelete.size() >= 1 && !interrupt) {
                if (messagesDelete.size() == 1) {
                    messagesDelete.get(0).delete().complete();
                } else {
                    channel.deleteMessages(messagesDelete).complete();
                }
            }

            Thread.sleep(500);
        }

        if (count > 0 && patreon) {
            messageHistory = channel.getHistory();
            while (count > 0 && !interrupt) {
                List<Message> messageList = messageHistory.retrievePast(100).complete();
                if (messageList.size() < 100 && messageList.size() < count) {
                    count = messageList.size();
                }

                for (Message message : messageList) {
                    if (!message.isPinned() && Arrays.stream(messageIdsIgnore).noneMatch(mId -> message.getIdLong() == mId)) {
                        message.delete().complete();
                        deleted++;
                        count--;
                        if (count > 0) {
                            TimeUnit.SECONDS.sleep(1);
                        }
                        if (count <= 0 || interrupt) {
                            break;
                        }
                    }
                }

                Thread.sleep(500);
            }
        }

        return new ClearResults(deleted, count);
    }

    @Override
    public boolean onButton(ButtonClickEvent event) throws Throwable {
        deregisterListenersWithComponents();
        interrupt = true;
        return true;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        if (!interrupt) {
            setComponents(Button.of(ButtonStyle.SECONDARY, "cancel", TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort")));
            return EmbedFactory.getEmbedDefault(this, getString("progress", EmojiUtil.getLoadingEmojiMention(getTextChannel().get())));
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort_description"));
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "deleteTime", "8"));
            return eb;
        }
    }

}
