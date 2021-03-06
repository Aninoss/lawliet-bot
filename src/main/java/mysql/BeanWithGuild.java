package mysql;

import core.assets.GuildAsset;

import java.util.Observable;

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
