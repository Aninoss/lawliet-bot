package core;

import commands.Command;
import core.utils.StringUtil;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

/**
 * Verarbeitet Exceptions
 */
public class ExceptionHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);

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
            LOGGER.error("Could not close String Writer", e);
        }

        if (postErrorMessage && channel.canYouWrite() && channel.canYouEmbedLinks()) {
            channel.sendMessage(EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(locale, TextManager.GENERAL, "error"))
                    .setDescription(errorMessage + (submitToDeveloper ? TextManager.getString(locale, TextManager.GENERAL, "error_submit") : ""))
            ).exceptionally(ExceptionLogger.get());
        }

        if (submitToDeveloper) {
            LOGGER.error("Exception for command \"{}\" with state {}", command.getTrigger(), command.getState(), throwable);
            if (Bot.isProductionMode()) {
                DiscordApiManager.getInstance().fetchOwner().join().sendMessage(EmbedFactory.getEmbedError()
                        .setTitle(TextManager.getString(locale, TextManager.GENERAL, "error") + " \"" + command.getTrigger() + "\"")
                        .setDescription(StringUtil.shortenString(stacktrace, 1000))).exceptionally(ExceptionLogger.get());
            }
        }
    }

    public static boolean exceptionIsClass(Throwable throwable, Class<?> c) {
        return c.isInstance(throwable) || (throwable.getMessage() != null && throwable.getMessage().startsWith(c.getName()));
    }



}