package mysql.modules.osuaccounts;

import core.assets.UserAsset;

public class OsuBeanBean implements UserAsset {

    private final long userId;
    private final long osuId;

    public OsuBeanBean(long userId, long osuId) {
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
