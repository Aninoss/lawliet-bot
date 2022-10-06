package mysql.modules.upvotes;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import mysql.DBDataLoad;
import mysql.MySQLManager;

public class DBUpvotes {

    public static UpvoteSlot getUpvoteSlot(long userId) {
        try {
            return MySQLManager.get(
                    "SELECT lastDate, remindersSent FROM Upvotes WHERE userId = ?;",
                    preparedStatement -> preparedStatement.setLong(1, userId),
                    resultSet -> {
                        if (resultSet.next()) {
                            return new UpvoteSlot(
                                    userId,
                                    resultSet.getTimestamp(1).toInstant(),
                                    resultSet.getInt(2)
                            );
                        } else {
                            return new UpvoteSlot(
                                    userId,
                                    Instant.now().minus(Duration.ofHours(24)),
                                    0
                            );
                        }
                    }
            );
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<UpvoteSlot> getAllUpvoteSlots() {
        return new DBDataLoad<UpvoteSlot>("Upvotes", "userId, lastDate, remindersSent", "1")
                .getList(resultSet -> new UpvoteSlot(
                                resultSet.getLong(1),
                                resultSet.getTimestamp(2).toInstant(),
                                resultSet.getInt(3)
                        )
                );
    }

    public static void saveUpvoteSlot(UpvoteSlot upvoteSlot) {
        MySQLManager.asyncUpdate("REPLACE INTO Upvotes (userId, lastDate, remindersSent) VALUES (?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, upvoteSlot.getUserId());
            preparedStatement.setString(2, MySQLManager.instantToDateTimeString(upvoteSlot.getLastUpvote()));
            preparedStatement.setInt(3, upvoteSlot.getRemindersSent());
        });
    }

}
