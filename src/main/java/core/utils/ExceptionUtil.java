package core.utils;

import commands.Command;
import core.*;
import net.dv8tion.jda.api.entities.TextChannel;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

public class ExceptionUtil {

    public static void handleCommandException(Throwable throwable, Command command, TextChannel channel) {
        Locale locale = command.getLocale();
        boolean postErrorMessage = true;
        boolean submitToDeveloper = new ExceptionFilter().checkThrowable(throwable);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stacktrace = sw.toString();

        String errorCause = stacktrace.split("\n")[0];
        String errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error_desc");

        if (errorCause.contains("500: Internal Server Error")) {
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error500");
        } else if (errorCause.contains("Server returned HTTP response code: 5")) {
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error500_alt");
        } else if (errorCause.contains("java.net.SocketTimeoutException")) {
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error_sockettimeout");
        } else if (errorCause.contains("MissingPermissions")) {
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "missing_permissions");
        } else if (throwable instanceof InterruptedException) {
            postErrorMessage = false;
        } else if (errorCause.contains("Read timed out")) {
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error_sockettimeout");
        }

        pw.close();
        try {
            sw.close();
        } catch (IOException e) {
            MainLogger.get().error("Could not close String Writer", e);
        }

        String transmitStackTrace = StringUtil.shortenString(stacktrace, 1000);
        String code = Long.toHexString(Math.abs(transmitStackTrace.hashCode())).toUpperCase();

        if (postErrorMessage && BotPermissionUtil.canWriteEmbed(channel)) {
            channel.sendMessage(EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(locale, TextManager.GENERAL, "error_code", code))
                    .setDescription(errorMessage + (submitToDeveloper ? TextManager.getString(locale, TextManager.GENERAL, "error_submit") : ""))
                    .build()
            ).queue();
        }

        if (submitToDeveloper) {
            MainLogger.get().error("Exception for command \"{}\"} and code {}", command.getTrigger(), code, throwable);
            if (Bot.isProductionMode()) {
                JDAUtil.sendPrivateMessage(
                        DiscordApiManager.getInstance().fetchOwner().join(),
                        EmbedFactory.getEmbedError()
                                .setTitle(TextManager.getString(locale, TextManager.GENERAL, "error_code", code) + " \"" + command.getTrigger() + "\"")
                                .setDescription(transmitStackTrace)
                                .build()
                ).queue();
            }
        }
    }

    public static boolean exceptionIsClass(Throwable throwable, Class<?> c) {
        return c.isInstance(throwable) || (throwable.getMessage() != null && throwable.getMessage().startsWith(c.getName()));
    }

    public static Exception generateForStack(Thread t) {
        Exception e = new Exception("Stack Trace");
        e.setStackTrace(t.getStackTrace());
        return e;
    }


}