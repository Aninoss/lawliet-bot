package mysql;

import com.mysql.cj.jdbc.MysqlDataSource;
import mysql.interfaces.SQLConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DBMain implements DriverAction {

    private static final DBMain ourInstance = new DBMain();
    public static DBMain getInstance() { return ourInstance; }
    private DBMain() {}

    private final static Logger LOGGER = LoggerFactory.getLogger(DBMain.class);
    private Connection connect = null;
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    private final ArrayList<DBCached> caches = new ArrayList<>();

    public void connect() throws SQLException {
        final MysqlDataSource rv = new MysqlDataSource();
        rv.setServerName(System.getenv("DB_HOST"));
        rv.setPortNumber(Integer.parseInt(System.getenv("DB_PORT")));
        rv.setDatabaseName(System.getenv("DB_DATABASE"));
        rv.setAllowMultiQueries(false);
        rv.setAutoReconnect(true);
        rv.setCharacterEncoding("UTF-8");
        rv.setUser(System.getenv("DB_USER"));
        rv.setPassword(System.getenv("DB_PASSWORD"));
        rv.setServerTimezone(TimeZone.getDefault().getID());
        rv.setRewriteBatchedStatements(true);

        LOGGER.info("Connecting with database {}", rv.getUrl());
        connect = rv.getConnection();
    }

    public void addDBCached(DBCached dbCached) {
        if (!caches.contains(dbCached)) caches.add(dbCached);
    }

    public void clearCache() {
        caches.forEach(DBCached::autoClear);
    }

    public static String instantToDateTimeString(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.ofInstant(instant,ZoneOffset.systemDefault()));
    }

    public static String localDateToDateString(LocalDate localDate) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(localDate);
    }

    public PreparedStatement preparedStatement(String sql) throws SQLException {
        return connect.prepareStatement(sql);
    }

    public Statement statementExecuted(String sql) throws SQLException {
        Statement statement = connect.createStatement();
        statement.execute(sql);
        return statement;
    }

    public int update(String sql, SQLConsumer<PreparedStatement> preparedStatementConsumer) throws SQLException, InterruptedException {
        SQLException exception = null;
        for(int i = 0; i < 3; i++) {
            try {
                PreparedStatement preparedStatement = preparedStatement(sql);
                preparedStatementConsumer.accept(preparedStatement);
                int n = preparedStatement.executeUpdate();
                preparedStatement.close();

                return n;
            } catch (SQLException e) {
                //Ignore
                exception = e;
                Thread.sleep(5000);
            }
        }

        throw exception;
    }

    public CompletableFuture<Integer> asyncUpdate(String sql, SQLConsumer<PreparedStatement> preparedStatementConsumer) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        executorService.submit(() -> {
            try {
                future.complete(update(sql, preparedStatementConsumer));
            } catch (SQLException | InterruptedException throwables) {
                future.completeExceptionally(throwables);
                LOGGER.error("Exception for query: " + sql, throwables);
            }
        });

        return future;
    }

    public boolean checkConnection() {
        boolean success = false;

        try {
            Statement statement = statementExecuted("SELECT 1;");
            ResultSet resultSet = statement.getResultSet();
            if (resultSet.next() && resultSet.getInt(1) == 1) success = true;
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            LOGGER.error("Database error", e);
        }

        return success;
    }

    @Override
    public void deregister() {
        LOGGER.info("Driver deregistered");
    }

}
