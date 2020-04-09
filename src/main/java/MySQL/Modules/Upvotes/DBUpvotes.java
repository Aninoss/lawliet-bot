package MySQL.Modules.Upvotes;

import MySQL.DBBeanGenerator;
import MySQL.DBDataLoad;
import MySQL.DBMain;
import MySQL.Modules.AutoRoles.AutoRolesBean;
import MySQL.Modules.SPBlock.SPBlockBean;
import MySQL.Modules.Server.DBServer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

public class DBUpvotes extends DBBeanGenerator<Long, UpvotesBean> {

    private static DBUpvotes ourInstance = new DBUpvotes();
    public static DBUpvotes getInstance() { return ourInstance; }
    private DBUpvotes() {}

    @Override
    protected UpvotesBean loadBean(Long userId) throws Exception {
        UpvotesBean upvotesBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT lastDate FROM Upvotes WHERE userId = ?;");
        preparedStatement.setLong(1, userId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            upvotesBean = new UpvotesBean(
                    userId,
                    resultSet.getTimestamp(1).toInstant()
            );
        } else {
            upvotesBean = new UpvotesBean(
                    userId,
                    Instant.now()
            );
        }

        resultSet.close();
        preparedStatement.close();

        return upvotesBean;
    }

    @Override
    protected void saveBean(UpvotesBean upvotesBean) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO Upvotes (userId, lastDate) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, upvotesBean.getUserId());
            preparedStatement.setString(2, DBMain.instantToDateTimeString(upvotesBean.getLastUpvote()));
        });
    }

}
