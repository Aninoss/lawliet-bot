package core.assets;

import net.dv8tion.jda.api.entities.VoiceChannel;
import java.util.Optional;

public interface VoiceChannelAsset extends GuildAsset {

    long getVoiceChannelId();

    default Optional<VoiceChannel> getVoiceChannel() {
        return getGuild().map(guild -> guild.getVoiceChannelById(getVoiceChannelId()));
    }

}
