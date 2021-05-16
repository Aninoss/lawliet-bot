package mysql.modules.ticket;

import core.assets.MemberAsset;
import core.assets.TextChannelAsset;

public class TicketChannel implements TextChannelAsset, MemberAsset {

    private final long guildId;
    private final long channelId;
    private final long userId;
    private final long messageChannelId;
    private final long messageMessageId;

    public TicketChannel(long guildId, long channelId, long userId, long messageChannelId, long messageMessageId) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.userId = userId;
        this.messageChannelId = messageChannelId;
        this.messageMessageId = messageMessageId;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public long getTextChannelId() {
        return channelId;
    }

    @Override
    public long getMemberId() {
        return userId;
    }

    public long getAnnouncementChannelId() {
        return messageChannelId;
    }

    public long getAnnouncementMessageId() {
        return messageMessageId;
    }

}
