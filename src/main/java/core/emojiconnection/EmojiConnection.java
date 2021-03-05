package core.emojiconnection;

import constants.LetterEmojis;
import core.utils.DiscordUtil;
import net.dv8tion.jda.api.entities.TextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;

import java.util.concurrent.CompletableFuture;

public class EmojiConnection {

    private final String emoji;
    private final String connection;

    public EmojiConnection(String emoji, String connection) {
        this.connection = connection;
        this.emoji = emoji;
    }

    public EmojiConnection(Emoji emoji, String connection) {
        this.connection = connection;
        this.emoji = emoji.getMentionTag();
    }

    public CompletableFuture<Void> addReaction(Message message) {
        if (emoji.startsWith("<"))
            return message.addReaction(DiscordUtil.createCustomEmojiFromTag(emoji));
        return message.addReaction(emoji);
    }

    public boolean isEmoji(Emoji emoji) {
        return DiscordUtil.emojiIsString(emoji, this.emoji);
    }

    public String getConnection() {
        return connection;
    }

    public String getEmojiTag() {
        return emoji;
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
        return getOptionsString(channel, withBackButton, -1, connections);
    }

    public static String getOptionsString(TextChannel channel, boolean withBackButton, int pageSize, String... connections) {
        EmojiConnection[] emojiConnections = getEmojiConnectionArray(channel, withBackButton, connections);
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < emojiConnections.length; i++) {
            EmojiConnection emojiConnection = emojiConnections[i];

            if (pageSize != -1 && i == pageSize) sb.append("\n");
            sb.append(emojiConnection.getEmojiTag());
            sb.append(" | ");
            sb.append(emojiConnection.getConnection());
            sb.append("\n");
        }

        String str = sb.toString();

        return str.substring(0,str.length()-1);
    }

}
