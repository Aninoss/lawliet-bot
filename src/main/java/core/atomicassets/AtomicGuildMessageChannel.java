package core.atomicassets;

import core.CustomObservableList;
import core.ShardManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AtomicGuildMessageChannel implements MentionableAtomicAsset<GuildMessageChannel> {

    private final long guildId;
    private final long channelId;

    public AtomicGuildMessageChannel(long guildId, long channelId) {
        this.guildId = guildId;
        this.channelId = channelId;
    }

    public AtomicGuildMessageChannel(GuildMessageChannel channel) {
        channelId = channel.getIdLong();
        guildId = channel.getGuild().getIdLong();
    }

    @Override
    public long getIdLong() {
        return channelId;
    }

    @Override
    public Optional<GuildMessageChannel> get() {
        return ShardManager.getLocalGuildById(guildId)
                .map(guild -> guild.getChannelById(GuildMessageChannel.class, channelId));
    }

    @Override
    public Optional<String> getPrefixedNameRaw() {
        return get().map(c -> "#" + c.getName());
    }

    @Override
    public Optional<String> getNameRaw() {
        return get().map(GuildMessageChannel::getName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtomicGuildMessageChannel that = (AtomicGuildMessageChannel) o;
        return channelId == that.channelId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId);
    }

    public static List<AtomicGuildMessageChannel> from(List<GuildMessageChannel> channels) {
        return channels.stream()
                .map(AtomicGuildMessageChannel::new)
                .collect(Collectors.toList());
    }

    public static List<GuildMessageChannel> to(List<AtomicGuildMessageChannel> channels) {
        return channels.stream()
                .map(AtomicGuildMessageChannel::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static CustomObservableList<AtomicGuildMessageChannel> transformIdList(Guild guild, CustomObservableList<Long> list) {
        return list.transform(
                id -> new AtomicGuildMessageChannel(guild.getIdLong(), id),
                AtomicGuildMessageChannel::getIdLong
        );
    }

}
