package mysql.modules.bump;

import java.sql.SQLException;
import java.time.Instant;
import mysql.MySQLManager;

public class DBBump {

    public static void setNextBump(Instant instant) throws SQLException {
        MySQLManager.asyncUpdate(
                "UPDATE Bump SET next = ?;",
                preparedStatement -> preparedStatement.setString(1, MySQLManager.instantToDateTimeString(instant))
        );
    }

    public static Instant getNextBump() throws SQLException, InterruptedException {
        return MySQLManager.get(
                "SELECT next FROM Bump;",
                resultSet -> {
                    if (resultSet.next()) {
                        return resultSet.getTimestamp(1).toInstant();
                    } else {
                        return null;
                    }
                }
        );
    }

}
