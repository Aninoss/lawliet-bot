package MySQL;

import MySQL.Interfaces.SQLConsumer;
import MySQL.Interfaces.SQLFunction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public class DBDataLoad<T> {

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
            T value = function.apply(resultSet);
            if (value != null) list.add(function.apply(resultSet));
        }

        resultSet.close();
        preparedStatement.close();

        return list;
    }

    public <U> HashMap<U, T> getHashMap(SQLFunction<T, U> getKeyFuntion, SQLFunction<ResultSet, T> function) throws SQLException {
        ResultSet resultSet = preparedStatement.getResultSet();
        HashMap<U, T> map = new HashMap<>();

        while (resultSet.next()) {
            T value = function.apply(resultSet);
            if (value != null) map.put(getKeyFuntion.apply(value), value);
        }

        resultSet.close();
        preparedStatement.close();

        return map;
    }

}
