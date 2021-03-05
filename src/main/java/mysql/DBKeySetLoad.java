package mysql;

import core.MainLogger;
import mysql.interfaces.SQLFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DBKeySetLoad<T> {

   private final Statement statement;

    public DBKeySetLoad(String table, String keyColumn) {
        try {
            statement = DBMain.getInstance().statementExecuted(String.format("SELECT %s FROM %s;", keyColumn, table));
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public ArrayList<T> get(SQLFunction<ResultSet, T> function) {
        try {
            ArrayList<T> list = new ArrayList<>();

            ResultSet resultSet = statement.getResultSet();
            while (resultSet.next()) {
                try {
                    list.add(function.apply(resultSet));
                } catch (Throwable e) {
                    MainLogger.get().error("Exception", e);
                }
            }

            resultSet.close();
            statement.close();

            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
