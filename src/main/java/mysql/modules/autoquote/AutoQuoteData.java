package mysql.modules.autoquote;

import mysql.DataWithGuild;

public class AutoQuoteData extends DataWithGuild {

    private boolean active;

    public AutoQuoteData(long serverId, boolean active) {
        super(serverId);
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void toggleActive() {
        this.active = !this.active;
        setChanged();
        notifyObservers();
    }

    public void setActive(boolean active) {
        if (this.isActive() != active) toggleActive();
    }

}
