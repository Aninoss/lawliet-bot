package mysql.modules.membercountdisplays;

import core.assets.VoiceChannelAsset;

public class MemberCountDisplaySlot implements VoiceChannelAsset {

    private final long guildId;
    private final long voiceChannelId;
    private final String mask;

    public MemberCountDisplaySlot(long guildId, long voiceChannelId, String mask) {
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
