package core;

import java.awt.*;
import java.util.ArrayList;
import java.util.Locale;
import commands.Command;
import constants.ExternalLinks;
import constants.Language;
import constants.Settings;
import core.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

public class EmbedFactory {

    public static final Color DEFAULT_EMBED_COLOR = new Color(254, 254, 254);
    public static final Color FAILED_EMBED_COLOR = Color.RED;

    public static EmbedBuilder getEmbedDefault(Command command) {
        return getEmbedDefault(command, null);
    }

    public static EmbedBuilder getEmbedDefault(Command command, String description) {
        EmbedBuilder eb = getEmbedDefault()
                .setColor(DEFAULT_EMBED_COLOR)
                .setTitle(command.getCommandProperties().emoji() + " " + command.getCommandLanguage().getTitle());

        if (description != null && description.length() > 0) {
            eb.setDescription(description);
        }
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
        return getEmbedError(command, null);
    }

    public static EmbedBuilder getEmbedError(Command command, String description) {
        EmbedBuilder eb = getEmbedError()
                .setColor(FAILED_EMBED_COLOR)
                .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "wrong_args"));

        if (description != null && description.length() > 0) {
            eb.setDescription(description);
        }
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

    public static Button getNSFWBlockButton(Locale locale) {
        String link = switch (Language.from(locale)) {
            case DE -> "https://support.discord.com/hc/de/articles/115000084051#h_adc93a2c-8fc3-4775-be02-bbdbfcde5010";
            case ES -> "https://support.discord.com/hc/es/articles/115000084051-Canales-y-contenido-de-NAPW-NSFW-por-sus-siglas-en-ingl%C3%A9s-#h_66a0d66a-562d-4f19-92ec-f16e2ba3cca3";
            case RU -> "https://support.discord.com/hc/ru/articles/115000084051";
            default -> "https://support.discord.com/hc/en-us/articles/115000084051#h_adc93a2c-8fc3-4775-be02-bbdbfcde5010";
        };

        return Button.of(ButtonStyle.LINK, link, TextManager.getString(locale, TextManager.GENERAL, "nsfw_block_button"));
    }

    public static EmbedBuilder getNSFWBlockEmbed(Locale locale) {
        return getEmbedError()
                .setTitle(TextManager.getString(locale, TextManager.GENERAL, "nsfw_block_title"))
                .setDescription(TextManager.getString(locale, TextManager.GENERAL, "nsfw_block_description"));
    }

    public static Button[] getPatreonBlockButtons(Locale locale) {
        return new Button[] {
                Button.of(ButtonStyle.LINK,ExternalLinks.PATREON_PAGE, TextManager.getString(locale, TextManager.GENERAL, "patreon_button_patreon")),
                Button.of(ButtonStyle.LINK, ExternalLinks.UNLOCK_SERVER_WEBSITE, TextManager.getString(locale, TextManager.GENERAL, "patreon_button_unlock"))
        };
    }

    public static EmbedBuilder getPatreonBlockEmbed(Locale locale) {
        String desc = TextManager.getString(
                locale,
                TextManager.GENERAL,
                "patreon_description",
                ExternalLinks.PATREON_PAGE,
                ExternalLinks.UNLOCK_SERVER_WEBSITE
        );

        return EmbedFactory.getEmbedDefault()
                .setColor(Settings.PATREON_COLOR)
                .setAuthor(TextManager.getString(locale, TextManager.GENERAL, "patreon_title"), ExternalLinks.PATREON_PAGE, "https://c5.patreon.com/external/favicon/favicon-32x32.png?v=69kMELnXkB")
                .setDescription(desc);
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

    public static EmbedBuilder getCommandDMEmbed(ArrayList<ActionRow> actionRowList) {
        EmbedBuilder eb;
        if (Program.publicVersion()) {
            eb = EmbedFactory.getEmbedError()
                    .setTitle("❌ Not Supported")
                    .setDescription("Commands via dm are not supported, you need to add Lawliet to a server!");
            actionRowList.add(ActionRow.of(Button.of(ButtonStyle.LINK, ExternalLinks.BOT_INVITE_URL, "Add Lawliet")));
        } else {
            eb = EmbedFactory.getEmbedError()
                    .setTitle("❌ Not Supported")
                    .setDescription("Commands via dm are not supported!");
        }
        return eb;
    }

}
