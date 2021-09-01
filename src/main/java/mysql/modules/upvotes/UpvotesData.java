package mysql.modules.upvotes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import core.CustomObservableMap;

public class UpvotesData {

    private final CustomObservableMap<Long, UpvoteSlot> upvoteMap;

    public UpvotesData(Map<Long, UpvoteSlot> lastUpvote) {
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
