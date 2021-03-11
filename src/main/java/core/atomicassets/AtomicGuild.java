package core.atomicassets;

import java.util.Objects;
import java.util.Optional;
import core.ShardManager;
import net.dv8tion.jda.api.entities.Guild;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtomicGuild that = (AtomicGuild) o;
        return guildId == that.guildId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(guildId);
    }

}
