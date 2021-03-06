package core.atomicassets;

import core.ShardManager;
import net.dv8tion.jda.api.entities.VoiceChannel;
import java.util.Objects;
import java.util.Optional;

public class AtomicVoiceChannel implements AtomicAsset<VoiceChannel> {

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
    public long getId() {
        return channelId;
    }

    @Override
    public Optional<VoiceChannel> get() {
        return ShardManager.getInstance().getLocalGuildById(guildId)
                .map(guild -> guild.getVoiceChannelById(channelId));
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

}
