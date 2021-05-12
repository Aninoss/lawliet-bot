package mysql.modules.ticket;

import core.assets.GuildAsset;

public class TicketChannel implements GuildAsset {

    private final long guildId;
    private final long channelId;
    private final long messageChannelId;
    private final long messageMessageId;

    public TicketChannel(long guildId, long channelId, long messageChannelId, long messageMessageId) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageChannelId = messageChannelId;
        this.messageMessageId = messageMessageId;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    public long getMessageChannelId() {
        return messageChannelId;
    }

    public long getMessageMessageId() {
        return messageMessageId;
    }

    public long getChannelId() {
        return channelId;
    }

}
