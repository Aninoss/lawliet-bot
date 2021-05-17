package modules;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import commands.Command;
import commands.CommandManager;
import commands.runnables.moderationcategory.ModSettingsCommand;
import constants.Category;
import core.EmbedFactory;
import core.MainLogger;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import core.utils.TimeUtil;
import javafx.util.Pair;
import modules.schedulers.TempBanScheduler;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationData;
import mysql.modules.tempban.DBTempBan;
import mysql.modules.tempban.TempBanData;
import mysql.modules.warning.DBServerWarnings;
import mysql.modules.warning.ServerWarningSlot;
import mysql.modules.warning.ServerWarningsData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

public class Mod {

    private static final String EMOJI_AUTOMOD = "👷";

    public static void insertWarning(Locale locale, Member target, Member requester, String reason, boolean withAutoActions) throws ExecutionException {
        insertWarning(locale, requester.getGuild(), target.getUser(), requester, reason, withAutoActions);
    }

    public static void insertWarning(Locale locale, Guild guild, User target, Member requester, String reason, boolean withAutoActions) throws ExecutionException {
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
            ModerationData moderationBean = DBModeration.getInstance().retrieve(guild.getIdLong());
            Role muteRole = moderationBean.getMuteRole().orElse(null);
            Member member = guild.getMember(target);

            int autoKickDays = moderationBean.getAutoKickDays();
            int autoBanDays = moderationBean.getAutoBanDays();
            int autoMuteDays = moderationBean.getAutoMuteDays();

            boolean autoKick = moderationBean.getAutoKick() > 0 && (autoKickDays > 0 ? serverWarningsBean.getAmountLatest(autoKickDays, ChronoUnit.DAYS).size() : serverWarningsBean.getWarnings().size()) >= moderationBean.getAutoKick();
            boolean autoBan = moderationBean.getAutoBan() > 0 && (autoBanDays > 0 ? serverWarningsBean.getAmountLatest(autoBanDays, ChronoUnit.DAYS).size() : serverWarningsBean.getWarnings().size()) >= moderationBean.getAutoBan();
            boolean autoMute = moderationBean.getAutoMute() > 0 && (autoMuteDays > 0 ? serverWarningsBean.getAmountLatest(autoMuteDays, ChronoUnit.DAYS).size() : serverWarningsBean.getWarnings().size()) >= moderationBean.getAutoMute();


            if (autoBan &&
                    PermissionCheckRuntime.getInstance().botHasPermission(locale, ModSettingsCommand.class, guild, Permission.BAN_MEMBERS) &&
                    BotPermissionUtil.canInteract(guild, target)
            ) {
                guild.retrieveBanList().queue(banList -> {
                    if (banList.stream().noneMatch(ban -> ban.getUser().getIdLong() == target.getIdLong())) {
                        int duration = moderationBean.getAutoBanDuration();
                        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                                .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_autoban"))
                                .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_autoban_template", duration > 0, target.getAsTag(), TimeUtil.getRemainingTimeString(locale, duration * 60_000L, false)));

                        postLogUsers(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, moderationBean.getGuildBean().getPrefix()), eb, guild, moderationBean, target).thenRun(() -> {
                            guild.ban(target.getId(), 0, TextManager.getString(locale, Category.MODERATION, "mod_autoban")).queue();
                            if (duration > 0) {
                                TempBanData tempBanData = new TempBanData(guild.getIdLong(), target.getIdLong(), Instant.now().plus(Duration.ofMinutes(duration)));
                                DBTempBan.getInstance().retrieve(guild.getIdLong()).put(target.getIdLong(), tempBanData);
                                TempBanScheduler.getInstance().loadTempBan(tempBanData);
                            }
                        });
                    }
                });
            } else if (autoKick &&
                    PermissionCheckRuntime.getInstance().botHasPermission(locale, ModSettingsCommand.class, guild, Permission.KICK_MEMBERS) &&
                    BotPermissionUtil.canInteract(guild, target) && guild.isMember(target)
            ) {
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_autokick"))
                        .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_autokick_template", target.getAsTag()));

                postLogUsers(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, moderationBean.getGuildBean().getPrefix()), eb, guild, moderationBean, target).thenRun(() -> {
                    guild.kick(target.getId(), TextManager.getString(locale, Category.MODERATION, "mod_autokick")).queue();
                });
            }

            if (autoMute &&
                    muteRole != null &&
                    PermissionCheckRuntime.getInstance().botCanManageRoles(locale, ModSettingsCommand.class, muteRole) &&
                    (member == null || !BotPermissionUtil.can(member, Permission.ADMINISTRATOR))
            ) {
                int duration = moderationBean.getAutoMuteDuration();
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_automute"))
                        .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_automute_template", duration > 0, target.getAsTag(), TimeUtil.getRemainingTimeString(locale, duration * 60_000L, false)));

                postLogUsers(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, moderationBean.getGuildBean().getPrefix()), eb, guild, moderationBean, target).thenRun(() -> {
                    Mute.mute(guild, target, duration, TextManager.getString(locale, Category.MODERATION, "mod_automute"));
                });
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
        return postLogMembers(command, eb, guild, DBModeration.getInstance().retrieve(guild.getIdLong()), members);
    }

    public static CompletableFuture<Void> postLogUsers(Command command, EmbedBuilder eb, Guild guild, List<User> users) {
        return postLogUsers(command, eb, guild, DBModeration.getInstance().retrieve(guild.getIdLong()), users);
    }

    public static CompletableFuture<Void> postLogMembers(Command command, EmbedBuilder eb, Guild guild, ModerationData moderationBean, Member member) {
        return postLogMembers(command, eb, guild, moderationBean, Collections.singletonList(member));
    }

    public static CompletableFuture<Void> postLogUsers(Command command, EmbedBuilder eb, Guild guild, ModerationData moderationBean, User user) {
        return postLogUsers(command, eb, guild, moderationBean, Collections.singletonList(user));
    }

    public static CompletableFuture<Void> postLogMembers(Command command, EmbedBuilder eb, Guild guild, ModerationData moderationBean, List<Member> members) {
        return Mod.postLogUsers(command, eb, guild, moderationBean, members.stream().map(Member::getUser).collect(Collectors.toList()));
    }

    public static CompletableFuture<Void> postLogUsers(Command command, EmbedBuilder eb, Guild guild, ModerationData moderationBean, List<User> users) {
        eb.setFooter("");
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
            users.forEach(user -> {
                if (!user.isBot() && guild.isMember(user)) {
                    try {
                        JDAUtil.sendPrivateMessage(user, eb.build()).complete();
                    } catch (Throwable e) {
                        MainLogger.get().error("Exception", e);
                    }
                }
            });
            return null;
        });

        moderationBean.getAnnouncementChannel().ifPresent(channel -> {
            if (PermissionCheckRuntime.getInstance().botHasPermission(command.getLocale(), command.getClass(), channel, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS)) {
                channel.sendMessage(eb.build()).queue();
            }
        });

        return future;
    }

}
