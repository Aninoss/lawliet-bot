package MySQL.Modules.NSFWFilter;

import Core.CustomObservableList;
import Core.DiscordApiCollection;
import MySQL.Modules.Server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.server.Server;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Optional;

public class NSFWFiltersBean extends Observable {

    private long serverId;
    private ServerBean serverBean;
    private CustomObservableList<String> keywords;

    public NSFWFiltersBean(long serverId, ServerBean serverBean, @NonNull ArrayList<String> keywords) {
        this.serverId = serverId;
        this.serverBean = serverBean;
        this.keywords = new CustomObservableList<>(keywords);
    }


    /* Getters */

    public long getServerId() {
        return serverId;
    }

    public Optional<Server> getServer() { return DiscordApiCollection.getInstance().getServerById(serverId); }

    public ServerBean getServerBean() {
        return serverBean;
    }

    public CustomObservableList<String> getKeywords() { return keywords; }

}
