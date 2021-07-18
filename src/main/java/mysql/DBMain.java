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
import mysql.interfaces.SQLFunction;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;

public class DBMain implements DriverAction {

    private static final DBMain ourInstance = new DBMain();

    public static DBMain getInstance() {
        return ourInstance;
    }

    private DBMain() {
    }

    private Connection connect = null;
    private final ExecutorService executorService = Executors.newFixedThreadPool(3, new CountingThreadFactory(() -> "Main", "DB", false));

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
        if (!caches.contains(dbCache)) {
            caches.add(dbCache);
        }
    }

    public void invalidateGuildId(long guildId) {
        caches.forEach(c -> c.invalidateGuildId(guildId));
    }

    public static String instantToDateTimeString(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault()));
    }

    public static String localDateToDateString(LocalDate localDate) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(localDate);
    }

    public Connection getConnection() {
        return connect;
    }

    public <T> T get(String sql, SQLFunction<ResultSet, T> resultSetFunction) throws SQLException, InterruptedException {
        SQLException exception = null;
        for (int i = 0; i < 3; i++) {
            try (Statement statement = getConnection().createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)
            ) {
                return resultSetFunction.apply(resultSet);
            } catch (SQLException e) {
                //ignore
                exception = e;
                Thread.sleep(5000);
            }
        }

        throw exception;
    }

    public <T> T get(String sql, SQLConsumer<PreparedStatement> preparedStatementConsumer, SQLFunction<ResultSet, T> resultSetFunction) throws SQLException, InterruptedException {
        SQLException exception = null;
        for (int i = 0; i < 3; i++) {
            try (PreparedStatement preparedStatement = getConnection().prepareStatement(sql)) {
                preparedStatementConsumer.accept(preparedStatement);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    return resultSetFunction.apply(resultSet);
                }
            } catch (SQLException e) {
                exception = e;
                Thread.sleep(5000);
            }
        }

        throw exception;
    }

    public <T> CompletableFuture<T> asyncGet(String sql, SQLFunction<ResultSet, T> resultSetFunction) {
        CompletableFuture<T> future = new CompletableFuture<>();

        executorService.submit(() -> {
            try {
                T t = get(sql, resultSetFunction);
                future.complete(t);
            } catch (Throwable e) {
                future.completeExceptionally(e);
                MainLogger.get().error("Exception for query: " + sql, e);
            }
        });

        return future;
    }

    public <T> CompletableFuture<T> asyncGet(String sql, SQLConsumer<PreparedStatement> preparedStatementConsumer, SQLFunction<ResultSet, T> resultSetFunction) {
        CompletableFuture<T> future = new CompletableFuture<>();

        executorService.submit(() -> {
            try {
                T t = get(sql, preparedStatementConsumer, resultSetFunction);
                future.complete(t);
            } catch (Throwable e) {
                future.completeExceptionally(e);
                MainLogger.get().error("Exception for query: " + sql, e);
            }
        });

        return future;
    }

    public int update(String sql) throws SQLException, InterruptedException {
        SQLException exception = null;
        for (int i = 0; i < 3; i++) {
            try (Statement statement = getConnection().createStatement()) {
                return statement.executeUpdate(sql);
            } catch (SQLException e) {
                exception = e;
                Thread.sleep(5000);
            }
        }

        throw exception;
    }

    public int update(String sql, SQLConsumer<PreparedStatement> preparedStatementConsumer) throws SQLException, InterruptedException {
        SQLException exception = null;
        for (int i = 0; i < 3; i++) {
            try (PreparedStatement preparedStatement = getConnection().prepareStatement(sql)) {
                preparedStatementConsumer.accept(preparedStatement);
                return preparedStatement.executeUpdate();
            } catch (SQLException e) {
                exception = e;
                Thread.sleep(5000);
            }
        }

        throw exception;
    }

    public CompletableFuture<Integer> asyncUpdate(String sql) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        executorService.submit(() -> {
            try {
                int n = update(sql);
                future.complete(n);
            } catch (Throwable e) {
                future.completeExceptionally(e);
                MainLogger.get().error("Exception for query: " + sql, e);
            }
        });

        return future;
    }

    public CompletableFuture<Integer> asyncUpdate(String sql, SQLConsumer<PreparedStatement> preparedStatementConsumer) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        executorService.submit(() -> {
            try {
                int n = update(sql, preparedStatementConsumer);
                future.complete(n);
            } catch (Throwable e) {
                future.completeExceptionally(e);
                MainLogger.get().error("Exception for query: " + sql, e);
            }
        });

        return future;
    }

    @Override
    public void deregister() {
        MainLogger.get().info("Driver deregistered");
    }

}
