package core.emoji;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import constants.Emojis;
import core.GlobalThreadPool;
import core.MainLogger;
import core.Program;

public class EmojiTable {

    private static final EmojiTable ourInstance = new EmojiTable();

    public static EmojiTable getInstance() {
        return ourInstance;
    }

    private final ArrayList<String> emojis = new ArrayList<>();

    private EmojiTable() {
    }

    public void load() throws IOException {
        if (Program.isProductionMode()) {
            GlobalThreadPool.getExecutorService().submit(() -> {
                try {
                    EmojiUnicodePointAndValueMaker emojiUnicodePointAndValueMaker = new EmojiUnicodePointAndValueMaker();

                    MainLogger.get().info("Downloading emoji lists...");
                    emojiUnicodePointAndValueMaker.build("https://unicode.org/emoji/charts/full-emoji-modifiers.html")
                            .forEach(emoji -> this.emojis.add(emoji.toEmoji()));

                    emojiUnicodePointAndValueMaker.build("https://unicode.org/emoji/charts/full-emoji-list.html")
                            .forEach(emoji -> this.emojis.add(emoji.toEmoji()));

                    emojis.addAll(List.of(Emojis.LETTERS));
                    MainLogger.get().info("Emoji lists completed with {} emojis", emojis.size());
                } catch (Throwable e) {
                    MainLogger.get().error("EXIT - Exception on emoji load", e);
                    System.exit(1);
                }
            });
        }
    }

    public Optional<String> extractFirstEmoji(String input) {
        Optional<String> emojiResult = Optional.empty();
        int maxLength = 0;

        for (String emoji : emojis) {
            if (input.contains(emoji) && emoji.length() > maxLength) {
                emojiResult = Optional.of(emoji);
                maxLength = emoji.length();
            }
        }
        return emojiResult;
    }

}
