package mysql.modules.fisheryusers;

import constants.FisheryGear;
import core.assets.MemberAsset;
import core.utils.NumberUtil;

public class FisheryMemberGearData implements MemberAsset {

    private final long guildId;
    private final long memberId;
    private final FisheryGear gear;
    private int level;
    private boolean changed = false;

    public FisheryMemberGearData(long guildId, long memberId, FisheryGear gear, int level) {
        this.guildId = guildId;
        this.memberId = memberId;
        this.gear = gear;
        this.level = level;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public long getMemberId() {
        return memberId;
    }

    public FisheryGear getGear() {
        return gear;
    }

    public int getLevel() {
        return level;
    }

    public boolean checkChanged() {
        boolean changedTemp = changed;
        changed = false;
        return changedTemp;
    }

    void setLevel(int level) {
        this.level = level;
        changed = true;
    }

    public void setChanged() {
        changed = true;
    }

    public long getPrice() {
        return NumberUtil.flattenLong(Math.round(Math.pow(getValue(level), 1.02) * gear.getStartPrice()), 4);
    }

    public long getEffect() {
        return getValue(level) * gear.getEffect();
    }

    public long getDeltaEffect() {
        return (getValue(level + 1) - getValue(level)) * gear.getEffect();
    }

    public static long getValue(long level) {
        long n = level + 1;
        return n * (n + 1) / 2;
    }

}
