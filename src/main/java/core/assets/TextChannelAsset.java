package core.assets;

import java.util.Optional;
import net.dv8tion.jda.api.entities.TextChannel;

public interface TextChannelAsset extends GuildAsset {

    long getTextChannelId();

    default Optional<TextChannel> getTextChannel() {
        return getGuild().map(guild -> guild.getTextChannelById(getTextChannelId()));
    }

}
