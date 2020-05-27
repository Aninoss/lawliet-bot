package Core;

import CommandSupporters.Command;
import Constants.LogStatus;
import Constants.Settings;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.io.IOException;
import java.util.Locale;

public class EmbedFactory {

    public static final Color DEFAULT_EMBED_COLOR = new Color(254, 254, 254);
    public static final Color SUCCESS_EMBED_COLOR = Color.GREEN;
    public static final Color FAILED_EMBED_COLOR = Color.RED;

    public static EmbedBuilder getCommandEmbedStandard(Command command) {
        return getCommandEmbedStandard(command,null);
    }

    public static EmbedBuilder getCommandEmbedStandard(Command command, String description) {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(DEFAULT_EMBED_COLOR)
                .setTitle(command.getEmoji()+" "+TextManager.getString(command.getLocale(), TextManager.COMMANDS, command.getTrigger()+"_title"))
                .setTimestampToNow();
        if (description != null && description.length() > 0) eb.setDescription(description);

        return eb;
    }

    public static EmbedBuilder getCommandEmbedStandard(Command command, String description, String title) {
        return getCommandEmbedStandard(command, description).setTitle(command.getEmoji()+" "+title);
    }

    public static EmbedBuilder getCommandEmbedError(Command command) {
        return getCommandEmbedError(command,null);
    }

    public static EmbedBuilder getCommandEmbedError(Command command, String description) {
        EmbedBuilder eb =  new EmbedBuilder()
                .setColor(FAILED_EMBED_COLOR)
                .setTitle(TextManager.getString(command.getLocale(),TextManager.GENERAL,"wrong_args"))
                .setTimestampToNow();
        if (description != null && description.length() > 0) eb.setDescription(description);
        return eb;
    }

    public static EmbedBuilder getCommandEmbedError(Command command, String description, String title) throws IOException {
        return getCommandEmbedError(command, description).setTitle(title);
    }

    public static EmbedBuilder getNSFWBlockEmbed(Locale locale) {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(FAILED_EMBED_COLOR)
                .setTitle(TextManager.getString(locale,TextManager.GENERAL, "nsfw_block_title"))
                .setDescription(TextManager.getString(locale,TextManager.GENERAL, "nsfw_block_description"))
                .setTimestampToNow();

        return eb;
    }

    public static EmbedBuilder getEmbed() {
        return new EmbedBuilder()
                .setColor(DEFAULT_EMBED_COLOR)
                .setTimestampToNow();
    }

    public static EmbedBuilder getEmbedSuccessful() {
        return new EmbedBuilder()
                .setColor(SUCCESS_EMBED_COLOR)
                .setTimestampToNow();
    }

    public static EmbedBuilder getEmbedError() {
        return new EmbedBuilder()
                .setColor(FAILED_EMBED_COLOR)
                .setTimestampToNow();
    }

    public static EmbedBuilder addNoResultsLog(EmbedBuilder eb, Locale locale, String searchString) {
        return addLog(eb, LogStatus.FAILURE, TextManager.getString(locale, TextManager.GENERAL, "no_results_description", searchString));
    }

    public static EmbedBuilder addLog(EmbedBuilder eb, LogStatus logStatus, String log) {
        if (log != null && log.length() > 0) {
            String add = "";
            if (logStatus != null) {
                switch (logStatus) {
                    case FAILURE:
                        add = "❌ ";
                        break;

                    case SUCCESS:
                        add = "✅ ";
                        break;

                    case WIN:
                        add = "\uD83C\uDF89 ";
                        break;

                    case LOSE:
                        add = "☠️ ";
                        break;

                    case WARNING:
                        add = "⚠️️ ";
                        break;
                }
            }
            eb.addField(Settings.EMPTY_EMOJI, "`" + add + log + "`");
        }

        return eb;
    }

    public static EmbedBuilder addTrackerNote(Locale locale, EmbedBuilder eb, String prefix, String trigger) {
        return EmbedFactory.addLog(eb, null, TextManager.getString(locale, TextManager.GENERAL, "tracker", prefix, trigger));
    }

}
