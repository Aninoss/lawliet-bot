package mysql.modules.reminders;

import java.time.Instant;
import core.assets.TextChannelAsset;
import mysql.BeanWithGuild;

public class ReminderSlot extends BeanWithGuild implements TextChannelAsset {

    private final int id;
    private final long channelId;
    private final Instant time;
    private final String message;
    private final Runnable completedRunnable;

    public ReminderSlot(long serverId, int id, long channelId, Instant time, String message) {
        this(serverId, id, channelId, time, message, null);
    }

    public ReminderSlot(long serverId, int id, long channelId, Instant time, String message, Runnable completedRunnable) {
        super(serverId);
        this.id = id;
        this.channelId = channelId;
        this.time = time;
        this.message = message;
        this.completedRunnable = completedRunnable;
    }

    public int getId() {
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
