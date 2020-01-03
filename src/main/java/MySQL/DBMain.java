package MySQL;

import General.Bot;
import General.DiscordApiCollection;
import General.SecretManager;
import com.mysql.cj.jdbc.MysqlDataSource;
import com.vdurmont.emoji.EmojiParser;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

public class DBMain implements DriverAction {
    private static DBMain ourInstance = new DBMain();

    private Connection connect = null;

    public static DBMain getInstance() {
        return ourInstance;
    }

    private DBMain() {}

    public void connect() throws IOException, SQLException {
        System.out.println("Connecting with database...");

        final MysqlDataSource rv = new MysqlDataSource();
        rv.setServerName(Bot.isDebug() ? SecretManager.getString("database.ip") : "127.0.0.1");
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

    public static void synchronizeAll() throws InterruptedException, ExecutionException, SQLException {
        DBServer.synchronize();
        DBServer.synchronizeAutoChannelChildChannels();
        DBUser.synchronize();
        DBBot.synchronize();
    }

    public static String instantToDateTimeString(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.ofInstant(instant,ZoneOffset.systemDefault()));
    }

    public static String instantToDateString(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.ofInstant(instant,ZoneOffset.systemDefault()));
    }

    public PreparedStatement preparedStatement(String sql) throws SQLException {
        return connect.prepareStatement(sql);
    }

    public Statement statement(String sql) throws SQLException {
        Statement statement = connect.createStatement();
        statement.execute(sql);
        return statement;
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

    public static String encryptEmojis(String str) {
        return EmojiParser.parseToAliases(str);
    }

    @Override
    public void deregister() {
        System.out.println("Driver deregistered");
    }
}
