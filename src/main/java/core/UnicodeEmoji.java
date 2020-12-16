package core;

import org.javacord.api.entity.emoji.Emoji;
import java.util.HashMap;
import java.util.Optional;

public class UnicodeEmoji implements Emoji {

    private static final HashMap<String, UnicodeEmoji> emojiCache = new HashMap<>();

    private final String unicode;

    private UnicodeEmoji(String unicode) {
        this.unicode = unicode;
    }

    @Override
    public Optional<String> asUnicodeEmoji() {
        return Optional.of(unicode);
    }

    @Override
    public boolean isAnimated() {
        return false;
    }

    @Override
    public String getMentionTag() {
        return unicode;
    }

    public static UnicodeEmoji fromString(String emoji) {
        return emojiCache.computeIfAbsent(emoji, e -> new UnicodeEmoji(emoji));
    }

}
