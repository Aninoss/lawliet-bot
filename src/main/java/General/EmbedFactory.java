package General;

import CommandSupporters.Command;
import Constants.LogStatus;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.io.IOException;
import java.util.Locale;

public class EmbedFactory {
    public static EmbedBuilder getCommandEmbedStandard(Command command) throws IOException {
        return getCommandEmbedStandard(command,null);
    }

    public static EmbedBuilder getCommandEmbedStandard(Command command, String description) throws IOException {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(Color.WHITE)
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
                .setColor(Color.GREEN)
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
                .setColor(Color.RED)
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
                .setColor(Color.RED)
                .setTitle(TextManager.getString(locale,TextManager.GENERAL, "nsfw_block_title"))
                .setDescription(TextManager.getString(locale,TextManager.GENERAL, "nsfw_block_description"))
                .setTimestampToNow();

        return eb;
    }

    public static EmbedBuilder getEmbed() {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(Color.WHITE)
                .setTimestampToNow();

        return eb;
    }

    public static EmbedBuilder getEmbedSuccessful() {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(Color.GREEN)
                .setTimestampToNow();

        return eb;
    }

    public static EmbedBuilder getEmbedError() {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(Color.RED)
                .setTimestampToNow();

        return eb;
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
            eb.addField(Tools.getEmptyCharacter(), "`" + add + log + "`");
        }

        return eb;
    }
}
