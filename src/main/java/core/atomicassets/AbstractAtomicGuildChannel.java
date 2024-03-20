package core.atomicassets;

import core.ShardManager;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.util.Objects;
import java.util.Optional;

public abstract class AbstractAtomicGuildChannel<T extends GuildChannel> implements MentionableAtomicAsset<T> {

    private final Class<T> type;
    private final long guildId;
    private final long channelId;

    public AbstractAtomicGuildChannel(Class<T> type, long guildId, long channelId) {
        this.type = type;
        this.guildId = guildId;
        this.channelId = channelId;
    }

    @Override
    public long getIdLong() {
        return channelId;
    }

    @Override
    public Optional<T> get() {
        return ShardManager.getLocalGuildById(guildId)
                .map(guild -> guild.getChannelById(type, channelId));
    }

    @Override
    public Optional<String> getPrefixedNameRaw() {
        return get().map(c -> {
            if (c instanceof ThreadChannel) {
                return "#" + ((ThreadChannel) c).getParentChannel().getName() + " → " + c.getName();
            } else if (c instanceof AudioChannel) {
                return "🔊" + c.getName();
            } else if (c instanceof Category) {
                return c.getName();
            } else {
                return "#" + c.getName();
            }
        });
    }

    @Override
    public Optional<String> getNameRaw() {
        return get().map(T::getName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractAtomicGuildChannel<T> that = (AbstractAtomicGuildChannel<T>) o;
        return channelId == that.channelId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId);
    }

}
