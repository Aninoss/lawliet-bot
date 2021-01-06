package mysql.modules.autoquote;

import mysql.BeanWithServer;

public class AutoQuoteBean extends BeanWithServer {

    private boolean active;

    public AutoQuoteBean(long serverId, boolean active) {
        super(serverId);
        this.active = active;
    }


    /* Getters */

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
