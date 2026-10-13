package commands;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import constants.ExceptionIds;
import constants.ExternalLinks;
import constants.Settings;
import core.ExceptionLogger;
import core.MainLogger;
import core.Program;
import core.ShardManager;
import core.TextManager;
import core.cache.ServerPatreonBoostCache;
import core.patreon.PatreonCache;
import core.schedule.MainScheduler;
import core.utils.BotPermissionUtil;
import core.utils.ComponentsUtil;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.user.UserEntity;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.entities.Member;

import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Locale;

/**
 * Decides if and when users are shown a bot invite prompt or a Pro subscription hint
 * after a successfully executed command.
 * <p>
 * Prompts are never shown in servers unlocked with a Pro subscription. Invite prompts are
 * additionally never shown to subscribers. The time of the last prompt is persisted per user
 * so that a single user is only prompted rarely.
 */
public class PromoManager {

    private enum PromoType {
        INVITE,
        PRO_CONFIG,
        PRO_FISHERY
    }

    private static final Duration ACTIVITY_WINDOW = Duration.ofHours(6);
    private static final Duration INVITE_INTERVAL = Duration.ofDays(30);
    private static final Duration PRO_INTERVAL = Duration.ofDays(21);
    private static final Duration ANY_PROMO_INTERVAL = Duration.ofDays(3);
    private static final Duration COOLDOWN_HINT_INTERVAL = Duration.ofHours(6);

    private static final int PROMO_DELETE_SEC = 60;
    private static final int INVITE_MIN_ENGAGING_COMMANDS = 5;
    private static final int INVITE_MIN_COMMANDS_MILESTONE = 25;
    private static final int PRO_FISHERY_MIN_COMMANDS = 8;
    private static final int PRO_CONFIG_MIN_COMMANDS = 3;
    private static final int PRO_CONFIG_MIN_MEMBERS = 20;

    private static final EnumSet<Category> INVITE_ENGAGING_CATEGORIES = EnumSet.of(
            Category.FISHERY, Category.CASINO, Category.GIMMICKS, Category.AI_TOYS, Category.INTERACTIONS,
            Category.NSFW, Category.NSFW_INTERACTIONS, Category.EXTERNAL
    );

    private static final Cache<Long, UserActivity> activityCache = CacheBuilder.newBuilder()
            .expireAfterAccess(ACTIVITY_WINDOW)
            .build();

    private static final Cache<Long, Boolean> cooldownHintCache = CacheBuilder.newBuilder()
            .expireAfterWrite(COOLDOWN_HINT_INTERVAL)
            .build();

    private static class UserActivity {

        private int total = 0;
        private int engaging = 0;
        private int fishery = 0;
        private int config = 0;

    }

    /**
     * Should be called after a command has been completed successfully.
     * Never throws.
     */
    public static void onCommandSuccess(CommandEvent event, Command command) {
        if (!Program.publicInstance()) {
            return;
        }

        try {
            process(event, command);
        } catch (Throwable e) {
            MainLogger.get().error("Error in promo manager", e);
        }
    }

    /**
     * Returns whether the cooldown message of this user should include a Pro hint.
     * Cooldowns do not apply to Pro users or unlocked servers, so this is always relevant,
     * but it is only shown rarely per user.
     */
    public static boolean shouldAddProHintToCooldown(long userId) {
        if (!Program.publicInstance()) {
            return false;
        }

        synchronized (cooldownHintCache) {
            if (cooldownHintCache.getIfPresent(userId) != null) {
                return false;
            }
            cooldownHintCache.put(userId, true);
            return true;
        }
    }

    private static void process(CommandEvent event, Command command) {
        Member member = event.getMember();
        long userId = member.getIdLong();
        long guildId = event.getGuild().getIdLong();
        Category category = command.getCategory();

        UserActivity activity = recordActivity(userId, category);

        if (member.getUser().isBot() ||
                !BotPermissionUtil.canWriteEmbed(event.getMessageChannel()) ||
                PatreonCache.getInstance().isUnlocked(guildId)
        ) {
            return;
        }

        boolean subscriber = PatreonCache.getInstance().hasPremium(userId, false);
        boolean canManageServer = BotPermissionUtil.can(member, Permission.MANAGE_SERVER);

        PromoType[] candidates = new PromoType[3];
        int n = 0;
        if (category == Category.FISHERY && !subscriber && activity.fishery >= PRO_FISHERY_MIN_COMMANDS) {
            candidates[n++] = PromoType.PRO_FISHERY;
        }
        if ((category == Category.CONFIGURATION || category == Category.MODERATION) &&
                canManageServer &&
                activity.config >= PRO_CONFIG_MIN_COMMANDS &&
                event.getGuild().getMemberCount() >= PRO_CONFIG_MIN_MEMBERS &&
                !ServerPatreonBoostCache.get(guildId)
        ) {
            candidates[n++] = PromoType.PRO_CONFIG;
        }

        if (!subscriber &&
                !BotPermissionUtil.can(member, Permission.MANAGE_SERVER, Permission.MESSAGE_MANAGE) &&
                isInviteMoment(category, activity)
        ) {
            candidates[n++] = PromoType.INVITE;
        }
        if (n == 0) {
            return;
        }

        PromoType selected = null;
        try (UserEntity userEntity = HibernateManager.findUserEntity(userId, PromoManager.class)) {
            Instant now = Instant.now();
            for (int i = 0; i < n && selected == null; i++) {
                if (isAllowedByHistory(userEntity, candidates[i], now)) {
                    selected = candidates[i];
                }
            }

            if (selected == null) {
                return;
            }

            userEntity.beginTransaction();
            if (selected == PromoType.INVITE) {
                userEntity.setLastInvitePromptInstant(now);
            } else {
                userEntity.setLastProPromptInstant(now);
            }
            userEntity.commitTransaction();
        }

        activityCache.invalidate(userId);
        send(event, command.getLocale(), command.getGuildEntity(), selected);
    }

