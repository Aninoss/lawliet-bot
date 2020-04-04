package MySQL;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

public class DBDataLoad<T> {

   private PreparedStatement preparedStatement;

    public DBDataLoad(String table, String requiredAttribute, String where, SQLConsumer<PreparedStatement> wherePreparedStatementConsumer) throws SQLException {
        this(table, requiredAttribute.split(","), where, wherePreparedStatementConsumer);
    }

    public DBDataLoad(String table, String[] requiredAttributes, String where, SQLConsumer<PreparedStatement> wherePreparedStatementConsumer) throws SQLException {
        if (requiredAttributes.length == 0) throw new SQLException("No attributes specified!");

        StringBuilder attrString = new StringBuilder();
        for(int i = 0; i < requiredAttributes.length; i++) {
            if (i > 0) attrString.append(",");
            attrString.append(requiredAttributes[i]);
        }

        String sqlString = String.format("SELECT %s FROM %s WHERE %s", attrString.toString(), table, where);

        preparedStatement = DBMain.getInstance().preparedStatement(sqlString);
        wherePreparedStatementConsumer.accept(preparedStatement);
        preparedStatement.execute();
    }

    public ArrayList<T> getArrayList(SQLFunction<ResultSet, T> function) throws SQLException {
        ResultSet resultSet = preparedStatement.getResultSet();
        ArrayList<T> list = new ArrayList<>();

        while (resultSet.next()) list.add(function.apply(resultSet));

        resultSet.close();
        preparedStatement.close();

        return list;
    }

    public <U> HashMap<U, T> getHashMap(Function<T, U> getKeyFuntion, SQLFunction<ResultSet, T> function) throws SQLException {
        ResultSet resultSet = preparedStatement.getResultSet();
        HashMap<U, T> map = new HashMap<>();

        while (resultSet.next()) {
            T value = function.apply(resultSet);
            map.put(getKeyFuntion.apply(value), value);
        }

        resultSet.close();
        preparedStatement.close();

        return map;
    }

    public interface SQLConsumer<T> {
        void accept(T t) throws SQLException;
    }

    public interface SQLFunction<T, R> {
        R apply(T t) throws SQLException;
    }

}
