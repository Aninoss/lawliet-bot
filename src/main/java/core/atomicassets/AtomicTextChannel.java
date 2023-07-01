package core.atomicassets;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import core.CustomObservableList;
import core.ShardManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class AtomicTextChannel implements MentionableAtomicAsset<TextChannel> {

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
    public long getIdLong() {
        return channelId;
    }

    @Override
    public Optional<TextChannel> get() {
        return ShardManager.getLocalGuildById(guildId)
                .map(guild -> guild.getTextChannelById(channelId));
    }

    @Override
    public Optional<String> getPrefixedNameRaw() {
        return get().map(c -> "#" + c.getName());
    }

    @Override
    public Optional<String> getNameRaw() {
        return get().map(TextChannel::getName);
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

    public static List<AtomicTextChannel> from(List<TextChannel> channels) {
        return channels.stream()
                .map(AtomicTextChannel::new)
                .collect(Collectors.toList());
    }

    public static List<TextChannel> to(List<AtomicTextChannel> channels) {
        return channels.stream()
                .map(AtomicTextChannel::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static CustomObservableList<AtomicTextChannel> transformIdList(Guild guild, CustomObservableList<Long> list) {
        return list.transform(
                id -> new AtomicTextChannel(guild.getIdLong(), id),
                AtomicTextChannel::getIdLong
        );
    }

}
