package core.assets;

import java.util.Optional;
import net.dv8tion.jda.api.entities.BaseGuildMessageChannel;

public interface BaseGuildMessageChannelAsset extends GuildAsset {

    long getBaseMessageChannelId();

    default Optional<BaseGuildMessageChannel> getBaseGuildMessageChannel() {
        return getGuild().map(guild -> guild.getChannelById(BaseGuildMessageChannel.class, getBaseMessageChannelId()));
    }

}
