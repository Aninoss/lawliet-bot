package Core;

import Core.Tools.StringTools;
import org.javacord.api.entity.channel.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Verarbeitet Exceptions
 */
public class ExceptionHandler {

    final static Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);

    public static void handleException(Throwable throwable, Locale locale, TextChannel channel) {
        boolean submitToDeveloper = true;
        LOGGER.error("Command exception", throwable);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stacktrace = sw.toString();

        String errorMessage = stacktrace.split("\n")[0];
        if (errorMessage.contains("500: Internal Server Error")) {
            submitToDeveloper = false;
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error500");
        } else if (errorMessage.contains("Server returned HTTP response code: 5")) {
            submitToDeveloper = false;
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error500_alt");
        } else if (errorMessage.contains("java.net.SocketTimeoutException")) {
            submitToDeveloper = false;
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error_sockettimeout");
        } else if (errorMessage.contains("MissingPermissions")) {
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "missing_permissions");
        } else if (errorMessage.contains("Read timed out")) {
            submitToDeveloper = false;
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error_sockettimeout");
        } else {
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error_desc");
        }

        pw.close();
        try {
            sw.close();
        } catch (IOException e) {
            LOGGER.error("Could not close String Writer", e);
        }

        try {
            if (channel.canYouWrite() && channel.canYouEmbedLinks()) channel.sendMessage(EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(locale,TextManager.GENERAL,"error"))
                    .setDescription(errorMessage + (submitToDeveloper ? "\n\n"+TextManager.getString(locale,TextManager.GENERAL,"error_submit") : ""))).get();

            if (submitToDeveloper) {
                DiscordApiCollection.getInstance().getOwner().sendMessage(EmbedFactory.getEmbedError()
                        .setTitle(TextManager.getString(locale,TextManager.GENERAL,"error"))
                        .setDescription(StringTools.shortenString(stacktrace, 1000))).get();
            }
        } catch (Throwable e1) {
            LOGGER.error("Could not send error message", e1);
        }
    }

    public static boolean exceptionIsClass(Throwable throwable, Class<?> c) {
        return c.isInstance(throwable) || (throwable.getMessage() != null && throwable.getMessage().startsWith(c.getName()));
    }



}