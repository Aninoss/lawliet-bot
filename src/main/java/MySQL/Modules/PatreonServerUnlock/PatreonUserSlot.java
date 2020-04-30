package MySQL.Modules.PatreonServerUnlock;

import java.time.Instant;
import java.util.Observable;

public class PatreonUserSlot extends Observable {

    private final long userId;
    private Instant unlockTime;

    public PatreonUserSlot(long userId, Instant unlockTime) {
        this.userId = userId;
        this.unlockTime = unlockTime;
    }

    public long getUserId() {
        return userId;
    }

    public Instant getUnlockTime() {
        return unlockTime;
    }

    public void updateUnlockTime() {
        unlockTime = Instant.now();
        setChanged();
        notifyObservers();
    }

}
