package modules;

import commands.Command;
import commands.CommandManager;
import commands.runnables.moderationcategory.ModSettingsCommand;
import constants.Category;
import core.*;
import core.utils.JDAUtil;
import javafx.util.Pair;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationBean;
import mysql.modules.warning.DBServerWarnings;
import mysql.modules.warning.ServerWarningsBean;
import mysql.modules.warning.GuildWarningsSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Mod {

    private static final String EMOJI_AUTOMOD = "👷";

    public static void insertWarning(Locale locale, Guild guild, Member member, Member requestor, String reason, boolean withAutoActions) throws ExecutionException {
        ServerWarningsBean serverWarningsBean = DBServerWarnings.getInstance().retrieve(new Pair<>(guild.getIdLong(), member.getIdLong()));
        serverWarningsBean.getWarnings().add(new GuildWarningsSlot(
                        guild.getIdLong(),
                        member.getIdLong(),
                        Instant.now(),
                        requestor.getIdLong(),
                        reason == null || reason.isEmpty() ? null : reason
                )
        );

        if (withAutoActions) {
            ModerationBean moderationBean = DBModeration.getInstance().retrieve(guild.getIdLong());

            int autoKickDays = moderationBean.getAutoKickDays();
            int autoBanDays = moderationBean.getAutoBanDays();

            boolean autoKick = moderationBean.getAutoKick() > 0 && (autoKickDays > 0 ? serverWarningsBean.getAmountLatest(autoKickDays, ChronoUnit.DAYS).size() : serverWarningsBean.getWarnings().size()) >= moderationBean.getAutoKick();
            boolean autoBan = moderationBean.getAutoBan() > 0 && (autoBanDays > 0 ? serverWarningsBean.getAmountLatest(autoBanDays, ChronoUnit.DAYS).size() : serverWarningsBean.getWarnings().size()) >= moderationBean.getAutoBan();

            if (autoBan && PermissionCheckRuntime.getInstance().botHasPermission(locale, ModSettingsCommand.class, guild, Permission.BAN_MEMBERS) && guild.getSelfMember().canInteract(member)) {
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_autoban"))
                        .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_autoban_template", member.getEffectiveName()));

                postLog(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, moderationBean.getGuildBean().getPrefix()), eb, moderationBean, member).thenRun(() -> {
                    guild.ban(member, 0, TextManager.getString(locale, Category.MODERATION, "mod_autoban")).queue();
                });
            } else if (autoKick && PermissionCheckRuntime.getInstance().botHasPermission(locale, ModSettingsCommand.class, guild, Permission.KICK_MEMBERS) && guild.getSelfMember().canInteract(member)) {
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_autokick"))
                        .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_autokick_template", member.getEffectiveName()));

                postLog(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, moderationBean.getGuildBean().getPrefix()), eb, moderationBean, member).thenRun(() -> {
                    guild.kick(member, TextManager.getString(locale, Category.MODERATION, "mod_autokick")).queue();
                });
            }
        }
    }

    public static CompletableFuture<Void> postLog(Command command, EmbedBuilder eb, Guild guild, Member member) {
        return postLog(command, eb, guild, Collections.singletonList(member));
    }

    public static CompletableFuture<Void> postLog(Command command, EmbedBuilder eb, Guild guild, List<Member> members) {
        return postLog(command, eb, DBModeration.getInstance().retrieve(guild.getIdLong()), members);
    }

    public static CompletableFuture<Void> postLog(Command command, EmbedBuilder eb, ModerationBean moderationBean, Member member) {
        return postLog(command, eb, moderationBean, Collections.singletonList(member));
    }

    public static CompletableFuture<Void> postLog(Command command, EmbedBuilder eb, ModerationBean moderationBean, List<Member> members) {
        eb.setFooter("");
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
            members.forEach(member -> {
                if (!member.getUser().isBot()) {
                    try {
                        JDAUtil.sendPrivateMessage(member, eb.build()).complete();
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
