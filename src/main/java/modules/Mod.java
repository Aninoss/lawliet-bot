package modules;

import commands.Command;
import commands.CommandManager;
import commands.runnables.moderationcategory.ModSettingsCommand;
import constants.Category;
import constants.Permission;
import core.EmbedFactory;
import core.GlobalThreadPool;
import core.PermissionCheckRuntime;
import core.TextManager;
import javafx.util.Pair;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationBean;
import mysql.modules.warning.DBServerWarnings;
import mysql.modules.warning.ServerWarningsBean;
import mysql.modules.warning.ServerWarningsSlot;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Mod {

    private final static Logger LOGGER = LoggerFactory.getLogger(Mod.class);
    private static final String EMOJI_AUTOMOD = "👷";

    public static void insertWarning(Locale locale, Server server, User user, User requestor, String reason, boolean withAutoActions) throws ExecutionException {
        ServerWarningsBean serverWarningsBean = DBServerWarnings.getInstance().getBean(new Pair<>(server.getId(), user.getId()));
        serverWarningsBean.getWarnings().add(new ServerWarningsSlot(
                        server.getId(),
                        user.getId(),
                        Instant.now(),
                        requestor.getId(),
                        reason == null || reason.isEmpty() ? null : reason
                )
        );

        if (withAutoActions) {
            ModerationBean moderationBean = DBModeration.getInstance().getBean(server.getId());

            int autoKickDays = moderationBean.getAutoKickDays();
            int autoBanDays = moderationBean.getAutoBanDays();

            boolean autoKick = moderationBean.getAutoKick() > 0 && (autoKickDays > 0 ? serverWarningsBean.getAmountLatest(autoKickDays, ChronoUnit.DAYS).size() : serverWarningsBean.getWarnings().size()) >= moderationBean.getAutoKick();
            boolean autoBan = moderationBean.getAutoBan() > 0 && (autoBanDays > 0 ? serverWarningsBean.getAmountLatest(autoBanDays, ChronoUnit.DAYS).size() : serverWarningsBean.getWarnings().size()) >= moderationBean.getAutoBan();

            if (autoBan && PermissionCheckRuntime.getInstance().botHasPermission(locale, ModSettingsCommand.class, server, Permission.BAN_MEMBERS) && server.canYouBanUser(user)) {
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_autoban"))
                        .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_autoban_template", user.getDisplayName(server)));

                try {
                    postLog(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, moderationBean.getServerBean().getPrefix()), eb, moderationBean, user).thenRun(() -> {
                        server.banUser(user, 0, TextManager.getString(locale, Category.MODERATION, "mod_autoban")).exceptionally(ExceptionLogger.get());
                    });
                } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    LOGGER.error("Error when creating command class");
                }
            } else if (autoKick && PermissionCheckRuntime.getInstance().botHasPermission(locale, ModSettingsCommand.class, server, Permission.KICK_MEMBERS) && server.canYouKickUser(user)) {
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_autokick"))
                        .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_autokick_template", user.getDisplayName(server)));

                try {
                    postLog(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, moderationBean.getServerBean().getPrefix()), eb, moderationBean, user).thenRun(() -> {
                        server.kickUser(user, TextManager.getString(locale, Category.MODERATION, "mod_autokick")).exceptionally(ExceptionLogger.get());
                    });
                } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    LOGGER.error("Error when creating command class");
                }
            }
        }
    }

    public static CompletableFuture<Void> postLog(Command command, EmbedBuilder eb, Server server, User user) throws ExecutionException {
        return postLog(command, eb, server, Collections.singletonList(user));
    }

    public static CompletableFuture<Void> postLog(Command command, EmbedBuilder eb, Server server, List<User> users) throws ExecutionException {
        return postLog(command, eb, DBModeration.getInstance().getBean(server.getId()), users);
    }

    public static CompletableFuture<Void> postLog(Command command, EmbedBuilder eb, ModerationBean moderationBean, User user) {
        return postLog(command, eb, moderationBean, Collections.singletonList(user));
    }

    public static CompletableFuture<Void> postLog(Command command, EmbedBuilder eb, ModerationBean moderationBean, List<User> users) {
        eb.setFooter("");
        CompletableFuture<Void> future = new CompletableFuture<>();

        GlobalThreadPool.getExecutorService().submit(() -> {
            users.forEach(user -> {
                if (!user.isBot()) {
                    user.sendMessage(eb)
                            .exceptionally(e -> null)
                            .join();
                }
            });
            future.complete(null);
        });

        moderationBean.getAnnouncementChannel().ifPresent(serverTextChannel -> {
            if (PermissionCheckRuntime.getInstance().botHasPermission(command.getLocale(), command.getClass(), serverTextChannel, Permission.SEND_MESSAGES | Permission.EMBED_LINKS)) {
                serverTextChannel.sendMessage(eb)
                        .exceptionally(e -> null)
                        .join();
            }
        });

        return future;
    }

}
