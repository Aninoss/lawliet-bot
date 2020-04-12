package MySQL.Modules.FisheryUsers;

import Constants.Settings;

import java.time.Instant;

public class FisheryHourlyIncomeBean {

    private Instant time;
    private long fishIncome;
    private boolean changed = false;
    private long serverId, userId;

    public FisheryHourlyIncomeBean(long serverId, long userId, Instant time, long fishIncome) {
        this.serverId = serverId;
        this.userId = userId;
        this.time = time;
        this.fishIncome = fishIncome;
    }


    /* Getters */

    public long getServerId() {
        return serverId;
    }

    public long getUserId() { return userId; }

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
            this.fishIncome = Math.min(Settings.MAX, this.fishIncome + fish);
            changed = true;
        }
    }

}