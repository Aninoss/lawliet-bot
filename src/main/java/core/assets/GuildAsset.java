package core.assets;

import core.ShardManager;
import mysql.modules.server.DBServer;
import mysql.modules.server.GuildBean;
import net.dv8tion.jda.api.entities.Guild;
import java.util.Optional;

public interface GuildAsset {

    long getGuildId();

    default Optional<Guild> getGuild() {
        return ShardManager.getInstance().getLocalGuildById(getGuildId());
    }

    default GuildBean getGuildBean() {
        return DBServer.getInstance().retrieve(getGuildId());
    }

}
