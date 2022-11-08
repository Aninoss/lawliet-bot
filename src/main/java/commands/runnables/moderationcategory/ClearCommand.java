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
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.cache.PatreonCache;
import core.mention.MentionList;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.EmojiUtil;
import core.utils.MentionUtil;
import modules.ClearResults;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "clear",
        botChannelPermissions = { Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY },
        userChannelPermissions = { Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY },
        emoji = "\uD83D\uDDD1\uFE0F",
        maxCalculationTimeSec = 20 * 60,
        executableWithoutArgs = false,
        aliases = { "clean", "purge" }
)
public class ClearCommand extends Command implements OnButtonListener {

    private boolean interrupt = false;
    private List<Member> memberFilter;
    private long amount;
    StandardGuildMessageChannel channel;

    public ClearCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws InterruptedException, ExecutionException {
        MentionList<StandardGuildMessageChannel> channelMention = MentionUtil.getStandardGuildMessageChannels(event.getGuild(), args);
        args = channelMention.getFilteredArgs();
        channel = event.getTextChannel();
        if (channelMention.getList().size() > 0) {
            channel = channelMention.getList().get(0);
        }
        EmbedBuilder errEmbed = BotPermissionUtil.getUserAndBotPermissionMissingEmbed(
                getLocale(),
                channel,
                event.getMember(),
                new Permission[0],
                new Permission[] { Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY },
                new Permission[0],
                new Permission[] { Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY },
                new Permission[0]
        );
        if (errEmbed != null) {
            drawMessageNew(errEmbed).exceptionally(ExceptionLogger.get());
            return false;
        }

        MentionList<Member> memberMention = MentionUtil.getMembers(event.getGuild(), args, null);
        memberFilter = memberMention.getList();
        args = memberMention.getFilteredArgs();
        amount = MentionUtil.getAmountExt(args);

        if (amount < 2 || amount > 500) {
            drawMessageNew(EmbedFactory.getEmbedError(this, getString("wrong_args", "2", "500")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        boolean patreon = PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), true) ||
                PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong());

        long messageId = registerButtonListener(event.getMember()).get();
        TimeUnit.SECONDS.sleep(1);
        long authorMessageId = event.isMessageReceivedEvent() ? event.getMessageReceivedEvent().getMessage().getIdLong() : 0L;
        ClearResults clearResults = clear(channel, patreon, (int) amount, memberMention.getList(), authorMessageId, messageId);

        String key = clearResults.getRemaining() > 0 ? "finished_too_old" : "finished_description";
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(key, clearResults.getDeleted() != 1, String.valueOf(clearResults.getDeleted())));
        EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "deleteTime", "8"));

        if (!interrupt) {
            deregisterListenersWithComponents();
            drawMessage(eb).exceptionally(ExceptionLogger.get());
        }

        RestAction<Void> restAction;
        if (event.isMessageReceivedEvent()) {
            restAction = event.getTextChannel().deleteMessagesByIds(List.of(String.valueOf(messageId), event.getMessageReceivedEvent().getMessage().getId()));
        } else {
            restAction = event.getTextChannel().deleteMessageById(messageId);
        }
        restAction.queueAfter(8, TimeUnit.SECONDS);
        return true;
    }

    private ClearResults clear(StandardGuildMessageChannel channel, boolean patreon, int count, List<Member> memberFilter, long... messageIdsIgnore) throws InterruptedException {
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
                } else if (Arrays.stream(messageIdsIgnore).noneMatch(mId -> message.getIdLong() == mId)) {
                    if (!message.isPinned() &&
                            (memberFilter.isEmpty() || memberFilter.contains(message.getMember()))
                    ) {
                        messagesDelete.add(message);
                        deleted++;
                    }
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
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        deregisterListenersWithComponents();
        interrupt = true;
        return true;
    }

    @Override
    public EmbedBuilder draw(@NotNull Member member) throws Throwable {
        if (!interrupt) {
            setComponents(Button.of(ButtonStyle.SECONDARY, "cancel", TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort")));
            if (memberFilter.isEmpty()) {
                return EmbedFactory.getEmbedDefault(this, getString("progress", String.valueOf(amount), channel.getAsMention(), EmojiUtil.getLoadingEmojiMention(getTextChannel().get())));
            } else {
                return EmbedFactory.getEmbedDefault(this, getString("progress_filter", String.valueOf(amount), MentionUtil.getMentionedStringOfMembers(getLocale(), memberFilter).getMentionText(), channel.getAsMention(), EmojiUtil.getLoadingEmojiMention(getTextChannel().get())));
            }
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort_description"));
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "deleteTime", "8"));
            return eb;
        }
    }

}
