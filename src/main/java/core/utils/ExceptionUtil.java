package core.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import commands.Command;
import commands.CommandEvent;
import commands.runnables.NavigationAbstract;
import core.EmbedFactory;
import core.MainLogger;
import core.TextManager;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;

public class ExceptionUtil {

    public static void handleCommandException(Throwable throwable, Command command, CommandEvent event, GuildEntity guildEntity) {
        Locale locale = command.getLocale();
        boolean postErrorMessage = true;

        String stacktrace = exceptionToString(throwable);
        String errorCause = stacktrace.split("\n")[0];
        String errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error_desc");

        if (errorCause.contains("500: Internal Server Error")) {
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error500");
        } else if (errorCause.contains("Server returned HTTP response code: 5")) {
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error500_alt");
        } else if (errorCause.contains("java.net.SocketTimeoutException")) {
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error_sockettimeout");
        } else if (errorCause.contains("50013") || errorCause.contains("PermissionException")) {
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "missing_permissions");
        } else if (throwable instanceof InterruptedException) {
            postErrorMessage = false;
        } else if (errorCause.contains("10008")) {
            postErrorMessage = false;
        } else if (errorCause.contains("Read timed out")) {
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error_sockettimeout");
        }

        String transmitStackTrace = StringUtil.shortenString(stacktrace, 1000);
        String code = Long.toHexString(Math.abs(transmitStackTrace.hashCode())).toUpperCase();

        if (postErrorMessage && BotPermissionUtil.can(event.getMessageChannel())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(locale, TextManager.GENERAL, "error_code", code))
                    .setDescription(errorMessage + TextManager.getString(locale, TextManager.GENERAL, "error_submit"));
            event.replyMessageEmbeds(guildEntity, eb.build()).queue();
        }

        int state = -1;
        if (command instanceof NavigationAbstract) {
            state = ((NavigationAbstract) command).getState();
        }
        MainLogger.get().error("Exception for command \"{}\" (state {}) and code {}", command.getTrigger(), state, code, throwable);
    }

    public static String exceptionToString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        String stacktrace;
        try (PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            stacktrace = sw.toString();
        }

        return stacktrace;
    }

    public static Exception generateForStack(Thread t) {
        return generateForStack(t, "Stack Trace");
    }

    public static Exception generateForStack(Thread t, String name) {
        return generateForStack(t.getStackTrace(), name);
    }

    public static Exception generateForStack(StackTraceElement[] stackTrace, String name) {
        Exception e = new Exception(name);
        e.setStackTrace(stackTrace);
        return e;
    }


}