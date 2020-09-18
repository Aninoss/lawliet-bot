package mysql.modules.botstats;

public class BotStatsServersSlot {

    private final int month;
    private final int year;
    private final int serverCount;

    public BotStatsServersSlot(int month, int year, int serverCount) {
        this.month = month;
        this.year = year;
        this.serverCount = serverCount;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public int getServerCount() {
        return serverCount;
    }

}
