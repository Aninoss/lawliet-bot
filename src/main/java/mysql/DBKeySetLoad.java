package mysql;

import mysql.interfaces.SQLFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DBKeySetLoad<T> {

    private final static Logger LOGGER = LoggerFactory.getLogger(DBKeySetLoad.class);

   private final Statement statement;

    public DBKeySetLoad(String table, String keyColumn) throws SQLException {
        statement = DBMain.getInstance().statementExecuted(String.format("SELECT %s FROM %s;", keyColumn, table));
    }

    public ArrayList<T> get(SQLFunction<ResultSet, T> function) throws SQLException {
        ArrayList<T> list = new ArrayList<>();

        ResultSet resultSet = statement.getResultSet();
        while (resultSet.next()) {
            try {
                list.add(function.apply(resultSet));
            } catch (Throwable e) {
                LOGGER.error("Exception", e);
            }
        }

        resultSet.close();
        statement.close();

        return list;
    }

}
