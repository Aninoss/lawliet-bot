package mysql.modules.botstats;

import java.util.concurrent.ExecutionException;
import core.MainLogger;
import mysql.DBMain;
import websockets.TopGG;

public class DBBotStats {

    public static void saveStatsServers(long serverCount) {
        DBMain.getInstance().asyncUpdate(
                "INSERT INTO StatsServerCount VALUES(NOW(), ?);",
                preparedStatement -> preparedStatement.setLong(1, serverCount)
        );
    }

    public static void saveStatsCommandUsages() {
        DBMain.getInstance().asyncUpdate("INSERT INTO StatsCommandUsages VALUES(NOW(), (SELECT SUM(usages) FROM CommandUsages));");
    }

    public static void saveStatsUpvotes() {
        DBMain.getInstance().asyncUpdate("INSERT INTO StatsUpvotes VALUES(NOW(), ?, ?);", preparedStatement -> {
            try {
                preparedStatement.setInt(1, TopGG.getInstance().getTotalUpvotes());
                preparedStatement.setInt(2, TopGG.getInstance().getMonthlyUpvotes());
            } catch (InterruptedException | ExecutionException e) {
                MainLogger.get().error("Error while fetching topgg upvotes", e);
            }
        });
    }

}
