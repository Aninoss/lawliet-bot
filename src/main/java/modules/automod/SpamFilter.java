package modules.automod;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import commands.Category;
import commands.Command;
import commands.runnables.moderationcategory.SpamFilterCommand;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import javafx.util.Pair;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.SpamFilterEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SpamFilter extends AutoModAbstract {

    public static final int EVENTS_SECONDS = 2;
    public static final int EVENTS_NUMBER = 5;

    private static final LoadingCache<Pair<Long, Long>, MemberEvents> memberEventsCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(EVENTS_SECONDS))
            .build(new CacheLoader<>() {
                @Override
                public MemberEvents load(@NotNull Pair<Long, Long> key) {
                    return new MemberEvents();
                }
            });

    private final SpamFilterEntity spamFilterEntity;

    public SpamFilter(Message message, GuildEntity guildEntity) {
        super(message, guildEntity);
        spamFilterEntity = guildEntity.getSpamFilter();
    }

    @Override
    protected void punish(Message message, Member member, GuildEntity guildEntity, Class<? extends Command> commandClass) {
        try {
            if (memberEventsCache.get(new Pair<>(member.getGuild().getIdLong(), member.getIdLong())).checkNotYetPunished()) {
                super.punish(message, member, guildEntity, commandClass);
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean willBanMember(Message message, Member member, Locale locale) {
        return spamFilterEntity.getAction() == SpamFilterEntity.Action.BAN_USER &&
                PermissionCheckRuntime.botHasPermission(locale, getCommandClass(), message.getGuildChannel(), Permission.BAN_MEMBERS) &&
                member.getGuild().getSelfMember().canInteract(member);
    }

    @Override
    protected boolean withAutoActions(Message message, Member member, Locale locale) {
        if (willBanMember(message, member, locale)) {
            message.getGuild()
                    .ban(member, 0, TimeUnit.DAYS)
                    .reason(TextManager.getString(locale, Category.MODERATION, "spamfilter_auditlog_sp"))
                    .queue();
            return false;
        } else if (spamFilterEntity.getAction() == SpamFilterEntity.Action.KICK_USER &&
                PermissionCheckRuntime.botHasPermission(locale, getCommandClass(), message.getGuildChannel(), Permission.KICK_MEMBERS) &&
                member.getGuild().getSelfMember().canInteract(member)
        ) {
            message.getGuild()
                    .kick(member)
                    .reason(TextManager.getString(locale, Category.MODERATION, "spamfilter_auditlog_sp"))
                    .queue();
            return false;
        }

        return true;
    }

    @Override
    protected void designEmbed(Message message, Member member, Locale locale, EmbedBuilder eb) {
        String content = JDAUtil.combineMessageContentRaw(message);
        eb.setDescription(TextManager.getString(locale, Category.MODERATION, "spamfilter_log", StringUtil.escapeMarkdown(member.getUser().getName())))
                .addField(TextManager.getString(locale, Category.MODERATION, "spamfilter_state0_maction"), TextManager.getString(locale, Category.MODERATION, "spamfilter_state0_mactionlist").split("\n")[spamFilterEntity.getAction().ordinal()], true)
                .addField(TextManager.getString(locale, Category.MODERATION, "spamfilter_log_channel"), message.getChannel().getAsMention(), true)
                .addField(TextManager.getString(locale, Category.MODERATION, "spamfilter_log_content"), StringUtil.shortenString(content, 1024), false);

        for (Long userId : spamFilterEntity.getLogReceiverUserIds()) {
            if (userId != message.getGuild().getSelfMember().getIdLong()) {
                JDAUtil.openPrivateChannel(message.getJDA(), userId)
                        .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                        .queue();
            }
        }
    }

    @Override
    protected Class<? extends Command> getCommandClass() {
        return SpamFilterCommand.class;
    }

    @Override
    protected boolean checkCondition(Message message, Member member) {
        try {
            return spamFilterEntity.getActive() &&
                    !spamFilterEntity.getExcludedMemberIds().contains(member.getIdLong()) &&
                    !JDAUtil.collectionContainsChannelOrParent(spamFilterEntity.getExcludedChannelIds(), message.getChannel()) &&
                    !BotPermissionUtil.can(member, Permission.ADMINISTRATOR) &&
                    memberEventsCache.get(new Pair<>(member.getGuild().getIdLong(), member.getIdLong())).checkAndSet(message.getContentRaw());
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    private static class MemberEvents {

        private final ArrayList<Instant> events = new ArrayList<>();
        private String messageContent = null;
        private boolean notYetPunished = true;

        public synchronized boolean checkAndSet(String newMessageContent) {
            if (!newMessageContent.equals(messageContent)) {
                messageContent = newMessageContent;
                events.clear();
            }

            boolean valid = false;
            if (events.size() >= EVENTS_NUMBER) {
                Instant firstOccurrence = events.get(0);
                if (TimeUtil.getMillisBetweenInstants(firstOccurrence, Instant.now()) < EVENTS_SECONDS * 1000) {
                    valid = true;
                }
                events.remove(0);
            }

            events.add(Instant.now());
            return valid;
        }

        public synchronized boolean checkNotYetPunished() {
            boolean value = notYetPunished;
            notYetPunished = false;
            return value;
        }

    }

}
