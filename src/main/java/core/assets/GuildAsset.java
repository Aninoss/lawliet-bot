package core.assets;

import core.ShardManager;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Optional;

public interface GuildAsset {

    long getGuildId();

    default Optional<Guild> getGuild() {
        return ShardManager.getLocalGuildById(getGuildId());
    }

}
