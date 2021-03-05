package core.atomicassets;

import core.ShardManager;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Optional;

public class AtomicGuild implements AtomicAsset<Guild> {

    private final long guildId;

    public AtomicGuild(long guildId) {
        this.guildId = guildId;
    }

    public AtomicGuild(Guild guild) {
        guildId = guild.getIdLong();
    }

    @Override
    public long getId() {
        return guildId;
    }

    @Override
    public Optional<Guild> get() {
        return ShardManager.getInstance().getLocalGuildById(guildId);
    }

}
