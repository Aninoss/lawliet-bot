package core.emojiconnection;

import constants.Settings;
import core.DiscordApiCollection;
import org.javacord.api.entity.channel.TextChannel;

public class BackEmojiConnection extends EmojiConnection {

    public BackEmojiConnection(TextChannel channel, String connection) {
        super("", connection);
        setEmoji(channel.canYouUseExternalEmojis(), connection);
    }

    public BackEmojiConnection(boolean customEmoji, String connection) {
        super("", connection);
        setEmoji(customEmoji, connection);
    }

    private void setEmoji(boolean customEmoji, String connection) {
        if (customEmoji) {
            setCustomEmoji(DiscordApiCollection.getInstance().getBackEmojiCustom());
            setCustom(true);
        } else {
            setUnicodeEmoji(Settings.BACK_EMOJI);
            setCustom(false);
        }
    }
}
