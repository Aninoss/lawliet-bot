package core.assets;

import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.util.Optional;

public interface GuildChannelAsset extends GuildAsset {

    long getGuildChannelId();

    default Optional<GuildChannel> getGuildChannel() {
        return getGuild().map(guild -> guild.getChannelById(GuildChannel.class, getGuildChannelId()));
    }

}
