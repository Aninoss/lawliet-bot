package mysql.modules.reminders;

import mysql.BeanWithServer;
import mysql.modules.server.ServerBean;

import java.time.Instant;

public class RemindersBean extends BeanWithServer {

    private final int id;
    private final long channelId;
    private final Instant time;
    private final String message;
    private boolean active = true;
    private final Runnable completedRunnable;

    public RemindersBean(ServerBean serverBean, int id, long channelId, Instant time, String message) {
        this(serverBean, id, channelId, time, message, null);
    }

    public RemindersBean(ServerBean serverBean, int id, long channelId, Instant time, String message, Runnable completedRunnable) {
        super(serverBean);
        this.id = id;
        this.channelId = channelId;
        this.time = time;
        this.message = message;
        this.completedRunnable = completedRunnable;
    }

    public int getId() {
        return id;
    }

    public long getChannelId() {
        return channelId;
    }

    public Instant getTime() {
        return time;
    }

    public String getMessage() {
        return message;
    }

    public void stop() {
        active = false;
    }

    public Runnable getCompletedRunnable() {
        return completedRunnable;
    }

    public boolean isActive() {
        return active;
    }

}
