package core.emoji;

import constants.Emojis;
import core.LocalFile;
import core.MainLogger;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class EmojiTable {

    private static final ArrayList<String> emojis = new ArrayList<>();

    public static void load() {
        Arrays.stream(Emojis.LETTERS)
                .map(UnicodeEmoji::getFormatted)
                .forEach(emojis::add);

        try {
            EmojiUnicodePointAndValueMaker emojiUnicodePointAndValueMaker = new EmojiUnicodePointAndValueMaker();

            MainLogger.get().info("Downloading emoji lists...");
            emojiUnicodePointAndValueMaker.build(new LocalFile(LocalFile.Directory.EMOJIS, "full-emoji-modifiers.htm"))
                    .forEach(emoji -> emojis.add(emoji.toEmoji()));

            emojiUnicodePointAndValueMaker.build(new LocalFile(LocalFile.Directory.EMOJIS, "full-emoji-list.htm"))
                    .forEach(emoji -> emojis.add(emoji.toEmoji()));

            MainLogger.get().info("Emoji lists completed with {} emojis", emojis.size());
        } catch (Throwable e) {
            MainLogger.get().error("Exception on emoji load", e);
        }
    }

    public static Optional<UnicodeEmoji> extractFirstUnicodeEmoji(String input) {
        Optional<String> emojiResult = Optional.empty();
        int maxLength = 0;

        for (String emoji : emojis) {
            if (input.contains(emoji) && emoji.length() > maxLength) {
                emojiResult = Optional.of(emoji);
                maxLength = emoji.length();
            }
        }
        return emojiResult.map(net.dv8tion.jda.api.entities.emoji.Emoji::fromUnicode);
    }

}
