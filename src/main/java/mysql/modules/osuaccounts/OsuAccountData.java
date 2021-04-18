package mysql.modules.osuaccounts;

import core.assets.UserAsset;

public class OsuAccountData implements UserAsset {

    private final long userId;
    private final long osuId;

    public OsuAccountData(long userId, long osuId) {
        this.userId = userId;
        this.osuId = osuId;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    public long getOsuId() {
        return osuId;
    }

}
