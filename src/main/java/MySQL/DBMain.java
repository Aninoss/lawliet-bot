package MySQL;

import General.Bot;
import General.SecretManager;
import com.mysql.cj.jdbc.MysqlDataSource;
import com.smattme.MysqlExportService;
import com.vdurmont.emoji.EmojiParser;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.Server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

public class DBMain implements DriverAction {
    private static DBMain ourInstance = new DBMain();

    private Connection connect = null;

    public static DBMain getInstance() {
        return ourInstance;
    }

    private DBMain() {}

    public boolean connect() {
        try {
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

            return true;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void synchronizeAll(DiscordApi api) throws InterruptedException, ExecutionException, SQLException {
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

    public static void backupAll() throws IOException, SQLException, ClassNotFoundException {
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

    @Override
    public void deregister() {
        System.out.println("Driver deregistered");
    }
}