    private static boolean isInviteMoment(Category category, UserActivity activity) {
        if (category != null && INVITE_ENGAGING_CATEGORIES.contains(category) && activity.engaging >= INVITE_MIN_ENGAGING_COMMANDS) {
            return true;
        }
        return category != Category.INFORMATION && activity.total >= INVITE_MIN_COMMANDS_MILESTONE;
    }

    private static boolean isAllowedByHistory(UserEntity userEntity, PromoType type, Instant now) {
        Instant lastInvite = userEntity.getLastInvitePromptInstant();
        Instant lastPro = userEntity.getLastProPromptInstant();

        if (isRecent(lastInvite, ANY_PROMO_INTERVAL, now) || isRecent(lastPro, ANY_PROMO_INTERVAL, now)) {
            return false;
        }
        if (type == PromoType.INVITE) {
            return !isRecent(lastInvite, INVITE_INTERVAL, now);
        }
        return !isRecent(lastPro, PRO_INTERVAL, now);
    }

    private static boolean isRecent(Instant instant, Duration duration, Instant now) {
        return instant != null && instant.plus(duration).isAfter(now);
    }

    private static UserActivity recordActivity(long userId, Category category) {
        UserActivity activity;
        synchronized (activityCache) {
            activity = activityCache.getIfPresent(userId);
            if (activity == null) {
                activity = new UserActivity();
                activityCache.put(userId, activity);
            }

            activity.total++;
            if (category != null && INVITE_ENGAGING_CATEGORIES.contains(category)) {
                activity.engaging++;
            }
            if (category == Category.FISHERY) {
                activity.fishery++;
            }
            if (category == Category.CONFIGURATION || category == Category.MODERATION) {
                activity.config++;
            }
        }
        return activity;
    }

    private static MessageComponentTree buildComponentTree(Locale locale, GuildEntity guildEntity, PromoType type, boolean autoDelete) {
        ArrayList<ContainerChildComponent> components = new ArrayList<>();
        Color color;
        Button button;

        if (type == PromoType.INVITE) {
            color = ComponentsUtil.DEFAULT_CONTAINER_COLOR;
            button = Button.of(ButtonStyle.LINK, ExternalLinks.BOT_INVITE_REMINDER_URL, TextManager.getString(locale, TextManager.GENERAL, "invite_button"));

            TextDisplay text = TextDisplay.of(TextManager.getString(locale, TextManager.GENERAL, "invite"));
            String avatarUrl = ShardManager.getSelf().getAvatarUrl();
            if (avatarUrl != null) {
                components.add(Section.of(Thumbnail.fromUrl(avatarUrl), text));
            } else {
                components.add(text);
            }
        } else {
            color = Settings.PREMIUM_COLOR;
            button = ComponentsUtil.getPatreonButton(locale);

            String key = type == PromoType.PRO_FISHERY ? "pro_hint_fishery" : "pro_hint_config";
            components.add(TextDisplay.of(TextManager.getString(guildEntity, locale, TextManager.GENERAL, key, -1)));
        }

        components.add(Separator.createInvisible(Separator.Spacing.SMALL));
        components.add(ActionRow.of(button));
        if (autoDelete) {
            components.add(TextDisplay.of("-# " + TextManager.getString(locale, TextManager.GENERAL, "deleteTime", String.valueOf(PROMO_DELETE_SEC))));
        }

        return MessageComponentTree.of(Container.of(components).withAccentColor(color));
    }

    private static void send(CommandEvent event, Locale locale, GuildEntity guildEntity, PromoType type) {
        if (event.isGenericCommandInteractionEvent()) {
            if (!event.getGenericCommandInteractionEvent().isAcknowledged()) {
                return;
            }
            event.getGenericCommandInteractionEvent().getHook()
                    .sendMessageComponents(buildComponentTree(locale, guildEntity, type, false))
                    .useComponentsV2()
                    .setEphemeral(true)
                    .queue();
        } else {
            event.getMessageChannel()
                    .sendMessageComponents(buildComponentTree(locale, guildEntity, type, true))
                    .useComponentsV2()
                    .queue(
                            message -> MainScheduler.schedule(Duration.ofSeconds(PROMO_DELETE_SEC), () -> {
                                if (BotPermissionUtil.can(event.getMessageChannel())) {
                                    message.delete().submit()
                                            .exceptionally(ExceptionLogger.get(ExceptionIds.UNKNOWN_MESSAGE, ExceptionIds.UNKNOWN_CHANNEL, ExceptionIds.MISSING_ACCESS));
                                }
                            })
                    );
        }
    }

}
