package mysql.modules.invitetracking;

import java.time.Instant;
import core.assets.MemberAsset;

public class GuildInvite implements MemberAsset {

    private final long guildId;
    private final String code;
    private final long memberId;
    private final int uses;
    private final Instant maxAge;

    public GuildInvite(long guildId, String code, long memberId, int usages, Instant maxAge) {
        this.guildId = guildId;
        this.code = code;
        this.memberId = memberId;
        this.uses = usages;
        this.maxAge = maxAge;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public long getMemberId() {
        return memberId;
    }

    public String getCode() {
        return code;
    }

    public int getUses() {
        return uses;
    }

    public Instant getMaxAge() {
        return maxAge;
    }

}
