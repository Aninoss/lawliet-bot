package core.atomicassets;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import core.CustomObservableList;
import core.ShardManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.TextChannel;

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
        return ShardManager.getInstance().getLocalGuildById(guildId)
                .map(guild -> guild.getTextChannelById(channelId));
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

    public static CustomObservableList<AtomicTextChannel> transformIdList(Guild guild, CustomObservableList<Long> list) {
        return list.transform(
                id -> Optional.ofNullable(guild.getTextChannelById(id)).map(AtomicTextChannel::new).orElse(null),
                atomic -> atomic.get().map(ISnowflake::getIdLong).orElse(null)
        );
    }

}
