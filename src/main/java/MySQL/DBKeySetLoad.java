package MySQL;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DBKeySetLoad<T> {

   private Statement statement;

    public DBKeySetLoad(String table, String keyColumn) throws SQLException {
        statement = DBMain.getInstance().statement(String.format("SELECT %s FROM %s;", keyColumn, table));
    }

    public ArrayList<T> get(DBArrayListLoad.SQLFunction<ResultSet, T> function) throws SQLException {
        ArrayList<T> list = new ArrayList<>();

        ResultSet resultSet = statement.getResultSet();
        while (resultSet.next()) list.add(function.apply(resultSet));

        resultSet.close();
        statement.close();

        return list;
    }

}
