package core.emojiconnection;

import constants.Emojis;
import core.utils.BotPermissionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;

public class BackEmojiConnection extends EmojiConnection {

    public BackEmojiConnection(TextChannel channel, String connection) {
        super(BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_EXT_EMOJI) ? Emojis.BACK_EMOTE : Emojis.BACK_UNICODE, connection);
    }

    public BackEmojiConnection(boolean customEmoji, String connection) {
        super(customEmoji ? Emojis.BACK_EMOTE : Emojis.BACK_UNICODE, connection);
    }

}
