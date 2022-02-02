package mysql.modules.membercountdisplays;

import core.assets.VoiceChannelAsset;
import org.checkerframework.checker.nullness.qual.NonNull;

public class MemberCountDisplaySlot implements VoiceChannelAsset {

    private final long guildId;
    private final long voiceChannelId;
    private final String mask;

    public MemberCountDisplaySlot(long guildId, long voiceChannelId, @NonNull String mask) {
        this.guildId = guildId;
        this.voiceChannelId = voiceChannelId;
        this.mask = mask;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public long getVoiceChannelId() {
        return voiceChannelId;
    }

    public String getMask() {
        return mask;
    }

}
