package core.assets;

import java.util.Optional;
import net.dv8tion.jda.api.entities.VoiceChannel;

public interface VoiceChannelAsset extends GuildAsset {

    long getVoiceChannelId();

    default Optional<VoiceChannel> getVoiceChannel() {
        return getGuild().map(guild -> guild.getVoiceChannelById(getVoiceChannelId()));
    }

}
