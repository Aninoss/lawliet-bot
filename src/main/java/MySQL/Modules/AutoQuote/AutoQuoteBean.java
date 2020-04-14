package MySQL.Modules.AutoQuote;

import Core.DiscordApiCollection;
import MySQL.BeanWithServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.server.Server;

import java.util.Observable;
import java.util.Optional;

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
