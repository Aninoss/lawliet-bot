package mysql;

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
import com.mysql.cj.jdbc.MysqlDataSource;
import core.MainLogger;
import mysql.interfaces.SQLConsumer;

public class DBMain implements DriverAction {

    private static final DBMain ourInstance = new DBMain();

    public static DBMain getInstance() {
        return ourInstance;
    }

    private DBMain() {
    }

    private Connection connect = null;
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    private final ArrayList<DBCache> caches = new ArrayList<>();

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

        MainLogger.get().info("Connecting with database {}", rv.getUrl());
        connect = rv.getConnection();
    }

    public void addDBCached(DBCache dbCache) {
        if (!caches.contains(dbCache)) caches.add(dbCache);
    }

    public static String instantToDateTimeString(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault()));
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
        for (int i = 0; i < 3; i++) {
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

    public CompletableFuture<Integer> asyncUpdate(String sql) {
        return asyncUpdate(sql, preparedStatement -> {
        });
    }

    public CompletableFuture<Integer> asyncUpdate(String sql, SQLConsumer<PreparedStatement> preparedStatementConsumer) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        executorService.submit(() -> {
            try {
                future.complete(update(sql, preparedStatementConsumer));
            } catch (SQLException | InterruptedException throwables) {
                future.completeExceptionally(throwables);
                MainLogger.get().error("Exception for query: " + sql, throwables);
            }
        });

        return future;
    }

    @Override
    public void deregister() {
        MainLogger.get().info("Driver deregistered");
    }

}
