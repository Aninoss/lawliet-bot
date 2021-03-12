package core.emoji;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import core.GlobalThreadPool;
import core.MainLogger;

public class EmojiTable {

    private static final EmojiTable ourInstance = new EmojiTable();

    public static EmojiTable getInstance() {
        return ourInstance;
    }

    private final ArrayList<String> emojis = new ArrayList<>();

    private EmojiTable() {
    }

    public void load() throws IOException {
        GlobalThreadPool.getExecutorService().submit(() -> {
            try {
                EmojiUnicodePointAndValueMaker emojiUnicodePointAndValueMaker = new EmojiUnicodePointAndValueMaker();

                MainLogger.get().info("Downloading emoji lists...");
                emojiUnicodePointAndValueMaker.build("https://unicode.org/emoji/charts/full-emoji-modifiers.html")
                        .forEach(emoji -> this.emojis.add(emoji.toEmoji()));

                emojiUnicodePointAndValueMaker.build("https://unicode.org/emoji/charts/full-emoji-list.html")
                        .forEach(emoji -> this.emojis.add(emoji.toEmoji()));

                MainLogger.get().info("Emoji lists completed with {} emojis", emojis.size());
            } catch (Throwable e) {
                MainLogger.get().error("EXIT - Exception on emoji load", e);
                System.exit(1);
            }
        });
    }

    public Optional<String> extractFirstEmoji(String input) {
        for (String emoji : emojis) {
            if (input.contains(emoji)) {
                return Optional.of(emoji);
            }
        }
        return Optional.empty();
    }

}
