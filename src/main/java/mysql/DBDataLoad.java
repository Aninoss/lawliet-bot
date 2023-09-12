package mysql;

import core.MainLogger;
import mysql.interfaces.SQLConsumer;
import mysql.interfaces.SQLFunction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBDataLoad<T> {

    private PreparedStatement preparedStatement;

    public DBDataLoad() {
    }

    public DBDataLoad(String table, String requiredAttributes, String where) {
        init(table, requiredAttributes, where, ps -> {
        });
    }

    public DBDataLoad(String table, String requiredAttributes, String where, SQLConsumer<PreparedStatement> wherePreparedStatementConsumer) {
        init(table, requiredAttributes, where, wherePreparedStatementConsumer);
    }

    public void init(String table, String requiredAttributes, String where, SQLConsumer<PreparedStatement> wherePreparedStatementConsumer) {
        try {
            if (requiredAttributes.isEmpty()) {
                throw new SQLException("No attributes specified!");
            }

            String sqlString = String.format("SELECT %s FROM %s WHERE %s", requiredAttributes, table, where);

            Connection connection = MySQLManager.getConnection();
            preparedStatement = connection.prepareStatement(sqlString);
            wherePreparedStatementConsumer.accept(preparedStatement);
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<T> getList(SQLFunction<ResultSet, T> function) {
        try (ResultSet resultSet = preparedStatement.getResultSet()) {
            ArrayList<T> list = new ArrayList<>();

            while (resultSet.next()) {
                try {
                    T value = function.apply(resultSet);
                    if (value != null) {
                        list.add(value);
                    }
                } catch (Throwable e) {
                    MainLogger.get().error("Exception", e);
                }
            }

            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                preparedStatement.close();
            } catch (SQLException throwables) {
                MainLogger.get().error("Could not close preparedStatement", throwables);
            }
        }
    }

    public <U> Map<U, T> getMap(SQLFunction<T, U> getKeyFunction, SQLFunction<ResultSet, T> function) {
        try (ResultSet resultSet = preparedStatement.getResultSet()) {
            HashMap<U, T> map = new HashMap<>();

            while (resultSet.next()) {
                try {
                    T value = function.apply(resultSet);
                    if (value != null) {
                        map.put(getKeyFunction.apply(value), value);
                    }
                } catch (Throwable e) {
                    MainLogger.get().error("Exception", e);
                }
            }

            return map;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                preparedStatement.close();
            } catch (SQLException throwables) {
                MainLogger.get().error("Could not close preparedStatement", throwables);
            }
        }
    }

}
