package mysql.modules.autoclaim;

import java.util.Observable;

public class AutoClaimBean extends Observable {

    private final long userId;
    private boolean active;

    public AutoClaimBean(long userId, boolean active) {
        this.userId = userId;
        this.active = active;
    }


    /* Getters */

    public long getUserId() { return userId; }

    public boolean isActive() {
        return active;
    }


    /* Setters */

    public void toggleActive() {
        this.active = !this.active;
        setChanged();
        notifyObservers();
    }

    public void setActive(boolean active) {
        if (this.isActive() != active) toggleActive();
    }

}
