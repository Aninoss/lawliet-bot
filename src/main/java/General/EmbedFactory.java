package General;

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

    public static EmbedBuilder getCommandEmbedStandard(Command command) throws IOException {
        return getCommandEmbedStandard(command,null);
    }

    public static EmbedBuilder getCommandEmbedStandard(Command command, String description) throws IOException {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(DEFAULT_EMBED_COLOR)
                .setTitle(command.getEmoji()+" "+TextManager.getString(command.getLocale(), TextManager.COMMANDS, command.getTrigger()+"_title"))
                .setTimestampToNow();
        if (description != null && description.length() > 0) eb.setDescription(description);
        if (command.getThumbnail() != null && command.getThumbnail().length() > 0) eb.setThumbnail(command.getThumbnail());

        return eb;
    }

    public static EmbedBuilder getCommandEmbedStandard(Command command, String description, String title) throws IOException {
        return getCommandEmbedStandard(command, description).setTitle(command.getEmoji()+" "+title);
    }

    public static EmbedBuilder getCommandEmbedSuccess(Command command) throws IOException {
        return getCommandEmbedSuccess(command,null);
    }

    public static EmbedBuilder getCommandEmbedSuccess(Command command, String description) throws IOException {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(SUCCESS_EMBED_COLOR)
                .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "success"))
                .setTimestampToNow();
        if (description != null && description.length() > 0) eb.setDescription(description);
        if (command.getThumbnail() != null && command.getThumbnail().length() > 0) eb.setThumbnail(command.getThumbnail());

        return eb;
    }

    public static EmbedBuilder getCommandEmbedSuccess(Command command, String description, String title) throws IOException {
        return getCommandEmbedSuccess(command, description).setTitle(command.getEmoji()+" "+title);
    }

    public static EmbedBuilder getCommandEmbedError(Command command) throws IOException {
        return getCommandEmbedError(command,null);
    }

    public static EmbedBuilder getCommandEmbedError(Command command, String description) throws IOException {
        EmbedBuilder eb =  new EmbedBuilder()
                .setColor(FAILED_EMBED_COLOR)
                .setTitle(TextManager.getString(command.getLocale(),TextManager.GENERAL,"wrong_args"))
                .setTimestampToNow();
        if (description != null && description.length() > 0) eb.setDescription(description);
        if (command.getThumbnail() != null && command.getThumbnail().length() > 0) eb.setThumbnail(command.getThumbnail());
        return eb;
    }

    public static EmbedBuilder getCommandEmbedError(Command command, String description, String title) throws IOException {
        return getCommandEmbedError(command, description).setTitle(title);
    }

    public static EmbedBuilder getNSFWBlockEmbed(Locale locale) throws IOException {
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
}
