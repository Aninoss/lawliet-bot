package core.assets;

import java.util.Optional;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;

public interface StandardGuildMessageChannelAsset extends GuildAsset {

    long getStandardGuildMessageChannelId();

    default Optional<StandardGuildMessageChannel> getStandardGuildMessageChannel() {
        return getGuild().map(guild -> guild.getChannelById(StandardGuildMessageChannel.class, getStandardGuildMessageChannelId()));
    }

}
