package mysql;

import core.DiscordApiManager;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import org.javacord.api.entity.server.Server;
import java.util.Observable;
import java.util.Optional;

public abstract class BeanWithServer extends Observable {

    private final long serverId;

    public BeanWithServer(long serverId) {
        this.serverId = serverId;
    }

    public ServerBean getServerBean() {
        return DBServer.getInstance().getBean(serverId);
    }

    public long getServerId() {
        return serverId;
    }
    public Optional<Server> getServer() {
        return DiscordApiManager.getInstance().getLocalGuildById(serverId);
    }

}
