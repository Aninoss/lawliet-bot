package core.atomicassets;

import core.ShardManager;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Objects;
import java.util.Optional;

public class AtomicTextChannel implements AtomicAsset<TextChannel> {

    private final long guildId;
    private final long channelId;

    public AtomicTextChannel(long guildId, long channelId) {
        this.guildId = guildId;
        this.channelId = channelId;
    }

    public AtomicTextChannel(TextChannel channel) {
        channelId = channel.getIdLong();
        guildId = channel.getGuild().getIdLong();
    }

    @Override
    public long getId() {
        return channelId;
    }

    @Override
    public Optional<TextChannel> get() {
        return ShardManager.getInstance().getLocalGuildById(guildId)
                .flatMap(guild -> Optional.ofNullable(guild.getTextChannelById(channelId)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtomicTextChannel that = (AtomicTextChannel) o;
        return channelId == that.channelId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId);
    }

}
