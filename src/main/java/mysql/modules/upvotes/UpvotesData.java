package mysql.modules.upvotes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import core.CustomObservableMap;
import org.checkerframework.checker.nullness.qual.NonNull;

public class UpvotesData {

    private final CustomObservableMap<Long, UpvoteSlot> upvoteMap;

    public UpvotesData(@NonNull HashMap<Long, UpvoteSlot> lastUpvote) {
        this.upvoteMap = new CustomObservableMap<>(lastUpvote);
    }

    public CustomObservableMap<Long, UpvoteSlot> getUpvoteMap() {
        return upvoteMap;
    }

    public Instant getLastUpvote(long userId) {
        if (upvoteMap.containsKey(userId)) {
            return upvoteMap.get(userId).getLastUpdate();
        } else {
            return Instant.now().minus(24, ChronoUnit.HOURS);
        }
    }

    public void updateLastUpvote(long userId) {
        upvoteMap.put(userId, new UpvoteSlot(userId, Instant.now()));
    }

}
