package modules;

import commands.Category;
import constants.Emojis;
import core.EmbedFactory;
import core.TextManager;
import mysql.hibernate.entity.GiveawayEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

public class Giveaway {

    public static EmbedBuilder getMessageEmbed(Locale locale, GiveawayEntity giveaway) {
        return getMessageEmbed(locale, giveaway, giveaway.getCreated());
    }

    public static EmbedBuilder getMessageEmbed(Locale locale, GiveawayEntity giveaway, Instant created) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(giveaway.getItem())
                .setDescription(giveaway.getDescription())
                .setFooter(TextManager.getString(locale, TextManager.GENERAL, "serverstaff_text"));

        String tutText = TextManager.getString(
                locale,
                Category.CONFIGURATION,
                "giveaway_tutorial",
                giveaway.getWinners() != 1,
                giveaway.getEmojiFormatted(),
                String.valueOf(giveaway.getWinners()),
                TimeFormat.RELATIVE.atInstant(created.plus(Duration.ofMinutes(giveaway.getDurationMinutes()))).toString()
        );

        if (giveaway.getDescription() == null) {
            eb.setDescription(tutText);
        } else {
            eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), tutText, false);
        }

        if (giveaway.getImageFilename() != null) {
            eb.setImage(giveaway.getImageUrl());
        }
        return eb;
    }

}
