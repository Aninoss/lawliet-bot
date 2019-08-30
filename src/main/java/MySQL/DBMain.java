package MySQL;

import General.SecretManager;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.smattme.MysqlExportService;
import com.vdurmont.emoji.EmojiParser;
import org.javacord.api.DiscordApi;

import javax.swing.plaf.nimbus.State;
import javax.xml.transform.Result;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

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

    public static void backupAll() throws Throwable {
        Properties properties = new Properties();
        properties.setProperty(MysqlExportService.DB_USERNAME, SecretManager.getString("database.username"));
        properties.setProperty(MysqlExportService.DB_PASSWORD, SecretManager.getString("database.password"));
        properties.setProperty(MysqlExportService.DB_NAME, "Lawliet");

        properties.setProperty(MysqlExportService.JDBC_CONNECTION_STRING, "jdbc:mysql://" + SecretManager.getString("database.ip") + ":3306/Lawliet?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC");

        MysqlExportService mysqlExportService = new MysqlExportService(properties);
        String sqlString = mysqlExportService.export();

        String fileName = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss").format(new Date());

        FileWriter fw = new FileWriter("data/database_backups/" + fileName + ".sql", false);
        BufferedWriter br = new BufferedWriter(fw);

        br.write(sqlString);
        br.flush();

        br.close();
        fw.close();
    }
}
