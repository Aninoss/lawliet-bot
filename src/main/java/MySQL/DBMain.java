package MySQL;

import General.SecretManager;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.vdurmont.emoji.EmojiParser;
import org.javacord.api.DiscordApi;

import javax.swing.plaf.nimbus.State;
import javax.xml.transform.Result;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DBMain {
    private static DBMain ourInstance = new DBMain();

    private Connection connect = null;

    public static DBMain getInstance() {
        return ourInstance;
    }

    private DBMain() {}

    public boolean connect() {
        try {
            System.out.println("Connecting with database...");
            Class.forName("com.mysql.jdbc.Driver");

            final MysqlDataSource rv = new MysqlDataSource();
            rv.setServerName(SecretManager.getString("database.ip"));
            rv.setPortNumber(3306);
            rv.setDatabaseName("Lawliet");
            rv.setUseUnicode(true);
            rv.setAllowMultiQueries(true);
            rv.setAutoReconnect(true);
            rv.setCharacterEncoding("UTF-8");
            rv.setUser(SecretManager.getString("database.username"));
            rv.setPassword(SecretManager.getString("database.password"));
            rv.setEncoding("UTF-8");
            connect = rv.getConnection();

            return true;
        } catch (Throwable e){
            e.printStackTrace();
            return false;
        }
    }

    public static void synchronizeAll(DiscordApi api) throws Throwable {
        DBServer.synchronize(api);
        DBUser.synchronize(api);
        DBBot.synchronize(api);
    }

    public static String instantToDateTimeString(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.ofInstant(instant,ZoneOffset.systemDefault()));
    }

    public static String instantToDateString(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.ofInstant(instant,ZoneOffset.systemDefault()));
    }

    public ResultSet getLastKey() throws Throwable {
        Statement statement = connect.createStatement();
        return statement.executeQuery("SELECT LAST_INSERT_ID();");
    }

    public PreparedStatement preparedStatement(String sql) throws Throwable {
        return connect.prepareStatement(sql);
    }

    public Statement statement(String sql) throws Throwable {
        Statement statement = connect.createStatement();
        statement.execute(sql);
        return statement;
    }

    public Statement statement() throws Throwable {
        return connect.createStatement();
    }

    public static String encryptEmojis(String str) {
        return EmojiParser.parseToAliases(str);
    }
}
