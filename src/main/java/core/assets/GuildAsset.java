package core.assets;

import java.util.Optional;
import core.ShardManager;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import net.dv8tion.jda.api.entities.Guild;

public interface GuildAsset {

    long getGuildId();

    default Optional<Guild> getGuild() {
        return ShardManager.getLocalGuildById(getGuildId());
    }

    default GuildData getGuildData() {
        return DBGuild.getInstance().retrieve(getGuildId());
    }

}
