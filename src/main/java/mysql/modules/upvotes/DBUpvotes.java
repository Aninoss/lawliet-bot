package mysql.modules.upvotes;

import java.util.Map;
import mysql.DBDataLoad;
import mysql.DBSingleCache;
import mysql.MySQLManager;

public class DBUpvotes extends DBSingleCache<UpvotesData> {

    private static final DBUpvotes ourInstance = new DBUpvotes();

    public static DBUpvotes getInstance() {
        return ourInstance;
    }

    private DBUpvotes() {
    }

    @Override
    protected UpvotesData loadBean() {
        Map<Long, UpvoteSlot> upvoteMap = new DBDataLoad<UpvoteSlot>("Upvotes", "userId, lastDate, remindersSent", "1")
                .getMap(
                        UpvoteSlot::getUserId,
                        resultSet -> new UpvoteSlot(
                                resultSet.getLong(1),
                                resultSet.getTimestamp(2).toInstant(),
                                resultSet.getInt(3)
                        )
                );

        UpvotesData upvotesData = new UpvotesData(upvoteMap);
        upvotesData.getUpvoteMap().addMapAddListener(this::addUpvote)
                .addMapRemoveListener(this::removeUpvote);

        return upvotesData;
    }

    private void addUpvote(UpvoteSlot upvoteSlot) {
        MySQLManager.asyncUpdate("REPLACE INTO Upvotes (userId, lastDate, remindersSent) VALUES (?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, upvoteSlot.getUserId());
            preparedStatement.setString(2, MySQLManager.instantToDateTimeString(upvoteSlot.getLastUpdate()));
            preparedStatement.setInt(3, upvoteSlot.getRemindersSent());
        });
    }

    private void removeUpvote(UpvoteSlot upvoteSlot) {
        MySQLManager.asyncUpdate("DELETE FROM Upvotes WHERE userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, upvoteSlot.getUserId());
        });
    }

    @Override
    public Integer getExpirationTimeMinutes() {
        return 1;
    }

}
