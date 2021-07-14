package mysql.modules.gamestatistics;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import core.Program;
import mysql.DBMain;
import mysql.DBObserverMapCache;

public class DBGameStatistics extends DBObserverMapCache<String, GameStatisticsData> {

    private static final DBGameStatistics ourInstance = new DBGameStatistics();

    public static DBGameStatistics getInstance() {
        return ourInstance;
    }

    private DBGameStatistics() {
    }

    @Override
    protected GameStatisticsData load(String command) throws Exception {
        try (PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT won, value FROM GameStatistics WHERE game = ?;")) {
            preparedStatement.setString(1, command);
            preparedStatement.execute();

            double[] values = { 0.0, 0.0 };
            ResultSet resultSet = preparedStatement.getResultSet();
            while (resultSet.next()) {
                values[resultSet.getInt(1)] = resultSet.getDouble(2);
            }

            return new GameStatisticsData(command, values);
        }
    }

    @Override
    protected void save(GameStatisticsData gameStatisticsData) {
        if (Program.isPublicVersion()) {
            DBMain.getInstance().asyncUpdate("REPLACE INTO GameStatistics (game, won, value) VALUES (?, ?, ?), (?, ?, ?);", preparedStatement -> {
                preparedStatement.setString(1, gameStatisticsData.getCommand());
                preparedStatement.setBoolean(2, false);
                preparedStatement.setDouble(3, gameStatisticsData.getValue(false));
                preparedStatement.setString(4, gameStatisticsData.getCommand());
                preparedStatement.setBoolean(5, true);
                preparedStatement.setDouble(6, gameStatisticsData.getValue(true));
            });
        }
    }

}
