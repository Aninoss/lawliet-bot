package mysql.modules.bump;

import java.sql.SQLException;
import java.time.Instant;
import mysql.DBMain;

public class DBBump {

    public static void setNextBump(Instant instant) throws SQLException {
        DBMain.getInstance().asyncUpdate(
                "UPDATE Bump SET next = ?;",
                preparedStatement -> preparedStatement.setString(1, DBMain.instantToDateTimeString(instant))
        );
    }

    public static Instant getNextBump() throws SQLException, InterruptedException {
        return DBMain.getInstance().get(
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
