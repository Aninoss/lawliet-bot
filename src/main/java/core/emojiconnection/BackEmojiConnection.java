package core.emojiconnection;

import constants.Emojis;
import org.javacord.api.entity.channel.TextChannel;

public class BackEmojiConnection extends EmojiConnection {

    public BackEmojiConnection(TextChannel channel, String connection) {
        super(channel.canYouUseExternalEmojis() ? Emojis.BACK_EMOJI : Emojis.BACK_EMOJI_UNICODE, connection);
    }

    public BackEmojiConnection(boolean customEmoji, String connection) {
        super(customEmoji ? Emojis.BACK_EMOJI : Emojis.BACK_EMOJI_UNICODE, connection);
    }
}
