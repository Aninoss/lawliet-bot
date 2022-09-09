package mysql.modules.bannedusers;

import core.assets.UserAsset;

public class BannedUserSlot implements UserAsset {

    private final long userId;
    private final String reason;

    public BannedUserSlot(long userId, String reason) {
        this.userId = userId;
        this.reason = reason;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    public String getReason() {
        return reason;
    }

}
