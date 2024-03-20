package core.assets;

import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.util.Optional;

public interface GuildMessageChannelAsset extends GuildAsset {

    long getGuildMessageChannelId();

    default Optional<GuildMessageChannel> getGuildMessageChannel() {
        return getGuild().map(guild -> guild.getChannelById(GuildMessageChannel.class, getGuildMessageChannelId()));
    }

}
