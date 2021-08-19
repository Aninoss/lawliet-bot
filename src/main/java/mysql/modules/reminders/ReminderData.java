package mysql.modules.reminders;

import java.time.Instant;
import core.assets.GuildAsset;
import mysql.DataWithGuild;

public class ReminderData extends DataWithGuild implements GuildAsset {

    private final long id;
    private final long sourceChannelId;
    private final long targetChannelId;
    private final long messageId;
    private final Instant time;
    private final String message;

    public ReminderData(long serverId, long id, long sourceChannelId, long targetChannelId, long messageId, Instant time, String message) {
        super(serverId);
        this.id = id;
        this.sourceChannelId = sourceChannelId;
        this.targetChannelId = targetChannelId;
        this.messageId = messageId;
        this.time = time;
        this.message = message;
    }

    public long getId() {
        return id;
    }

    public long getSourceChannelId() {
        return sourceChannelId != 0 ? sourceChannelId : targetChannelId;
    }

    public long getTargetChannelId() {
        return targetChannelId;
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
