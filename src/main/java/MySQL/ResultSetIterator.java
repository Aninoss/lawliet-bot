package MySQL;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

public class ResultSetIterator implements Iterator<ResultSet> {
    private Statement statement;
    private boolean hasMoreResultSets;
    private int i=0;

    public ResultSetIterator(String sql) throws SQLException {
        statement = DBMain.getInstance().statement();
        hasMoreResultSets = statement.execute( sql );
    }

    public ResultSetIterator(PreparedStatement preparedStatement) throws SQLException {
        statement = preparedStatement;
        hasMoreResultSets = ((PreparedStatement) statement).execute();
    }

    @Override
    public boolean hasNext() {
        try {
            if (i != 0) hasMoreResultSets = statement.getMoreResults();
            if (!hasMoreResultSets && statement.getUpdateCount() == -1) {
                statement.close();
                return false;
            }
            while (!hasMoreResultSets) {
                if (statement.getUpdateCount() == -1) {
                    statement.close();
                    return false;
                }
                hasMoreResultSets = statement.getMoreResults();
            }
            i++;
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public ResultSet next() {
        try {
            ResultSet resultSet = statement.getResultSet();
            return resultSet;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
