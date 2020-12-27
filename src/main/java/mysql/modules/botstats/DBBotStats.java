package mysql.modules.botstats;

import mysql.DBMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.TopGG;

import java.util.concurrent.ExecutionException;

public class DBBotStats {

    private final static Logger LOGGER = LoggerFactory.getLogger(DBBotStats.class);

    public static void saveStatsServers(long serverCount) {
        DBMain.getInstance().asyncUpdate("INSERT INTO StatsServerCount VALUES(NOW(), ?);",
                preparedStatement -> preparedStatement.setLong(1, serverCount)
        );
    }

    public static void saveStatsCommandUsages() {
        DBMain.getInstance().asyncUpdate("INSERT INTO StatsCommandUsages VALUES(NOW(), (SELECT SUM(usages) FROM CommandUsages));",
                preparedStatement -> {}
        );
    }

    public static void saveStatsUpvotes() {
        DBMain.getInstance().asyncUpdate("INSERT INTO StatsUpvotes VALUES(NOW(), ?, ?);", preparedStatement -> {
            try {
                preparedStatement.setInt(1, TopGG.getInstance().getTotalUpvotes());
                preparedStatement.setInt(2, TopGG.getInstance().getMonthlyUpvotes());
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("Error while fetching topgg upvotes", e);
            }
        });
    }

}
