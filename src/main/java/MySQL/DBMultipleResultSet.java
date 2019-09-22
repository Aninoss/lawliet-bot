package MySQL;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

public class DBMultipleResultSet implements Iterable<ResultSet> {
    private String sql;
    private PreparedStatement preparedStatement;

    public DBMultipleResultSet(String sql) {
        this.sql = sql;
    }

    public DBMultipleResultSet(PreparedStatement preparedStatement) {
        this.preparedStatement = preparedStatement;
    }

    public Iterator<ResultSet> iterator() {
        try {
            if (preparedStatement != null) return new ResultSetIterator(preparedStatement);
            else return new ResultSetIterator(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
