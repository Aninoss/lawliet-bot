package modules;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import commands.Category;
import constants.Emojis;
import core.EmbedFactory;
import core.TextManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.TimeFormat;

public class Giveaway {

    public static EmbedBuilder getMessageEmbed(Locale locale, String title, String desc, int amountOfWinners,
                                               Emoji emoji, long durationMinutes, String imageUrl, Instant instant) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle("🎆 " + title)
                .setDescription(desc)
                .setFooter(TextManager.getString(locale, TextManager.GENERAL, "serverstaff_text"));

        String tutText = TextManager.getString(
                locale,
                Category.UTILITY,
                "giveaway_tutorial",
                amountOfWinners != 1,
                emoji.getFormatted(),
                String.valueOf(amountOfWinners),
                TimeFormat.RELATIVE.atInstant(instant.plus(Duration.ofMinutes(durationMinutes))).toString()
        );

        if (desc == null || desc.isEmpty()) {
            eb.setDescription(tutText);
        } else {
            eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), tutText, false);
        }

        if (imageUrl != null) {
            eb.setImage(imageUrl);
        }
        return eb;
    }

}
