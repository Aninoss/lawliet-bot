package Modules;

import CommandSupporters.Command;
import CommandSupporters.CommandManager;
import Commands.ModerationCategory.ModSettingsCommand;
import Constants.Category;
import Constants.Permission;
import Core.EmbedFactory;
import Core.PermissionCheckRuntime;
import Core.TextManager;
import MySQL.Modules.Moderation.DBModeration;
import MySQL.Modules.Moderation.ModerationBean;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Warning.DBServerWarnings;
import MySQL.Modules.Warning.ServerWarningsBean;
import MySQL.Modules.Warning.ServerWarningsSlot;
import javafx.util.Pair;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class Mod {

    private final static Logger LOGGER = LoggerFactory.getLogger(Mod.class);
    private static final String EMOJI_AUTOMOD = "👷";

    public static void insertWarning(Locale locale, Server server, User user, User requestor, String reason, boolean withAutoMod) throws ExecutionException, InterruptedException {
        ServerWarningsBean serverWarningsBean = DBServerWarnings.getInstance().getBean(new Pair<>(server.getId(), user.getId()));
        serverWarningsBean.getWarnings().add(new ServerWarningsSlot(
                DBServer.getInstance().getBean(server.getId()),
                user.getId(),
                Instant.now(),
                requestor.getId(),
                reason == null || reason.isEmpty() ? null : reason)
        );

        if (withAutoMod) {
            ModerationBean moderationBean = DBModeration.getInstance().getBean(server.getId());

            int autoKickDays = moderationBean.getAutoKickDays();
            int autoBanDays = moderationBean.getAutoBanDays();

            boolean autoKick = moderationBean.getAutoKick() > 0 && (autoKickDays > 0 ? serverWarningsBean.getAmountLatest(autoKickDays, ChronoUnit.DAYS).size() : serverWarningsBean.getWarnings().size()) >= moderationBean.getAutoKick();
            boolean autoBan = moderationBean.getAutoBan() > 0 && (autoBanDays > 0 ? serverWarningsBean.getAmountLatest(autoBanDays, ChronoUnit.DAYS).size() : serverWarningsBean.getWarnings().size()) >= moderationBean.getAutoBan();

            if (autoBan && PermissionCheckRuntime.getInstance().botHasPermission(locale, ModSettingsCommand.class, server, Permission.BAN_MEMBERS) && server.canYouBanUser(user)) {
                try {
                    server.banUser(user, 0, TextManager.getString(locale, Category.MODERATION, "mod_autoban")).get();

                    EmbedBuilder eb = EmbedFactory.getEmbed()
                            .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_autoban"))
                            .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_autoban_template", user.getDisplayName(server)));

                    postLog(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, moderationBean.getServerBean().getPrefix()), eb, moderationBean);
                } catch (IllegalAccessException | InstantiationException | ExecutionException | InvocationTargetException e) {
                    LOGGER.error("Could not ban user", e);
                }
            } else if (autoKick && PermissionCheckRuntime.getInstance().botHasPermission(locale, ModSettingsCommand.class, server, Permission.KICK_MEMBERS) && server.canYouKickUser(user)) {
                try {
                    server.kickUser(user, TextManager.getString(locale, Category.MODERATION, "mod_autokick")).get();

                    EmbedBuilder eb = EmbedFactory.getEmbed()
                            .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_autokick"))
                            .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_autokick_template", user.getDisplayName(server)));

                    postLog(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, moderationBean.getServerBean().getPrefix()), eb, moderationBean);
                } catch (ExecutionException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    LOGGER.error("Could not kick user", e);
                }
            }
        }
    }

    public static void postLog(Command command, EmbedBuilder eb, Server server) throws ExecutionException {
        postLog(command, eb, DBModeration.getInstance().getBean(server.getId()));
    }

    public static void postLog(Command command, EmbedBuilder eb, ModerationBean moderationBean) {
        moderationBean.getAnnouncementChannel().ifPresent(serverTextChannel -> {
            if (PermissionCheckRuntime.getInstance().botHasPermission(command.getLocale(), command.getClass(), serverTextChannel, Permission.SEND_MESSAGES | Permission.EMBED_LINKS)) {
                try {
                    serverTextChannel.sendMessage(eb).get();
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.error("Could not post warning", e);
                }
            }
        });
    }
}
