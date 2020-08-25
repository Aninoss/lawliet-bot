package MySQL.Modules.FeatureRequests;

import Constants.FRPanelType;
import MySQL.DBMain;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DBFeatureRequests {

    public static ArrayList<FREntry> fetchEntries(long userId, FRPanelType type) throws SQLException {
        ArrayList<FREntry> list = new ArrayList<>();

        String sql = "SELECT id, public, description, COUNT(`boostDatetime`) AS `boosts`\n" +
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
            list.add(new FREntry(
                    resultSet.getInt(1),
                    resultSet.getBoolean(2),
                    resultSet.getString(3),
                    resultSet.getInt(4)
            ));
        }

        resultSet.close();
        preparedStatement.close();

        return list;
    }

    public static int fetchBoostsThisMonth(long userId) throws SQLException {
        int ret = -1;

        String sql = "SELECT COUNT(*) FROM FeatureRequestBoosts\n" +
                "WHERE boostUserId = ? AND DATE_FORMAT(boostDatetime, '%Y-%m-01') = DATE_FORMAT(NOW(), '%Y-%m-01');";

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

}
