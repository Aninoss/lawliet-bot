package mysql.modules.upvotes;

import core.assets.UserAsset;
import java.time.Instant;

public class UpvoteSlot implements UserAsset {

    private final long userId;
    private final Instant lastUpdate;

    public UpvoteSlot(long userId, Instant lastUpdate) {
        this.userId = userId;
        this.lastUpdate = lastUpdate;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    public Instant getLastUpdate() {
        return lastUpdate;
    }

}
