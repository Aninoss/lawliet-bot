package mysql;

import core.ShardManager;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import net.dv8tion.jda.api.entities.Guild;
import java.util.Observable;
import java.util.Optional;

public abstract class BeanWithServer extends Observable {

    private final long guildId;

    public BeanWithServer(long guildId) {
        this.guildId = guildId;
    }

    public ServerBean getServerBean() {
        return DBServer.getInstance().getBean(guildId);
    }

    public long getGuildId() {
        return guildId;
    }

    public Optional<Guild> getServer() {
        return ShardManager.getInstance().getLocalGuildById(guildId);
    }

}
