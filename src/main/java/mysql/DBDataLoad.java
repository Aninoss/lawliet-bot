package mysql;

import mysql.interfaces.SQLConsumer;
import mysql.interfaces.SQLFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class DBDataLoad<T> {

    private final static Logger LOGGER = LoggerFactory.getLogger(DBDataLoad.class);

    private final PreparedStatement preparedStatement;

    public DBDataLoad(String table, String requiredAttributes, String where, SQLConsumer<PreparedStatement> wherePreparedStatementConsumer) throws SQLException {
        if (requiredAttributes.isEmpty()) throw new SQLException("No attributes specified!");

        String sqlString = String.format("SELECT %s FROM %s WHERE %s", requiredAttributes, table, where);

        preparedStatement = DBMain.getInstance().preparedStatement(sqlString);
        wherePreparedStatementConsumer.accept(preparedStatement);
        preparedStatement.execute();
    }

    public ArrayList<T> getArrayList(SQLFunction<ResultSet, T> function) throws SQLException {
        ResultSet resultSet = preparedStatement.getResultSet();
        ArrayList<T> list = new ArrayList<>();

        while (resultSet.next()) {
            try {
                T value = function.apply(resultSet);
                if (value != null) list.add(function.apply(resultSet));
            } catch (Throwable e) {
                LOGGER.error("Exception", e);
            }
        }

        resultSet.close();
        preparedStatement.close();

        return list;
    }

    public <U> HashMap<U, T> getHashMap(SQLFunction<T, U> getKeyFuntion, SQLFunction<ResultSet, T> function) throws SQLException {
        ResultSet resultSet = preparedStatement.getResultSet();
        HashMap<U, T> map = new HashMap<>();

        while (resultSet.next()) {
            try {
                T value = function.apply(resultSet);
                if (value != null) map.put(getKeyFuntion.apply(value), value);
            } catch (Throwable e) {
                LOGGER.error("Exception", e);
            }
        }

        resultSet.close();
        preparedStatement.close();

        return map;
    }

}
