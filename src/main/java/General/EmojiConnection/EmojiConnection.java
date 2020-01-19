package General.EmojiConnection;

import Constants.LetterEmojis;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;

import java.util.concurrent.CompletableFuture;

public class EmojiConnection {

    private String unicodeEmoji, connection;
    private CustomEmoji customEmoji;
    private boolean custom = false;

    public EmojiConnection(String emoji, String connection) {
        this.unicodeEmoji = emoji;
        this.connection = connection;
    }

    public EmojiConnection(CustomEmoji customEmoji, String connection) {
        this.customEmoji = customEmoji;
        this.connection = connection;
        custom = true;
    }

    public EmojiConnection(Emoji emoji, String connection) {
        this.connection = connection;
        if (emoji.isUnicodeEmoji()) this.unicodeEmoji =  emoji.asUnicodeEmoji().get();
        if (emoji.isCustomEmoji()) {
            this.customEmoji = emoji.asCustomEmoji().get();
            custom = true;
        }
    }

    public CompletableFuture<Void> addReaction(Message message) {
        if (!custom) return message.addReaction(unicodeEmoji);
        return message.addReaction(customEmoji);
    }

    public boolean isEmoji(Emoji emoji) {
        if (emoji.isUnicodeEmoji() && !custom) return emoji.asUnicodeEmoji().get().equals(unicodeEmoji);
        if (emoji.isCustomEmoji() && custom) return emoji.asCustomEmoji().get().equals(customEmoji);
        return false;
    }

    public String getConnection() {
        return connection;
    }

    public String getEmojiTag() {
        if (!custom) return unicodeEmoji;
        return customEmoji.getMentionTag();
    }

    public static EmojiConnection[] getEmojiConnectionArray(TextChannel channel, boolean withBackButton, String... connections) {
        EmojiConnection[] array = new EmojiConnection[connections.length];
        int index = 0;
        for(int i=0; i<array.length; i++) {
            if (withBackButton) {
                array[i] = new BackEmojiConnection(channel, connections[i]);
                withBackButton = false;
            } else {
                array[i] = new EmojiConnection(LetterEmojis.LETTERS[index],connections[i]);
                index++;
            }
        }

        return array;
    }

    public static String getOptionsString(EmojiConnection... emojiConnections) {
        StringBuilder sb = new StringBuilder();

        for(EmojiConnection emojiConnection: emojiConnections) { ;
            sb.append(emojiConnection.getEmojiTag());
            sb.append(" | ");
            sb.append(emojiConnection.getConnection());
            sb.append("\n");
        }

        return sb.toString();
    }

    public static String getOptionsString(TextChannel channel, boolean withBackButton, String... connections) {
        EmojiConnection[] emojiConnections = getEmojiConnectionArray(channel, withBackButton, connections);
        StringBuilder sb = new StringBuilder();

        for(EmojiConnection emojiConnection: emojiConnections) {
            sb.append(emojiConnection.getEmojiTag());
            sb.append(" | ");
            sb.append(emojiConnection.getConnection());
            sb.append("\n");
        }

        String str = sb.toString();

        return str.substring(0,str.length()-1);
    }

    public void setUnicodeEmoji(String unicodeEmoji) {
        this.unicodeEmoji = unicodeEmoji;
    }

    public void setCustomEmoji(CustomEmoji customEmoji) {
        this.customEmoji = customEmoji;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }
}
