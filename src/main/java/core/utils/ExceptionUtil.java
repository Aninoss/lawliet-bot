package core.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import commands.Command;
import constants.AssetIds;
import core.*;
import net.dv8tion.jda.api.entities.TextChannel;

public class ExceptionUtil {

    public static void handleCommandException(Throwable throwable, Command command, TextChannel channel) {
        Locale locale = command.getLocale();
        boolean postErrorMessage = true;
        boolean submitToDeveloper = new ExceptionFilter().shouldBeVisible(throwable.toString());

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
        } else if (errorCause.contains("Read timed out")) {
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error_sockettimeout");
        }

        String transmitStackTrace = StringUtil.shortenString(stacktrace, 1000);
        String code = Long.toHexString(Math.abs(transmitStackTrace.hashCode())).toUpperCase();

        if (postErrorMessage && BotPermissionUtil.canWriteEmbed(channel)) {
            channel.sendMessageEmbeds(EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(locale, TextManager.GENERAL, "error_code", code))
                    .setDescription(errorMessage + (submitToDeveloper ? TextManager.getString(locale, TextManager.GENERAL, "error_submit") : ""))
                    .build()
            ).queue();
        }

        if (submitToDeveloper) {
            MainLogger.get().error("Exception for command \"{}\" and code {}", command.getTrigger(), code, throwable);
            if (Program.productionMode()) {
                JDAUtil.sendPrivateMessage(
                        AssetIds.OWNER_USER_ID,
                        EmbedFactory.getEmbedError()
                                .setTitle(TextManager.getString(locale, TextManager.GENERAL, "error_code", code) + " \"" + command.getTrigger() + "\"")
                                .setDescription(transmitStackTrace)
                                .build()
                ).queue();
            }
        }
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
        Exception e = new Exception("Stack Trace");
        e.setStackTrace(t.getStackTrace());
        return e;
    }


}