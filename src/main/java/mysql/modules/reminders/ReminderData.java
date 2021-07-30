package mysql.modules.reminders;

import java.time.Instant;
import core.assets.TextChannelAsset;
import mysql.DataWithGuild;

public class ReminderData extends DataWithGuild implements TextChannelAsset {

    private final long id;
    private final long channelId;
    private final long messageId;
    private final Instant time;
    private final String message;

    public ReminderData(long serverId, long id, long channelId, long messageId, Instant time, String message) {
        super(serverId);
        this.id = id;
        this.channelId = channelId;
        this.messageId = messageId;
        this.time = time;
        this.message = message;
    }

    public long getId() {
        return id;
    }

    @Override
    public long getTextChannelId() {
        return channelId;
    }

    public long getMessageId() {
        return messageId;
    }

    public Instant getTime() {
        return time;
    }

    public String getMessage() {
        return message;
    }

}
