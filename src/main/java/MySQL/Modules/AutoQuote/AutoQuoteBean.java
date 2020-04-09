package MySQL.Modules.AutoQuote;

import Core.DiscordApiCollection;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.server.Server;

import java.util.Observable;
import java.util.Optional;

public class AutoQuoteBean extends Observable {

    private long serverId;
    private ServerBean serverBean;
    private boolean active;

    public AutoQuoteBean(long serverId, ServerBean serverBean, boolean active) {
        this.serverId = serverId;
        this.serverBean = serverBean;
        this.active = active;
    }


    /* Getters */

    public long getServerId() {
        return serverId;
    }

    public Optional<Server> getServer() { return DiscordApiCollection.getInstance().getServerById(serverId); }

    public ServerBean getServerBean() {
        return serverBean;
    }

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
