package mysql.modules.fisheryusers;

import constants.FisheryCategoryInterface;
import core.assets.MemberAsset;
import core.utils.NumberUtil;

public class FisheryMemberPowerUpBean implements MemberAsset {

    private final long guildId;
    private final long memberId;
    private final int powerUpId;
    private int level;
    private final long startPrice, effect;
    private boolean changed = false;

    public FisheryMemberPowerUpBean(long guildId, long memberId, int powerUpId, int level) {
        this.guildId = guildId;
        this.memberId = memberId;
        this.powerUpId = powerUpId;
        this.level = level;
        this.startPrice = FisheryCategoryInterface.START_PRICE[powerUpId];
        this.effect = FisheryCategoryInterface.EFFECT[powerUpId];
    }


    /* Getters */

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public long getMemberId() {
        return memberId;
    }

    public int getPowerUpId() {
        return powerUpId;
    }

    public int getLevel() {
        return level;
    }

    public boolean checkChanged() {
        boolean changedTemp = changed;
        changed = false;
        return changedTemp;
    }


    /* Setters */

    void setLevel(int level) {
        this.level = level;
        changed = true;
    }

    public void setChanged() {
        changed = true;
    }


    /* Tools */

    public long getPrice() {
        return NumberUtil.flattenLong(Math.round(Math.pow(getValue(level), 1.02) * startPrice), 4);
    }

    public long getEffect() {
        return getValue(level) * effect;
    }

    public long getDeltaEffect() {
        return (getValue(level + 1) - getValue(level)) * effect;
    }

    public static long getValue(long level) {
        long n = level + 1;
        return n * (n + 1) / 2;
    }

}
