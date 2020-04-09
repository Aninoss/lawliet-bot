package MySQL.Modules.Upvotes;

import java.time.Instant;
import java.util.Observable;

public class UpvotesBean extends Observable {

    private long userId;
    private Instant lastUpvote;

    public UpvotesBean(long userId, Instant lastUpvote) {
        this.userId = userId;
        this.lastUpvote = lastUpvote;
    }


    /* Getters */

    public long getUserId() {
        return userId;
    }

    public Instant getLastUpvote() { return lastUpvote; }


    /* Setters */

    public void updateLastUpvote() {
        this.lastUpvote = Instant.now();
        setChanged();
        notifyObservers();
    }

}
