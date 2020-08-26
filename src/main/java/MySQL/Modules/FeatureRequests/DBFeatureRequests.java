package MySQL.Modules.FeatureRequests;

import Constants.FRPanelType;
import MySQL.DBMain;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DBFeatureRequests {

    public static ArrayList<FREntryBean> fetchEntries(long userId, FRPanelType type) throws SQLException {
        ArrayList<FREntryBean> list = new ArrayList<>();

        String sql = "SELECT id, public, title, description, COUNT(`boostDatetime`) AS `boosts`\n" +
                "FROM FeatureRequests\n" +
                "LEFT JOIN FeatureRequestBoosts USING (`id`)\n" +
                "WHERE `type` = ? AND (`public` = 1 OR `userId` = ?)\n" +
                "GROUP BY `id`\n" +
                "ORDER BY `boosts` DESC;";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setString(1, type.name());
        preparedStatement.setLong(2, userId);
        preparedStatement.execute();
        ResultSet resultSet = preparedStatement.getResultSet();

        while(resultSet.next()) {
            list.add(new FREntryBean(
                    resultSet.getInt(1),
                    resultSet.getBoolean(2),
                    resultSet.getString(3),
                    resultSet.getString(4),
                    resultSet.getInt(5)
            ));
        }

        resultSet.close();
        preparedStatement.close();

        return list;
    }

    public static int fetchBoostsThisWeek(long userId) throws SQLException {
        int ret = -1;

        String sql = "SELECT COUNT(*) FROM FeatureRequestBoosts\n" +
                "WHERE boostUserId = ? AND CONCAT(YEAR(boostDatetime), \"/\", WEEK(boostDatetime)) = CONCAT(YEAR(NOW()), \"/\", WEEK(NOW()));";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setLong(1, userId);
        preparedStatement.execute();
        ResultSet resultSet = preparedStatement.getResultSet();

        if (resultSet.next())
            ret = resultSet.getInt(1);

        resultSet.close();
        preparedStatement.close();

        return ret;
    }

    public static void insertBoost(int id, long userId) throws SQLException {
        String sql = "INSERT INTO FeatureRequestBoosts(id, boostDatetime, boostUserId) VALUES (?, NOW(), ?);";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.setLong(2, userId);
        preparedStatement.execute();
        preparedStatement.close();
    }

    public static void postFeatureRequest(long userId, String title, String desc) throws SQLException {
        String sql = "INSERT INTO FeatureRequests(userId, date, type, public, title, description) VALUES (?, CURDATE(), ?, 0, ?, ?);";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setLong(1, userId);
        preparedStatement.setString(2, FRPanelType.PENDING.name());
        preparedStatement.setString(3, title);
        preparedStatement.setString(4, desc);
        preparedStatement.execute();
        preparedStatement.close();
    }

    public static boolean canPost(long userId) throws SQLException {
        boolean canPost = false;
        String sql = "SELECT COUNT(*) FROM FeatureRequests WHERE userId = ? AND public = 0 AND type = ?;";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setLong(1, userId);
        preparedStatement.setString(2, FRPanelType.PENDING.name());
        preparedStatement.execute();
        ResultSet resultSet = preparedStatement.getResultSet();

        if (resultSet.next())
            canPost = resultSet.getInt(1) == 0;

        resultSet.close();
        preparedStatement.close();
        return canPost;
    }

}
