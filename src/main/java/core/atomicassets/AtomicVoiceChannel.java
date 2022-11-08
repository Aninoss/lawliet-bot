package core.atomicassets;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import core.CustomObservableList;
import core.ShardManager;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

public class AtomicVoiceChannel implements MentionableAtomicAsset<VoiceChannel> {

    private final long guildId;
    private final long channelId;

    public AtomicVoiceChannel(long guildId, long channelId) {
        this.guildId = guildId;
        this.channelId = channelId;
    }

    public AtomicVoiceChannel(VoiceChannel channel) {
        channelId = channel.getIdLong();
        guildId = channel.getGuild().getIdLong();
    }

    @Override
    public long getIdLong() {
        return channelId;
    }

    @Override
    public Optional<VoiceChannel> get() {
        return ShardManager.getLocalGuildById(guildId)
                .map(guild -> guild.getVoiceChannelById(channelId));
    }

    @Override
    public Locale getLocale() {
        return DBGuild.getInstance().retrieve(guildId).getLocale();
    }

    @Override
    public Optional<String> getPrefixedNameRaw() {
        return get().map(v -> "@" + v.getName());
    }

    @Override
    public Optional<String> getNameRaw() {
        return get().map(Channel::getName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtomicVoiceChannel that = (AtomicVoiceChannel) o;
        return channelId == that.channelId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId);
    }

    public static List<AtomicVoiceChannel> from(List<VoiceChannel> channels) {
        return channels.stream()
                .map(AtomicVoiceChannel::new)
                .collect(Collectors.toList());
    }

    public static List<VoiceChannel> to(List<AtomicVoiceChannel> channels) {
        return channels.stream()
                .map(AtomicVoiceChannel::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static CustomObservableList<AtomicVoiceChannel> transformIdList(Guild guild, CustomObservableList<Long> list) {
        return list.transform(
                id -> new AtomicVoiceChannel(guild.getIdLong(), id),
                AtomicVoiceChannel::getIdLong
        );
    }

}
