package mysql.modules.upvotes;

import java.util.HashMap;
import mysql.DBDataLoad;
import mysql.MySQLManager;
import mysql.DBSingleCache;

public class DBUpvotes extends DBSingleCache<UpvotesData> {

    private static final DBUpvotes ourInstance = new DBUpvotes();

    public static DBUpvotes getInstance() {
        return ourInstance;
    }

    private DBUpvotes() {
    }

    @Override
    protected UpvotesData loadBean() throws Exception {
        HashMap<Long, UpvoteSlot> upvoteMap = new DBDataLoad<UpvoteSlot>("Upvotes", "userId, lastDate", "1")
                .getHashMap(
                        UpvoteSlot::getUserId,
                        resultSet -> new UpvoteSlot(
                                resultSet.getLong(1),
                                resultSet.getTimestamp(2).toInstant()
                        )
                );

        UpvotesData upvotesData = new UpvotesData(upvoteMap);
        upvotesData.getUpvoteMap().addMapAddListener(this::addUpvote)
                .addMapRemoveListener(this::removeUpvote);

        return upvotesData;
    }

    private void addUpvote(UpvoteSlot upvoteSlot) {
        MySQLManager.asyncUpdate("REPLACE INTO Upvotes (userId, lastDate) VALUES (?,?);", preparedStatement -> {
            preparedStatement.setLong(1, upvoteSlot.getUserId());
            preparedStatement.setString(2, MySQLManager.instantToDateTimeString(upvoteSlot.getLastUpdate()));
        });
    }

    private void removeUpvote(UpvoteSlot upvoteSlot) {
        MySQLManager.asyncUpdate("DELETE FROM Upvotes WHERE userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, upvoteSlot.getUserId());
        });
    }

    public void cleanUp() {
        MySQLManager.asyncUpdate("DELETE FROM Upvotes WHERE DATE_ADD(lastDate, INTERVAL 12 HOUR) < NOW();");
    }

    @Override
    public Integer getExpirationTimeMinutes() {
        return 1;
    }

}
