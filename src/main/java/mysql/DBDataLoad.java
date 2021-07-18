package mysql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import core.MainLogger;
import mysql.interfaces.SQLConsumer;
import mysql.interfaces.SQLFunction;

public class DBDataLoad<T> {

    private final PreparedStatement preparedStatement;

    public DBDataLoad(String table, String requiredAttributes, String where) {
        this(table, requiredAttributes, where, ps -> {
        });
    }

    public DBDataLoad(String table, String requiredAttributes, String where, SQLConsumer<PreparedStatement> wherePreparedStatementConsumer) {
        try {
            if (requiredAttributes.isEmpty()) throw new SQLException("No attributes specified!");

            String sqlString = String.format("SELECT %s FROM %s WHERE %s", requiredAttributes, table, where);

            preparedStatement = DBMain.getInstance().getConnection().prepareStatement(sqlString);
            wherePreparedStatementConsumer.accept(preparedStatement);
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<T> getArrayList(SQLFunction<ResultSet, T> function) {
        try (ResultSet resultSet = preparedStatement.getResultSet()) {
                ArrayList<T> list = new ArrayList<>();

                while (resultSet.next()) {
                    try {
                        T value = function.apply(resultSet);
                        if (!Objects.isNull(value)) list.add(value);
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

    public <U> HashMap<U, T> getHashMap(SQLFunction<T, U> getKeyFunction, SQLFunction<ResultSet, T> function) {
        try (ResultSet resultSet = preparedStatement.getResultSet()) {
            HashMap<U, T> map = new HashMap<>();

            while (resultSet.next()) {
                try {
                    T value = function.apply(resultSet);
                    if (value != null) map.put(getKeyFunction.apply(value), value);
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
