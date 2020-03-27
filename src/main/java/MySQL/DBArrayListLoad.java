package MySQL;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DBArrayListLoad<T> {

   private PreparedStatement preparedStatement;

    public DBArrayListLoad(String mySQLQuery, SQLConsumer<PreparedStatement> preparedStatementConsumer) throws SQLException {
        preparedStatement = DBMain.getInstance().preparedStatement(mySQLQuery);
        preparedStatementConsumer.accept(preparedStatement);
        preparedStatement.execute();
    }

    public DBArrayListLoad(String table, String requiredAttribute, String where, SQLConsumer<PreparedStatement> wherePreparedStatementConsumer) throws SQLException {
        this(table, new String[]{requiredAttribute}, where, wherePreparedStatementConsumer);
    }

    public DBArrayListLoad(String table, String[] requiredAttributes, String where, SQLConsumer<PreparedStatement> wherePreparedStatementConsumer) throws SQLException {
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

    public interface SQLConsumer<T> {
        void accept(T t) throws SQLException;
    }

    public interface SQLFunction<T, R> {
        R apply(T t) throws SQLException;
    }

}
