package core.emojiconnection;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import core.utils.EmojiUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.requests.RestAction;

public class EmojiConnection {

    private final Emoji emoji;
    private final String connection;

    public EmojiConnection(Emoji emoji, String connection) {
        this.connection = connection;
        this.emoji = emoji;
    }

    @Nonnull
    @CheckReturnValue
    public RestAction<Void> addReaction(Message message) {
        return message.addReaction(emoji);
    }

    public boolean isEmoji(Emoji emoji) {
        return EmojiUtil.equals(this.emoji, emoji);
    }

    public String getConnection() {
        return connection;
    }

    public Emoji getEmoji() {
        return emoji;
    }

}
