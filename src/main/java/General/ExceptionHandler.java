package General;

import CommandSupporters.Command;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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
            try {
                errorMessage = TextManager.getString(locale, TextManager.GENERAL, "error500");
            } catch (IOException e) {
                e.printStackTrace();
            }
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

}