package mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import mysql.interfaces.SQLConsumer;

public class DBBatch implements AutoCloseable {

    private final PreparedStatement preparedStatement;

    public DBBatch(String sql) throws SQLException {
        Connection connection = DBMain.getInstance().getConnection();
        preparedStatement = connection.prepareStatement(sql);
    }

    public void add(SQLConsumer<PreparedStatement> preparedStatementConsumer) throws SQLException {
        preparedStatementConsumer.accept(preparedStatement);
        preparedStatement.addBatch();
    }

    public void execute() throws SQLException {
        preparedStatement.executeBatch();
    }

    @Override
    public void close() throws SQLException {
        preparedStatement.close();
    }

}
