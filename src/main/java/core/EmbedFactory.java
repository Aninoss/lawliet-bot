package core;

import java.awt.*;
import java.util.Locale;
import commands.Command;
import core.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;

public class EmbedFactory {

    public static final Color DEFAULT_EMBED_COLOR = new Color(254, 254, 254);
    public static final Color FAILED_EMBED_COLOR = Color.RED;

    public static EmbedBuilder getEmbedDefault(Command command) {
        return getEmbedDefault(command,null);
    }

    public static EmbedBuilder getEmbedDefault(Command command, String description) {
        EmbedBuilder eb = getEmbedDefault()
                .setColor(DEFAULT_EMBED_COLOR)
                .setTitle(command.getCommandProperties().emoji() + " " + command.getCommandLanguage().getTitle());

        if (description != null && description.length() > 0)
            eb.setDescription(description);
        EmbedUtil.setFooter(eb, command);
        return eb;
    }

    public static EmbedBuilder getEmbedDefault(Command command, String description, String title) {
        return getEmbedDefault(command, description)
                .setTitle(command.getCommandProperties().emoji() + " " + title);
    }

    public static EmbedBuilder getEmbedDefault() {
        return new EmbedBuilder()
                .setColor(DEFAULT_EMBED_COLOR);
    }

    public static EmbedBuilder getEmbedError(Command command) {
        return getEmbedError(command,null);
    }

    public static EmbedBuilder getEmbedError(Command command, String description) {
        EmbedBuilder eb = getEmbedError()
                .setColor(FAILED_EMBED_COLOR)
                .setTitle(TextManager.getString(command.getLocale(),TextManager.GENERAL,"wrong_args"));

        if (description != null && description.length() > 0)
            eb.setDescription(description);
        EmbedUtil.setFooter(eb, command);
        return eb;
    }

    public static EmbedBuilder getEmbedError(Command command, String description, String title) {
        return getEmbedError(command, description)
                .setTitle(title);
    }

    public static EmbedBuilder getEmbedError() {
        return new EmbedBuilder()
                .setColor(FAILED_EMBED_COLOR);
    }

    public static EmbedBuilder getNSFWBlockEmbed(Locale locale) {
        return getEmbedError()
                .setTitle(TextManager.getString(locale,TextManager.GENERAL, "nsfw_block_title"))
                .setDescription(TextManager.getString(locale,TextManager.GENERAL, "nsfw_block_description"));
    }

    public static EmbedBuilder getApiDownEmbed(Locale locale, String service) {
        return EmbedFactory.getEmbedError()
                .setTitle(TextManager.getString(locale, TextManager.GENERAL, "quiz_down_title"))
                .setDescription(TextManager.getString(locale, TextManager.GENERAL, "api_down", service));
    }

    public static EmbedBuilder getAbortEmbed(Command command) {
        return EmbedFactory.getEmbedDefault(
                command,
                TextManager.getString(command.getLocale(), TextManager.GENERAL, "process_abort_description"),
                TextManager.getString(command.getLocale(), TextManager.GENERAL, "process_abort_title")
        );
    }

}
