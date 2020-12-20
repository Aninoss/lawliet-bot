package mysql.modules.upvotes;

import java.time.Instant;

public class UpvoteSlot {

    private final long userId;
    private final Instant lastUpdate;

    public UpvoteSlot(long userId, Instant lastUpdate) {
        this.userId = userId;
        this.lastUpdate = lastUpdate;
    }

    public long getUserId() {
        return userId;
    }

    public Instant getLastUpdate() {
        return lastUpdate;
    }

}
