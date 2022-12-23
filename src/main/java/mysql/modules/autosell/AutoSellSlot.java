package mysql.modules.autosell;

import core.assets.UserAsset;

public class AutoSellSlot implements UserAsset {

    private final long userId;
    private final int threshold;

    public AutoSellSlot(long userId, int threshold) {
        this.userId = userId;
        this.threshold = threshold;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    public int getThreshold() {
        return threshold;
    }

}
