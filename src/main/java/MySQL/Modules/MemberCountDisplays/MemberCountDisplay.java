package MySQL.Modules.MemberCountDisplays;

import Core.DiscordApiCollection;
import org.javacord.api.entity.channel.ServerVoiceChannel;

import java.util.Optional;

public class MemberCountDisplay {

    private final long serverId, voiceChannelId;
    private final String mask;

    public MemberCountDisplay(long serverId, long voiceChannelId, String mask) {
        this.serverId = serverId;
        this.voiceChannelId = voiceChannelId;
        this.mask = mask;
    }

    public long getVoiceChannelId() {
        return voiceChannelId;
    }

    public Optional<ServerVoiceChannel> getVoiceChannel() {
        return DiscordApiCollection.getInstance().getServerById(serverId).flatMap(server -> server.getVoiceChannelById(voiceChannelId));
    }

    public String getMask() {
        return mask;
    }

}
