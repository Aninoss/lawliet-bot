package modules;

import commands.Category;
import commands.Command;
import commands.CommandManager;
import commands.runnables.moderationcategory.ModSettingsCommand;
import core.*;
import core.utils.BotPermissionUtil;
import core.utils.FutureUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import javafx.util.Pair;
import modules.schedulers.TempBanScheduler;
import mysql.hibernate.entity.GuildEntity;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationData;
import mysql.modules.tempban.DBTempBan;
import mysql.modules.tempban.TempBanData;
import mysql.modules.warning.DBServerWarnings;
import mysql.modules.warning.ServerWarningSlot;
import mysql.modules.warning.ServerWarningsData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Mod {

    private static final String EMOJI_AUTOMOD = "👷";

    public static void insertWarning(GuildEntity guildEntity, Member target, Member requester, String reason,
                                     boolean withAutoActions
    ) {
        insertWarning(guildEntity, requester.getGuild(), target.getUser(), requester, reason, withAutoActions);
    }

    public static void insertWarning(GuildEntity guildEntity, Guild guild, User target, Member requester, String reason,
                                     boolean withAutoActions
    ) {
        ServerWarningsData serverWarningsBean = DBServerWarnings.getInstance().retrieve(new Pair<>(guild.getIdLong(), target.getIdLong()));
        serverWarningsBean.getWarnings().add(new ServerWarningSlot(
                        guild.getIdLong(),
                        target.getIdLong(),
                        Instant.now(),
                        requester.getIdLong(),
                        reason == null || reason.isEmpty() ? null : reason
                )
        );

        if (withAutoActions) {
            String prefix = guildEntity.getPrefix();
            Locale locale = guildEntity.getLocale();

            ModerationData moderationBean = DBModeration.getInstance().retrieve(guild.getIdLong());
            Member member = MemberCacheController.getInstance().loadMember(guild, target.getIdLong()).join();

            int autoKickDays = moderationBean.getAutoKickDays();
            int autoBanDays = moderationBean.getAutoBanDays();
            int autoMuteDays = moderationBean.getAutoMuteDays();
            int autoJailDays = moderationBean.getAutoJailDays();

            boolean autoKick = moderationBean.getAutoKick() > 0 && (autoKickDays > 0 ? serverWarningsBean.getAmountLatest(autoKickDays, ChronoUnit.DAYS).size() : serverWarningsBean.getWarnings().size()) >= moderationBean.getAutoKick();
            boolean autoBan = moderationBean.getAutoBan() > 0 && (autoBanDays > 0 ? serverWarningsBean.getAmountLatest(autoBanDays, ChronoUnit.DAYS).size() : serverWarningsBean.getWarnings().size()) >= moderationBean.getAutoBan();
            boolean autoMute = moderationBean.getAutoMute() > 0 && (autoMuteDays > 0 ? serverWarningsBean.getAmountLatest(autoMuteDays, ChronoUnit.DAYS).size() : serverWarningsBean.getWarnings().size()) >= moderationBean.getAutoMute();
            boolean autoJail = moderationBean.getAutoJail() > 0 && (autoJailDays > 0 ? serverWarningsBean.getAmountLatest(autoJailDays, ChronoUnit.DAYS).size() : serverWarningsBean.getWarnings().size()) >= moderationBean.getAutoJail();

            if (autoBan &&
                    PermissionCheckRuntime.botHasPermission(locale, ModSettingsCommand.class, guild, Permission.BAN_MEMBERS) &&
                    BotPermissionUtil.canInteract(guild, target)
            ) {
                guild.retrieveBanList().queue(banList -> {
                    if (banList.stream().noneMatch(ban -> ban.getUser().getIdLong() == target.getIdLong())) {
                        int duration = moderationBean.getAutoBanDuration();
                        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                                .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_autoban"))
                                .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_autoban_template", duration > 0, StringUtil.escapeMarkdown(target.getAsTag()), TimeFormat.DATE_TIME_SHORT.after(Duration.ofMinutes(duration)).toString()));

                        postLogUsers(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, prefix), eb, guild, moderationBean, target).join();
                        guild.ban(target, 0, TimeUnit.DAYS)
                                .reason(TextManager.getString(locale, Category.MODERATION, "mod_autoban"))
                                .queue();
                        if (duration > 0) {
                            TempBanData tempBanData = new TempBanData(guild.getIdLong(), target.getIdLong(), Instant.now().plus(Duration.ofMinutes(duration)));
                            DBTempBan.getInstance().retrieve(guild.getIdLong()).put(target.getIdLong(), tempBanData);
                            TempBanScheduler.loadTempBan(tempBanData);
                        }
                    }
                });
            } else if (autoKick &&
                    PermissionCheckRuntime.botHasPermission(locale, ModSettingsCommand.class, guild, Permission.KICK_MEMBERS) &&
                    BotPermissionUtil.canInteract(guild, target) &&
                    guild.isMember(target)
            ) {
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_autokick"))
                        .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_autokick_template", StringUtil.escapeMarkdown(target.getAsTag())));

                postLogUsers(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, prefix), eb, guild, moderationBean, target).join();
                guild.kick(target, TextManager.getString(locale, Category.MODERATION, "mod_autokick")).queue();
            }

            if (autoJail &&
                    member != null &&
                    !BotPermissionUtil.can(member, Permission.ADMINISTRATOR)
            ) {
                List<Role> jailRoles = DBModeration.getInstance().retrieve(guild.getIdLong()).getJailRoleIds().transform(guild::getRoleById, ISnowflake::getIdLong);
                if (PermissionCheckRuntime.botCanManageRoles(locale, ModSettingsCommand.class, jailRoles)) {
                    int duration = moderationBean.getAutoJailDuration();
                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_autojail"))
                            .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_autojail_template", duration > 0, StringUtil.escapeMarkdown(target.getAsTag()), TimeFormat.DATE_TIME_SHORT.after(Duration.ofMinutes(duration)).toString()));

                    postLogUsers(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, prefix), eb, guild, moderationBean, target).join();
                    Jail.jail(guild, member, duration, TextManager.getString(locale, Category.MODERATION, "mod_autojail"), guildEntity);
                }
            }

            if (autoMute &&
                    PermissionCheckRuntime.botHasPermission(locale, ModSettingsCommand.class, guild, Permission.MODERATE_MEMBERS) &&
                    BotPermissionUtil.canInteract(guild, target)
            ) {
                int duration = moderationBean.getAutoMuteDuration();
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_automute"))
                        .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_automute_template", duration > 0, StringUtil.escapeMarkdown(target.getAsTag()), TimeFormat.DATE_TIME_SHORT.after(Duration.ofMinutes(duration)).toString()));

                postLogUsers(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, prefix), eb, guild, moderationBean, target).join();
                Mute.mute(guild, target, duration, TextManager.getString(locale, Category.MODERATION, "mod_automute"));
            }
        }
    }

    public static CompletableFuture<Void> postLogMembers(Command command, EmbedBuilder eb, Guild guild, Member member) {
        return postLogMembers(command, eb, guild, Collections.singletonList(member));
    }

    public static CompletableFuture<Void> postLogUsers(Command command, EmbedBuilder eb, Guild guild, User user) {
        return postLogUsers(command, eb, guild, Collections.singletonList(user));
    }

    public static CompletableFuture<Void> postLogMembers(Command command, EmbedBuilder eb, Guild guild, List<Member> members) {
        return postLogMembers(command, eb, DBModeration.getInstance().retrieve(guild.getIdLong()), members);
    }

    public static CompletableFuture<Void> postLogUsers(Command command, EmbedBuilder eb, Guild guild, List<User> users) {
        return postLogUsers(command, eb, guild, DBModeration.getInstance().retrieve(guild.getIdLong()), users);
    }

    public static CompletableFuture<Void> postLogMembers(Command command, EmbedBuilder eb, ModerationData moderationBean, Member member) {
        return postLogMembers(command, eb, moderationBean, Collections.singletonList(member));
    }

    public static CompletableFuture<Void> postLogUsers(Command command, EmbedBuilder eb, Guild guild, ModerationData moderationBean, User user) {
        return postLogUsers(command, eb, guild, moderationBean, Collections.singletonList(user));
    }

    public static CompletableFuture<Void> postLogMembers(Command command, EmbedBuilder eb, ModerationData moderationBean, List<Member> members) {
        eb.setFooter("");

        CompletableFuture<Void> future = FutureUtil.supplyAsync(() -> {
            members.forEach(member -> {
                if (!member.getUser().isBot()) {
                    try {
                        JDAUtil.openPrivateChannel(member)
                                .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                                .complete();
                    } catch (Throwable e) {
                        MainLogger.get().error("Exception", e);
                    }
                }
            });
            return null;
        });

        sendAnnouncement(command, eb, moderationBean);
        return future;
    }

    public static CompletableFuture<Void> postLogUsers(Command command, EmbedBuilder eb, Guild guild, ModerationData moderationBean, List<User> users) {
        eb.setFooter("");

        CompletableFuture<Void> future = FutureUtil.supplyAsync(() -> {
            MemberCacheController.getInstance().loadMembersWithUsers(guild, users).join()
                    .forEach(member -> {
                        if (!member.getUser().isBot()) {
                            try {
                                JDAUtil.openPrivateChannel(member)
                                        .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                                        .complete();
                            } catch (Throwable e) {
                                MainLogger.get().error("Exception", e);
                            }
                        }
                    });
            return null;
        });

        sendAnnouncement(command, eb, moderationBean);
        return future;
    }

    public static void sendAnnouncement(Command command, EmbedBuilder eb, ModerationData moderationBean) {
        eb.setFooter("");

        moderationBean.getAnnouncementChannel().ifPresent(channel -> {
            if (PermissionCheckRuntime.botHasPermission(command.getLocale(), command.getClass(), channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
                channel.sendMessageEmbeds(eb.build()).queue();
            }
        });
    }

}
