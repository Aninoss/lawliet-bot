package mysql.modules.upvotes;

import java.time.Instant;
import core.assets.UserAsset;

public class UpvoteSlot implements UserAsset {

    private final long userId;
    private final Instant lastUpvote;
    private final int remindersSent;

    public UpvoteSlot(long userId, Instant lastUpvote, int remindersSent) {
        this.userId = userId;
        this.lastUpvote = lastUpvote;
        this.remindersSent = remindersSent;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    public Instant getLastUpvote() {
        return lastUpvote;
    }

    public int getRemindersSent() {
        return remindersSent;
    }

}
