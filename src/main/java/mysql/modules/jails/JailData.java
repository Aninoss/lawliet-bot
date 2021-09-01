package mysql.modules.jails;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import core.assets.MemberAsset;

public class JailData implements MemberAsset {

    private final long guildId;
    private final long memberId;
    private final Instant expiration;
    private final List<Long> previousRoleIds;

    public JailData(long guildId, long memberId, Instant expiration, List<Long> previousRoleIds) {
        this.guildId = guildId;
        this.memberId = memberId;
        this.expiration = expiration;
        this.previousRoleIds = previousRoleIds;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public long getMemberId() {
        return memberId;
    }

    public Optional<Instant> getExpirationTime() {
        return Optional.ofNullable(expiration);
    }

    public List<Long> getPreviousRoleIds() {
        return previousRoleIds;
    }

}
