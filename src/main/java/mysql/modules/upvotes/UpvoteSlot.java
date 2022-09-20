package mysql.modules.upvotes;

import java.time.Instant;
import core.assets.UserAsset;

public class UpvoteSlot implements UserAsset {

    private final long userId;
    private final Instant lastUpdate;
    private final int remindersSent;

    public UpvoteSlot(long userId, Instant lastUpdate, int remindersSent) {
        this.userId = userId;
        this.lastUpdate = lastUpdate;
        this.remindersSent = remindersSent;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    public Instant getLastUpdate() {
        return lastUpdate;
    }

    public int getRemindersSent() {
        return remindersSent;
    }

}
