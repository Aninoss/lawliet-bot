package mysql.modules.tempban;

import java.time.Instant;
import core.assets.MemberAsset;
import mysql.BeanWithGuild;

public class TempBanSlot extends BeanWithGuild implements MemberAsset {

    private final long memberId;
    private final Instant expires;

    public TempBanSlot(long guildId,long memberId, Instant expires) {
        super(guildId);
        this.memberId = memberId;
        this.expires = expires;
    }

    @Override
    public long getMemberId() {
        return memberId;
    }

    public Instant getExpirationTime() {
        return expires;
    }

}
