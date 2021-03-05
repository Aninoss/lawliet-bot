package core.atomicassets;

import core.ShardManager;
import net.dv8tion.jda.api.entities.TextChannel;
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

}
