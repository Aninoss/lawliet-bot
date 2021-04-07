package mysql.modules.servermute;

import java.time.Instant;
import java.util.Optional;
import core.assets.MemberAsset;

public class ServerMuteSlot implements MemberAsset {

    private final long guildId;
    private final long memberId;
    private final Instant expiration;

    public ServerMuteSlot(long guildId, long memberId, Instant expiration) {
        this.guildId = guildId;
        this.memberId = memberId;
        this.expiration = expiration;
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

}
