package modules;

import java.util.List;
import core.assets.MessageAsset;
import core.emojiconnection.EmojiConnection;

public class ReactionMessage implements MessageAsset {

    private final long guildId;
    private final long channelId;
    private final long messageId;
    private final String title;
    private final String description;
    private final boolean removeRole;
    private final boolean multipleRoles;
    private final List<EmojiConnection> emojiConnections;

    public ReactionMessage(long guildId, long channelId, long messageId, String title, String description, boolean removeRole, boolean multipleRoles, List<EmojiConnection> emojiConnections) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageId = messageId;
        this.title = title;
        this.description = description;
        this.removeRole = removeRole;
        this.multipleRoles = multipleRoles;
        this.emojiConnections = emojiConnections;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public long getMessageId() {
        return messageId;
    }

    @Override
    public long getTextChannelId() {
        return channelId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRemoveRole() {
        return removeRole;
    }

    public boolean isMultipleRoles() {
        return multipleRoles;
    }

    public List<EmojiConnection> getEmojiConnections() {
        return emojiConnections;
    }

}
