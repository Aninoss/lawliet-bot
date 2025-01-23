package commands.runnables.moderationcategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import commands.listeners.OnButtonListener;
import constants.ExceptionIds;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.mention.MentionList;
import core.utils.*;
import modules.ClearResults;
import modules.schedulers.AlertResponse;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@CommandProperties(
        trigger = "fullclear",
        botChannelPermissions = {Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY},
        userChannelPermissions = {Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY},
        emoji = "\uD83E\uDDF9",
        executableWithoutArgs = true,
        maxCalculationTimeSec = 20 * 60,
        aliases = {"fclear", "allclear", "clearall"}
)
public class FullClearCommand extends Command implements OnAlertListener, OnButtonListener {

    private static final int HOURS_MAX = 20159;
    private static final Random random = new Random();

    private boolean interrupt = false;
    private List<Member> memberFilter;
    GuildMessageChannel channel;

    public FullClearCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws InterruptedException, ExecutionException {
        MentionList<GuildMessageChannel> channelMention = MentionUtil.getGuildMessageChannels(event.getGuild(), args);
        args = channelMention.getFilteredArgs();
        channel = event.getMessageChannel();
        if (!channelMention.getList().isEmpty()) {
            channel = channelMention.getList().get(0);
        }
        EmbedBuilder errEmbed = BotPermissionUtil.getUserAndBotPermissionsMissingEmbed(
                getLocale(),
                channel,
                event.getMember(),
                new Permission[0],
                new Permission[]{Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY},
                new Permission[0],
                new Permission[]{Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY}
        );
        if (errEmbed != null) {
            drawMessageNew(errEmbed).exceptionally(ExceptionLogger.get());
            return false;
        }

        MentionList<Member> memberMention = MentionUtil.getMembers(event.getGuild(), args, null);
        memberFilter = memberMention.getList();
        args = memberMention.getFilteredArgs();
        long hoursMin = Math.max(0, MentionUtil.getAmountExt(args));

        if (hoursMin > HOURS_MAX) {
            drawMessageNew(EmbedFactory.getEmbedError(this, getString("wrong_args", "0", StringUtil.numToString(HOURS_MAX))))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        long messageId = registerButtonListener(event.getMember()).get();
        TimeUnit.SECONDS.sleep(1);

        EntityManagerWrapper entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        BotLogEntity.log(entityManager, BotLogEntity.Event.MOD_FULLCLEAR, event.getMember(), channel.getId());
        entityManager.getTransaction().commit();

        long authorMessageId = event.isMessageReceivedEvent() ? event.getMessageReceivedEvent().getMessage().getIdLong() : 0L;
        ClearResults clearResults = fullClear(channel, (int) hoursMin, memberFilter, authorMessageId, messageId);

        String key = clearResults.getRemaining() > 0 ? "finished_too_old" : "finished_description";
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(key, clearResults.getDeleted() != 1, String.valueOf(clearResults.getDeleted())));
        EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "deleteTime", "8"));

        if (!interrupt) {
            deregisterListenersWithComponents();
            drawMessage(eb).exceptionally(ExceptionLogger.get());
        }

        RestAction<Void> restAction;
        if (event.isMessageReceivedEvent()) {
            restAction = event.getMessageChannel().deleteMessagesByIds(List.of(String.valueOf(messageId), event.getMessageReceivedEvent().getMessage().getId()));
        } else {
            restAction = event.getMessageChannel().deleteMessageById(messageId);
        }
        restAction.submitAfter(8, TimeUnit.SECONDS)
                .exceptionally(ExceptionLogger.get(ExceptionIds.UNKNOWN_MESSAGE, ExceptionIds.UNKNOWN_CHANNEL, ExceptionIds.MISSING_ACCESS, ExceptionIds.THREAD_ARCHIVED));
        return true;
    }

    private void fullClear(GuildMessageChannel channel, int hours) throws InterruptedException {
        fullClear(channel, hours, Collections.emptyList(), 0L);
    }

    private ClearResults fullClear(GuildMessageChannel channel, int hours, List<Member> memberFilter, long... messageIdsIgnore) throws InterruptedException {
        int deleted = 0;
        boolean tooOld = false;

        MessageHistory messageHistory = channel.getHistory();
        do {
            /* Check for message date and therefore permissions */
            List<Message> messageList = messageHistory.retrievePast(100).complete();
            if (messageList.isEmpty() || interrupt) {
                break;
            }

            ArrayList<Message> messagesDelete = new ArrayList<>();
            for (Message message : messageList) {
                if (message.getTimeCreated().toInstant().isBefore(Instant.now().minus(14, ChronoUnit.DAYS))) {
                    tooOld = true;
                    break;
                } else if (!message.isPinned() &&
                        Arrays.stream(messageIdsIgnore).noneMatch(mId -> message.getIdLong() == mId) &&
                        message.getTimeCreated().toInstant().isBefore(Instant.now().minus(hours, ChronoUnit.HOURS)) &&
                        (memberFilter.isEmpty() || memberFilter.contains(message.getMember()))
                ) {
                    messagesDelete.add(message);
                    deleted++;
                }
            }

            if (!messagesDelete.isEmpty()) {
                if (messagesDelete.size() == 1) {
                    messagesDelete.get(0).delete().complete();
                } else {
                    channel.deleteMessages(messagesDelete).complete();
                }
            }

            Thread.sleep(500);
        } while (!tooOld && !interrupt);

        return new ClearResults(deleted, tooOld ? 1 : 0);
    }

    @Override
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        GuildMessageChannel channel = slot.getGuildMessageChannel().get();
        if (PermissionCheckRuntime.botHasPermission(getLocale(), getClass(), channel, Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE)) {
            long hoursMin = Math.max(0, MentionUtil.getAmountExt(slot.getCommandKey()));
            if (hoursMin <= HOURS_MAX) {
                try {
                    fullClear(channel, (int) hoursMin);
                    if (slot.getEffectiveUserMessage(getLocale()).isPresent() && !slot.getEffectiveUserMessage(getLocale()).get().isBlank()) {
                        slot.sendMessage(getLocale(), true, "");
                    }
                } catch (InterruptedException e) {
                    //ignore
                }
                slot.setNextRequest(Instant.now().plus(60 + random.nextInt(120), ChronoUnit.MINUTES));
                return AlertResponse.CONTINUE_AND_SAVE;
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("wrong_args", "0", StringUtil.numToString(HOURS_MAX)));
                slot.sendMessage(getLocale(), false, eb.build());
            }
        }

        return AlertResponse.STOP_AND_DELETE;
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        deregisterListenersWithComponents();
        interrupt = true;
        return true;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        if (!interrupt) {
            setComponents(Button.of(ButtonStyle.SECONDARY, "cancel", TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort")));
            if (memberFilter.isEmpty()) {
                return EmbedFactory.getEmbedDefault(this, getString("progress", channel.getAsMention(), EmojiUtil.getLoadingEmojiMention(getGuildMessageChannel().get())));
            } else {
                return EmbedFactory.getEmbedDefault(this, getString("progress_filter", MentionUtil.getMentionedStringOfMembers(getLocale(), memberFilter).getMentionText(), channel.getAsMention(), EmojiUtil.getLoadingEmojiMention(getGuildMessageChannel().get())));
            }
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort_description"));
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "deleteTime", "8"));
            return eb;
        }
    }

}
