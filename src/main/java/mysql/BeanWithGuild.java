package mysql;

import core.ShardManager;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import net.dv8tion.jda.api.entities.Guild;
import java.util.Observable;
import java.util.Optional;

public abstract class BeanWithGuild extends Observable {

    private final long guildId;

    public BeanWithGuild(long guildId) {
        this.guildId = guildId;
    }

    public ServerBean getGuildBean() {
        return DBServer.getInstance().retrieve(guildId);
    }

    public long getGuildId() {
        return guildId;
    }

    public Optional<Guild> getGuild() {
        return ShardManager.getInstance().getLocalGuildById(guildId);
    }

}
