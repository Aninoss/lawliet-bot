package MySQL;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

public class ResultSetIterator implements Iterator<ResultSet> {

    private Statement statement;
    private boolean hasMoreResultSets = false;
    private int i=0;

    public ResultSetIterator(String sql) {
        try {
            statement = DBMain.getInstance().statement();
            hasMoreResultSets = statement.execute( sql );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSetIterator(PreparedStatement preparedStatement) {
        statement = preparedStatement;
        try {
            hasMoreResultSets = ((PreparedStatement) statement).execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
