package mysql.modules.botstats;

import mysql.DBMain;
import websockets.TopGG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DBBotStats {

    private final static Logger LOGGER = LoggerFactory.getLogger(DBBotStats.class);

    public static void saveStatsServers(int serverCount) {
        DBMain.getInstance().asyncUpdate("INSERT INTO StatsServerCount VALUES(NOW(), ?);",
                preparedStatement -> preparedStatement.setInt(1, serverCount)
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

    public static List<BotStatsServersSlot> getMonthlyServerStats() throws SQLException {
        ArrayList<BotStatsServersSlot> slots = new ArrayList<>();

        Statement statement = DBMain.getInstance().statementExecuted("SELECT (MONTH(`date`) - 1) AS mon, YEAR(`date`) AS yea, MAX(`count`), MIN(`date`) AS dat FROM StatsServerCount GROUP BY mon, yea ORDER BY dat DESC LIMIT 13;");
        ResultSet resultSet = statement.getResultSet();

        while(resultSet.next()) {
            BotStatsServersSlot slot = new BotStatsServersSlot(
                    resultSet.getInt(1),
                    resultSet.getInt(2),
                    resultSet.getInt(3)
            );
            slots.add(0, slot);
        }

        resultSet.close();
        statement.close();

        return slots;
    }

}
