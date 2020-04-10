package MySQL;

import Core.Bot;
import Core.ExceptionHandler;
import Core.SecretManager;
import MySQL.Interfaces.SQLConsumer;
import MySQL.Modules.AutoChannel.DBAutoChannel;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.javacord.api.DiscordApi;

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
    public static DBMain getInstance() {
        return ourInstance;
    }
    private DBMain() {}

    private Connection connect = null;

    private ArrayList<DBCached> caches = new ArrayList<>();

    public void connect() throws IOException, SQLException {
        System.out.println("Connecting with database...");

        final MysqlDataSource rv = new MysqlDataSource();
        rv.setServerName(Bot.isProductionMode() ? "127.0.0.1" : SecretManager.getString("database.ip"));
        rv.setPortNumber(3306);
        rv.setDatabaseName(SecretManager.getString("database.database"));
        rv.setAllowMultiQueries(true);
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

    public void asyncLoad(String sql, SQLConsumer<PreparedStatement> preparedStatementConsumer, Consumer<ResultSet> resultSetConsumer) {
        Thread t = new Thread(() -> {
            SQLException exception = null;
            for(int i = 0; i < 3; i++) {
                try {
                    PreparedStatement preparedStatement = preparedStatement(sql);
                    preparedStatementConsumer.accept(preparedStatement);
                    preparedStatement.execute();

                    ResultSet resultSet = preparedStatement.getResultSet();
                    while (resultSet.next()) resultSetConsumer.accept(resultSet);

                    resultSet.close();
                    preparedStatement.close();
                    return;
                } catch (SQLException e) {
                    //Ignore
                    exception = e;
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        //Ignore
                    }
                }
            }

            exception.printStackTrace();
        });
        t.setPriority(1);
        t.setName("sql_load");
        t.start();
    }

    public CompletableFuture<Integer> asyncUpdate(String sql, SQLConsumer<PreparedStatement> preparedStatementConsumer) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        Thread t = new Thread(() -> {
            SQLException exception = null;
            for(int i = 0; i < 3; i++) {
                try {
                    PreparedStatement preparedStatement = preparedStatement(sql);
                    preparedStatementConsumer.accept(preparedStatement);
                    int n = preparedStatement.executeUpdate();
                    preparedStatement.close();

                    future.complete(n);
                    return;
                } catch (SQLException e) {
                    //Ignore
                    exception = e;
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        //Ignore
                    }
                }
            }

            exception.printStackTrace();
            future.completeExceptionally(exception);
        });
        t.setPriority(1);
        t.setName("sql_update");
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
            e.printStackTrace();
        }

        return success;
    }

    @Override
    public void deregister() {
        System.out.println("Driver deregistered");
    }

}
