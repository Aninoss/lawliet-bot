package core.atomicassets;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import core.CustomObservableList;
import core.ShardManager;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.entities.BaseGuildMessageChannel;
import net.dv8tion.jda.api.entities.Guild;

public class AtomicBaseGuildMessageChannel implements MentionableAtomicAsset<BaseGuildMessageChannel> {

    private final long guildId;
    private final long channelId;

    public AtomicBaseGuildMessageChannel(long guildId, long channelId) {
        this.guildId = guildId;
        this.channelId = channelId;
    }

    public AtomicBaseGuildMessageChannel(BaseGuildMessageChannel channel) {
        channelId = channel.getIdLong();
        guildId = channel.getGuild().getIdLong();
    }

    @Override
    public long getIdLong() {
        return channelId;
    }

    @Override
    public Optional<BaseGuildMessageChannel> get() {
        return ShardManager.getLocalGuildById(guildId)
                .map(guild -> guild.getChannelById(BaseGuildMessageChannel.class, channelId));
    }

    @Override
    public Locale getLocale() {
        return DBGuild.getInstance().retrieve(guildId).getLocale();
    }

    @Override
    public Optional<String> getPrefixedNameRaw() {
        return get().map(c -> "#" + c.getName());
    }

    @Override
    public Optional<String> getNameRaw() {
        return get().map(BaseGuildMessageChannel::getName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtomicBaseGuildMessageChannel that = (AtomicBaseGuildMessageChannel) o;
        return channelId == that.channelId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId);
    }

    public static List<AtomicBaseGuildMessageChannel> from(List<BaseGuildMessageChannel> channels) {
        return channels.stream()
                .map(AtomicBaseGuildMessageChannel::new)
                .collect(Collectors.toList());
    }

    public static List<BaseGuildMessageChannel> to(List<AtomicBaseGuildMessageChannel> channels) {
        return channels.stream()
                .map(AtomicBaseGuildMessageChannel::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static CustomObservableList<AtomicBaseGuildMessageChannel> transformIdList(Guild guild, CustomObservableList<Long> list) {
        return list.transform(
                id -> new AtomicBaseGuildMessageChannel(guild.getIdLong(), id),
                AtomicBaseGuildMessageChannel::getIdLong
        );
    }

}
