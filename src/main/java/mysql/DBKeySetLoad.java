package mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import core.MainLogger;
import mysql.interfaces.SQLFunction;

public class DBKeySetLoad<T> {

    private final Statement statement;

    public DBKeySetLoad(String table, String keyColumn) {
        try {
            statement = DBMain.getInstance().getConnection().createStatement();
            statement.execute(String.format("SELECT %s FROM %s;", keyColumn, table));
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public ArrayList<T> get(SQLFunction<ResultSet, T> function) {
        ArrayList<T> list = new ArrayList<>();
        try (ResultSet resultSet = statement.getResultSet()) {
            while (resultSet.next()) {
                try {
                    list.add(function.apply(resultSet));
                } catch (Throwable e) {
                    MainLogger.get().error("Exception", e);
                }
            }

            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                statement.close();
            } catch (SQLException throwables) {
                MainLogger.get().error("Could not close statement", throwables);
            }
        }
    }

}
