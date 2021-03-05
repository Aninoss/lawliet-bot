package mysql.modules.membercountdisplays;

import core.DiscordApiManager;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import java.util.Optional;

public class MemberCountDisplaySlot {

    private final long serverId, voiceChannelId;
    private final String mask;

    public MemberCountDisplaySlot(long serverId, long voiceChannelId, String mask) {
        this.serverId = serverId;
        this.voiceChannelId = voiceChannelId;
        this.mask = mask;
    }

    public long getVoiceChannelId() {
        return voiceChannelId;
    }

    public Optional<ServerVoiceChannel> getVoiceChannel() {
        return DiscordApiManager.getInstance().getLocalGuildById(serverId).flatMap(server -> server.getVoiceChannelById(voiceChannelId));
    }

    public String getMask() {
        return mask;
    }

}
