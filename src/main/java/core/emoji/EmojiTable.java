package core.emoji;

import constants.Emojis;
import core.LocalFile;
import core.MainLogger;
import javafx.util.Pair;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;

import java.util.ArrayList;
import java.util.Optional;

public class EmojiTable {

    private static final ArrayList<Pair<String, String>> emojis = new ArrayList<>();

    public static void load() {
        for (int i = 0; i < Emojis.LETTERS.length; i++) {
            String name = "regional_indicator_" + (char) ('a' + i);
            String emoji = Emojis.LETTERS[i].getFormatted();
            emojis.add(new Pair<>(name, emoji));
        }

        try {
            EmojiUnicodePointAndValueMaker emojiUnicodePointAndValueMaker = new EmojiUnicodePointAndValueMaker();

            MainLogger.get().info("Downloading emoji lists...");
            emojiUnicodePointAndValueMaker.build(new LocalFile(LocalFile.Directory.EMOJIS, "full-emoji-modifiers.htm"))
                    .forEach(emoji -> emojis.add(new Pair<>(emoji.getName(), emoji.toEmoji())));

            emojiUnicodePointAndValueMaker.build(new LocalFile(LocalFile.Directory.EMOJIS, "full-emoji-list.htm"))
                    .forEach(emoji -> emojis.add(new Pair<>(emoji.getName(), emoji.toEmoji())));

            MainLogger.get().info("Emoji lists completed with {} emojis", emojis.size());
        } catch (Throwable e) {
            MainLogger.get().error("Exception on emoji load", e);
        }
    }

    public static Optional<UnicodeEmoji> extractFirstUnicodeEmoji(String input) {
        Optional<String> emojiResult = Optional.empty();
        int maxLength = 0;

        for (Pair<String, String> emojiPair : emojis) {
            String emoji = emojiPair.getValue();
            if (input.contains(emoji) && emoji.length() > maxLength) {
                emojiResult = Optional.of(emoji);
                maxLength = emoji.length();
            }
        }
        return emojiResult.map(net.dv8tion.jda.api.entities.emoji.Emoji::fromUnicode);
    }

    public static ArrayList<Pair<String, String>> getEmojis() {
        return emojis;
    }

    public static String getEmojiName(String emoji) {
        for (Pair<String, String> pair : emojis) {
            if (pair.getValue().equals(emoji)) {
                return pair.getKey();
            }
        }
        return "";
    }

}
