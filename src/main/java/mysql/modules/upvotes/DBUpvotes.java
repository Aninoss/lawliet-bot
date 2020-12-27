package mysql.modules.upvotes;

import core.Bot;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBSingleBeanGenerator;
import java.util.HashMap;

public class DBUpvotes extends DBSingleBeanGenerator<UpvotesBean> {

    private static final DBUpvotes ourInstance = new DBUpvotes();
    public static DBUpvotes getInstance() { return ourInstance; }
    private DBUpvotes() {}

    @Override
    protected UpvotesBean loadBean() throws Exception {
        HashMap<Long, UpvoteSlot> upvoteMap = new DBDataLoad<UpvoteSlot>("Upvotes", "userId, lastDate", "1",
                preparedStatement -> {}
        ).getHashMap(
                UpvoteSlot::getUserId,
                resultSet -> new UpvoteSlot(
                        resultSet.getLong(1),
                        resultSet.getTimestamp(2).toInstant()
                )
        );

        UpvotesBean upvotesBean = new UpvotesBean(upvoteMap);
        upvotesBean.getUpvoteMap().addMapAddListener(this::addUpvote);

        return upvotesBean;
    }

    private void addUpvote(UpvoteSlot upvoteSlot) {
        if (Bot.getClusterId() == 0) {
            DBMain.getInstance().asyncUpdate("REPLACE INTO Upvotes (userId, lastDate) VALUES (?,?);", preparedStatement -> {
                preparedStatement.setLong(1, upvoteSlot.getUserId());
                preparedStatement.setString(2, DBMain.instantToDateTimeString(upvoteSlot.getLastUpdate()));
            });
        }
    }

    public void cleanUp() {
        DBMain.getInstance().asyncUpdate("DELETE FROM Upvotes WHERE DATE_ADD(lastDate, INTERVAL 12 HOUR) < NOW();", preparedStatement -> {});
    }

    @Override
    public Integer getExpirationTimeMinutes() {
        return 5;
    }

}
