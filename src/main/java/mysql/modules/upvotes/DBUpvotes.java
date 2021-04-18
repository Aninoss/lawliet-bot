package mysql.modules.upvotes;

import java.util.HashMap;
import core.Program;
import mysql.DBDataLoad;
import mysql.DBMain;
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
        upvotesData.getUpvoteMap().addMapAddListener(this::addUpvote);

        return upvotesData;
    }

    private void addUpvote(UpvoteSlot upvoteSlot) {
        if (Program.getClusterId() == 1) {
            DBMain.getInstance().asyncUpdate("REPLACE INTO Upvotes (userId, lastDate) VALUES (?,?);", preparedStatement -> {
                preparedStatement.setLong(1, upvoteSlot.getUserId());
                preparedStatement.setString(2, DBMain.instantToDateTimeString(upvoteSlot.getLastUpdate()));
            });
        }
    }

    public void cleanUp() {
        DBMain.getInstance().asyncUpdate("DELETE FROM Upvotes WHERE DATE_ADD(lastDate, INTERVAL 12 HOUR) < NOW();");
    }

    @Override
    public Integer getExpirationTimeMinutes() {
        return 5;
    }

}
