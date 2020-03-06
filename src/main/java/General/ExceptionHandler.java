package General;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

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

    public static void handleException(Throwable throwable, Locale locale, TextChannel channel) {
        boolean showError = true;

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stacktrace = sw.toString();

        String errorMessage = stacktrace.split("\n")[0];
        if (errorMessage.contains("500: Internal Server Error")) {
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error500");
        } else if (errorMessage.contains("Server returned HTTP response code: 5")) {
            try {
                errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error500_alt");
                showError = false;
            } catch (Throwable throwable1) {
                throwable1.printStackTrace();
            }
        } else if (errorMessage.contains("java.net.SocketTimeoutException: timeout")) {
            showError = false;
            try {
                errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error_sockettimeout");
            } catch (Throwable throwable1) {
                throwable1.printStackTrace();
            }
        } else if (errorMessage.contains("MissingPermissions")) {
            try {
                errorMessage = TextManager.getString(locale, TextManager.GENERAL, "missing_permissions");
            } catch (Throwable throwable1) {
                throwable1.printStackTrace();
            }
        } else if (errorMessage.contains("CONSTRAINT `PowerPlantUserGainedUserBase`")) {
            showError = false;
        } else if (errorMessage.contains("Read timed out")) {
            showError = false;
            errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error_sockettimeout");
        } else {
            try {
                errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error_desc");
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        pw.close();
        try {
            sw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (showError) throwable.printStackTrace();

        try {
            if (channel.canYouWrite() && channel.canYouEmbedLinks()) channel.sendMessage(EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(locale,TextManager.GENERAL,"error"))
                    .setDescription(errorMessage+"\n\n"+TextManager.getString(locale,TextManager.GENERAL,"error_submit"))).get();

            if (showError) DiscordApiCollection.getInstance().getOwner().sendMessage(EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(locale,TextManager.GENERAL,"error"))
                    .setDescription(Tools.shortenString(stacktrace, 1000))).get();
        } catch (Throwable e1) {
            e1.printStackTrace();
        }
    }

    public static void showErrorLog(String str) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String text = String.format("[ERROR] %s: %s", dtf.format(now), str);
        System.err.println(text);
        System.out.println(text);
        DiscordApiCollection.getInstance().getOwner().sendMessage(text);
    }

    public static void showInfoLog(String str) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String text = String.format("[INFO] %s: %s", dtf.format(now), str);
        System.err.println(text);
        System.out.println(text);
    }

}