package mysql.modules.reminders;

import java.time.Instant;
import core.assets.TextChannelAsset;
import mysql.DataWithGuild;

public class ReminderData extends DataWithGuild implements TextChannelAsset {

    private final long id;
    private final long channelId;
    private final Instant time;
    private final String message;
    private final Runnable completedRunnable;

    public ReminderData(long serverId, long id, long channelId, Instant time, String message) {
        this(serverId, id, channelId, time, message, null);
    }

    public ReminderData(long serverId, long id, long channelId, Instant time, String message, Runnable completedRunnable) {
        super(serverId);
        this.id = id;
        this.channelId = channelId;
        this.time = time;
        this.message = message;
        this.completedRunnable = completedRunnable;
    }

    public long getId() {
        return id;
    }

    @Override
    public long getTextChannelId() {
        return channelId;
    }

    public Instant getTime() {
        return time;
    }

    public String getMessage() {
        return message;
    }

    public Runnable getCompletedRunnable() {
        return completedRunnable;
    }

}
