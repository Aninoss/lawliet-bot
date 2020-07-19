package MySQL;

import Core.Bot;
import Core.CustomThread;
import Core.ExceptionHandler;
import Core.SecretManager;
import MySQL.Interfaces.SQLConsumer;
import MySQL.Modules.AutoChannel.DBAutoChannel;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.javacord.api.DiscordApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DBMain implements DriverAction {

    private static DBMain ourInstance = new DBMain();
    public static DBMain getInstance() { return ourInstance; }
    private DBMain() {}

    private final static Logger LOGGER = LoggerFactory.getLogger(DBMain.class);
    private Connection connect = null;

    private ArrayList<DBCached> caches = new ArrayList<>();

    public void connect() throws IOException, SQLException {
        LOGGER.info("Connecting with database");

        final MysqlDataSource rv = new MysqlDataSource();
        rv.setServerName(Bot.isProductionMode() ? "127.0.0.1" : SecretManager.getString("database.ip"));
        rv.setPortNumber(3306);
        rv.setDatabaseName(SecretManager.getString("database.database"));
        rv.setAllowMultiQueries(false);
        rv.setAutoReconnect(true);
        rv.setCharacterEncoding("UTF-8");
        rv.setUser(SecretManager.getString("database.username"));
        rv.setPassword(SecretManager.getString("database.password"));
        rv.setServerTimezone(TimeZone.getDefault().getID());
        connect = rv.getConnection();
    }

    public void addDBCached(DBCached dbCached) {
        if (!caches.contains(dbCached)) caches.add(dbCached);
    }

    public void clearCache() { caches.forEach(DBCached::clear); }

    public static String instantToDateTimeString(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.ofInstant(instant,ZoneOffset.systemDefault()));
    }

    public static String localDateToDateString(LocalDate localDate) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(localDate);
    }

    public PreparedStatement preparedStatement(String sql) throws SQLException {
        return connect.prepareStatement(sql);
    }

    public Statement statement(String sql) throws SQLException {
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

        Thread t = new CustomThread(() -> {
            try {
                future.complete(update(sql, preparedStatementConsumer));
            } catch (SQLException | InterruptedException throwables) {
                future.completeExceptionally(throwables);
                LOGGER.error("Exception", throwables);
            }
        }, "sql_update", 1);
        t.start();

        return future;
    }

    public Statement statement() throws SQLException {
        return connect.createStatement();
    }

    public boolean checkConnection() {
        boolean success = false;

        try {
            Statement statement = statement("SELECT 1;");
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
