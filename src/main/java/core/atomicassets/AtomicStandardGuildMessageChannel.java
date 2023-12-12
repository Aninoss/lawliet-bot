package core.atomicassets;

import core.CustomObservableList;
import core.ShardManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AtomicStandardGuildMessageChannel implements MentionableAtomicAsset<StandardGuildMessageChannel> {

    private final long guildId;
    private final long channelId;

    public AtomicStandardGuildMessageChannel(Guild guild, long channelId) {
        this.guildId = guild.getIdLong();
        this.channelId = channelId;
    }

    public AtomicStandardGuildMessageChannel(long guildId, long channelId) {
        this.guildId = guildId;
        this.channelId = channelId;
    }

    public AtomicStandardGuildMessageChannel(StandardGuildMessageChannel channel) {
        channelId = channel.getIdLong();
        guildId = channel.getGuild().getIdLong();
    }

    @Override
    public long getIdLong() {
        return channelId;
    }

    @Override
    public Optional<StandardGuildMessageChannel> get() {
        return ShardManager.getLocalGuildById(guildId)
                .map(guild -> guild.getChannelById(StandardGuildMessageChannel.class, channelId));
    }

    @Override
    public Optional<String> getPrefixedNameRaw() {
        return get().map(c -> "#" + c.getName());
    }

    @Override
    public Optional<String> getNameRaw() {
        return get().map(StandardGuildMessageChannel::getName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtomicStandardGuildMessageChannel that = (AtomicStandardGuildMessageChannel) o;
        return channelId == that.channelId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId);
    }

    public static List<AtomicStandardGuildMessageChannel> from(List<StandardGuildMessageChannel> channels) {
        return channels.stream()
                .map(AtomicStandardGuildMessageChannel::new)
                .collect(Collectors.toList());
    }

    public static List<StandardGuildMessageChannel> to(List<AtomicStandardGuildMessageChannel> channels) {
        return channels.stream()
                .map(AtomicStandardGuildMessageChannel::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static CustomObservableList<AtomicStandardGuildMessageChannel> transformIdList(Guild guild, CustomObservableList<Long> list) {
        return list.transform(
                id -> new AtomicStandardGuildMessageChannel(guild.getIdLong(), id),
                AtomicStandardGuildMessageChannel::getIdLong
        );
    }

}
