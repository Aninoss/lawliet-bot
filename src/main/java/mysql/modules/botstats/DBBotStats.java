package mysql.modules.botstats;

import java.util.concurrent.ExecutionException;
import core.MainLogger;
import mysql.MySQLManager;
import core.botlists.TopGG;

public class DBBotStats {

    public static void saveStatsServers(long serverCount) {
        MySQLManager.asyncUpdate(
                "INSERT INTO StatsServerCount VALUES(NOW(), ?);",
                preparedStatement -> preparedStatement.setLong(1, serverCount)
        );
    }

    public static void saveStatsCommandUsages() {
        MySQLManager.asyncUpdate("INSERT INTO StatsCommandUsages VALUES(NOW(), (SELECT SUM(usages) FROM CommandUsages));");
    }

    public static void saveStatsUpvotes() {
        MySQLManager.asyncUpdate("INSERT INTO StatsUpvotes VALUES(NOW(), ?, ?);", preparedStatement -> {
            try {
                preparedStatement.setLong(1, TopGG.getTotalUpvotes());
                preparedStatement.setLong(2, TopGG.getMonthlyUpvotes());
            } catch (InterruptedException | ExecutionException e) {
                MainLogger.get().error("Error while fetching topgg upvotes", e);
            }
        });
    }

}
