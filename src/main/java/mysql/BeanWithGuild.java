package mysql;

import java.util.Observable;
import core.assets.GuildAsset;

public abstract class BeanWithGuild extends Observable implements GuildAsset {

    private final long guildId;

    public BeanWithGuild(long guildId) {
        this.guildId = guildId;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

}
