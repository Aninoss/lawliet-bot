package mysql.modules.fisheryusers;

import constants.Settings;
import core.assets.MemberAsset;

import java.time.Instant;

public class FisheryHourlyIncomeBean implements MemberAsset {

    private final Instant time;
    private long fishIncome;
    private boolean changed = false;
    private final long guildId, memberId;

    public FisheryHourlyIncomeBean(long guildId, long memberId, Instant time, long fishIncome) {
        this.guildId = guildId;
        this.memberId = memberId;
        this.time = time;
        this.fishIncome = fishIncome;
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

    public Instant getTime() { return time; }

    public long getFishIncome() { return fishIncome; }

    public boolean checkChanged() {
        boolean changedTemp = changed;
        changed = false;
        return changedTemp;
    }


    /* Setters */

    void add(long fish) {
        if (fish > 0) {
            this.fishIncome = Math.min(Settings.FISHERY_MAX, this.fishIncome + fish);
            changed = true;
        }
    }

    public void setChanged() {
        changed = true;
    }

}