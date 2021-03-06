package mysql.modules.membercountdisplays;

import core.ShardManager;
import net.dv8tion.jda.api.entities.VoiceChannel;
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

    public Optional<VoiceChannel> getVoiceChannel() {
        return ShardManager.getInstance().getLocalGuildById(serverId).map(guild -> guild.getVoiceChannelById(voiceChannelId));
    }

    public String getMask() {
        return mask;
    }

}
