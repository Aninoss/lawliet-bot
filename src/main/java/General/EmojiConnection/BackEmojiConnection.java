package General.EmojiConnection;

import General.Shortcuts;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;

public class BackEmojiConnection extends EmojiConnection {
    public BackEmojiConnection(TextChannel channel, String connection) {
        super("", connection);
        setEmoji(channel.getApi(), channel.canYouUseExternalEmojis(), connection);
    }

    public BackEmojiConnection(DiscordApi api, boolean customEmoji, String connection) {
        super("", connection);
        setEmoji(api, customEmoji, connection);
    }

    private void setEmoji(DiscordApi api, boolean customEmoji, String connection) {
        if (customEmoji) {
            setCustomEmoji(Shortcuts.getBackEmojiCustom(api));
            setCustom(true);
        } else {
            setUnicodeEmoji(Shortcuts.getBackEmojiUnicode());
            setCustom(false);
        }
    }
}
