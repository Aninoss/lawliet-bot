package core.atomicassets;

import core.CustomObservableList;
import core.ShardManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AtomicGuildChannel implements MentionableAtomicAsset<GuildChannel> {

    private final long guildId;
    private final long channelId;

    public AtomicGuildChannel(long guildId, long channelId) {
        this.guildId = guildId;
        this.channelId = channelId;
    }

    public AtomicGuildChannel(GuildChannel channel) {
        channelId = channel.getIdLong();
        guildId = channel.getGuild().getIdLong();
    }

    @Override
    public long getIdLong() {
        return channelId;
    }

    @Override
    public Optional<GuildChannel> get() {
        return ShardManager.getLocalGuildById(guildId)
                .map(guild -> guild.getChannelById(GuildChannel.class, channelId));
    }

    @Override
    public Optional<String> getPrefixedNameRaw() {
        return get().map(c -> "#" + c.getName());
    }

    @Override
    public Optional<String> getNameRaw() {
        return get().map(GuildChannel::getName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtomicGuildChannel that = (AtomicGuildChannel) o;
        return channelId == that.channelId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId);
    }

    public static List<AtomicGuildChannel> from(List<GuildChannel> channels) {
        return channels.stream()
                .map(AtomicGuildChannel::new)
                .collect(Collectors.toList());
    }

    public static List<GuildChannel> to(List<AtomicGuildChannel> channels) {
        return channels.stream()
                .map(AtomicGuildChannel::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static CustomObservableList<AtomicGuildChannel> transformIdList(Guild guild, CustomObservableList<Long> list) {
        return list.transform(
                id -> new AtomicGuildChannel(guild.getIdLong(), id),
                AtomicGuildChannel::getIdLong
        );
    }

}
