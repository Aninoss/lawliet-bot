package mysql;

import java.util.Observable;
import core.assets.GuildAsset;

public abstract class DataWithGuild extends Observable implements GuildAsset {

    private final long guildId;

    public DataWithGuild(long guildId) {
        this.guildId = guildId;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

}
