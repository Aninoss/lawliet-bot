package mysql.modules.servermute;

import java.time.Instant;
import java.util.Optional;
import core.assets.MemberAsset;

public class ServerMuteData implements MemberAsset {

    private final long guildId;
    private final long memberId;
    private final Instant expiration;
    private final boolean newMethod;

    public ServerMuteData(long guildId, long memberId, Instant expiration, boolean newMethod) {
        this.guildId = guildId;
        this.memberId = memberId;
        this.expiration = expiration;
        this.newMethod = newMethod;
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

    public boolean isNewMethod() {
        return newMethod;
    }

}
