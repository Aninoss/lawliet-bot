package mysql.modules.autoquote;

import mysql.BeanWithServer;
import mysql.modules.server.ServerBean;

public class AutoQuoteBean extends BeanWithServer {

    private boolean active;

    public AutoQuoteBean(ServerBean serverBean, boolean active) {
        super(serverBean);
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
