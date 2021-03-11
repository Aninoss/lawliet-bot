package mysql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import mysql.interfaces.SQLConsumer;

public class DBBatch {

    private final PreparedStatement preparedStatement;

    public DBBatch(String sql) throws SQLException {
        preparedStatement = DBMain.getInstance().preparedStatement(sql);
    }

    public void add(SQLConsumer<PreparedStatement> preparedStatementConsumer) throws SQLException {
        preparedStatementConsumer.accept(preparedStatement);
        preparedStatement.addBatch();
    }

    public void execute() throws SQLException {
        preparedStatement.executeBatch();
        preparedStatement.close();
    }

}
