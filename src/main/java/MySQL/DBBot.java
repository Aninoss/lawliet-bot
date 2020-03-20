package MySQL;

import General.Bot;
import General.DiscordApiCollection;
import General.Fishing.FishingRecords;
import General.RankingSlot;
import General.Tools;
import General.Tracker.TrackerData;
import General.Tracker.TrackerManager;
import ServerStuff.DiscordbotsAPI;
import org.javacord.api.DiscordApi;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

public class DBBot {

    public static String getCurrentVersions() throws SQLException {
        String result = null;
        Statement statement = DBMain.getInstance().statement("SELECT version FROM Version ORDER BY date DESC LIMIT 1;");
        ResultSet resultSet = statement.getResultSet();
        if (resultSet.next()) result = resultSet.getString(1);
        resultSet.close();
        statement.close();
        return result;
    }

    public static ArrayList<String> getCurrentVersions(long i) throws SQLException {
        ArrayList<String> versions = new ArrayList<>();
        Statement statement = DBMain.getInstance().statement("SELECT version FROM Version ORDER BY date DESC LIMIT " + i + ";");
        ResultSet resultSet = statement.getResultSet();
        while (resultSet.next()) versions.add(resultSet.getString(1));
        resultSet.close();
        statement.close();
        return versions;
    }

    public static ArrayList<String> getCurrentVersions(String... versions) throws SQLException {
        ArrayList<String> acceptedVersions = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        for(int i=0; i < Math.min(10, versions.length); i++) {
            if (Tools.stringIsDouble(versions[i].replace(".", ""))) {
                if (i == 0) sb.append("WHERE version = '").append(versions[i]).append("'");
                else sb.append(" OR version = '").append(versions[i]).append("'");
            }
        }

        Statement statement = DBMain.getInstance().statement("SELECT version FROM Version " + sb.toString() + " ORDER BY date DESC;");
        ResultSet resultSet = statement.getResultSet();
        if (resultSet.next()) acceptedVersions.add(resultSet.getString(1));
        resultSet.close();
        statement.close();
        return acceptedVersions;
    }

    public static Instant getCurrentVersionDate() throws SQLException {
        Instant result = null;
        Statement statement = DBMain.getInstance().statement("SELECT date FROM Version ORDER BY date DESC LIMIT 1;");
        ResultSet resultSet = statement.getResultSet();
        if (resultSet.next()) result = resultSet.getTimestamp(1).toInstant();
        resultSet.close();
        statement.close();
        return result;
    }

    public static void insertVersion(String idVersion, Instant date) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("INSERT INTO Version VALUES (?, ?);");
        preparedStatement.setString(1, idVersion);
        preparedStatement.setString(2, DBMain.instantToDateTimeString(date));
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static double getGameWonMultiplicator(String game, boolean won, double valueAdd) throws SQLException {
        String str = "INSERT IGNORE INTO GameStatistics (game, won) VALUES(?, 0);" +
                "INSERT IGNORE INTO GameStatistics (game, won) VALUES(?, 1);" +
                "UPDATE GameStatistics set value = value + ? WHERE game = ? AND won = ?;" +
                "SELECT IFNULL(lost.value / won.value, 1) FROM GameStatistics lost, GameStatistics won WHERE lost.game = ? AND lost.won = 0 AND lost.game = won.game AND won.won = 1;";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(str);
        preparedStatement.setString(1, game);
        preparedStatement.setString(2, game);
        preparedStatement.setDouble(3, valueAdd);
        preparedStatement.setString(4, game);
        preparedStatement.setBoolean(5, won);
        preparedStatement.setString(6, game);

        for(ResultSet resultSet: new DBMultipleResultSet(preparedStatement)) {
            resultSet.next();
            return resultSet.getDouble(1);
        }

        return 1;
    }

    public static void fisheryCleanUp() throws SQLException {
        String sql = "DELETE FROM PowerPlantUserGained WHERE TIMESTAMPDIFF(HOUR, time, NOW()) > 168;";
        DBMain.getInstance().statement(sql);
    }

    public static void startTrackers(DiscordApi api) throws SQLException {
        if (!Bot.isDebug()) {
            List<TrackerData> trackerDataList = getTracker(api);
            for (TrackerData trackerData : trackerDataList) {
                TrackerManager.startTracker(trackerData);
            }
        }
    }

    public static ArrayList<TrackerData> getTracker(DiscordApi api) throws SQLException {
        ArrayList<TrackerData> dataArrayList = new ArrayList<>();

        Statement statement = DBMain.getInstance().statement("SELECT * FROM Tracking;");
        ResultSet resultSet = statement.getResultSet();
        while(resultSet.next()) {
            long serverId = resultSet.getLong(1);
            long channelId = resultSet.getLong(2);
            String command = resultSet.getString(3);
            long messageId = resultSet.getLong(4);
            String key = resultSet.getString(5);
            Instant instant = resultSet.getTimestamp(6).toInstant();
            String arg = resultSet.getString(7);

            Optional<Server> serverOptional = api.getServerById(serverId);
            if (serverOptional.isPresent()) {
                Server server = serverOptional.get();
                Optional<ServerTextChannel> channelOptional = server.getTextChannelById(channelId);
                if (channelOptional.isPresent()) {
                    ServerTextChannel channel = channelOptional.get();
                    dataArrayList.add(new TrackerData(server, channel, messageId, command, key, instant, arg));
                } else removeTracker(serverId, channelId, command);
            }
        }
        return dataArrayList;
    }

    public static void saveTracker(TrackerData trackerData) throws SQLException {
        if (Bot.isDebug()) return;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("REPLACE INTO Tracking VALUES (?, ?, ?, ?, ?, ?, ?);");
        preparedStatement.setLong(1, trackerData.getServerId());
        preparedStatement.setLong(2, trackerData.getChannelId());
        preparedStatement.setString(3, trackerData.getCommand());
        preparedStatement.setLong(4, trackerData.getMessageId());
        preparedStatement.setString(5, trackerData.getKey());
        preparedStatement.setString(6, DBMain.instantToDateTimeString(trackerData.getInstant()));
        preparedStatement.setString(7, trackerData.getArg());
        preparedStatement.executeUpdate();
    }

    public static void removeTracker(TrackerData trackerData) throws SQLException {
        removeTracker(trackerData.getServerId(), trackerData.getChannelId(), trackerData.getCommand());
    }

    public static void removeTracker(long serverId, long channelId, String command) throws SQLException {
        if (Bot.isDebug()) return;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM Tracking WHERE serverId = ? AND channelId = ? AND command = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.setLong(2, channelId);
        preparedStatement.setString(3, command);
        preparedStatement.executeUpdate();
    }

    public static FishingRecords getFishingRecords() throws SQLException, ExecutionException, InterruptedException {
        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();

        String sql = "SELECT serverId, userId, growth FROM PowerPlantUsersExtended ORDER BY growth DESC LIMIT 5;\n" +
                "SELECT serverId, userId, joule FROM PowerPlantUsersExtended ORDER BY joule DESC LIMIT 5;\n" +
                "SELECT serverId, userId, coins FROM PowerPlantUsersExtended ORDER BY coins DESC LIMIT 5;\n" +
                "SELECT serverId, userId, dailyStreak FROM PowerPlantUsersExtended ORDER BY dailyStreak DESC LIMIT 5;";

        Server[] servers = new Server[4];
        User[] users = new User[4];
        long[] values = new long[4];

        int i=0;
        for(ResultSet resultSet: new DBMultipleResultSet(sql)) {
            while (resultSet.next()) {
                if (apiCollection.getServerById(resultSet.getLong(1)).isPresent() && apiCollection.getServerById(resultSet.getLong(1)).get().getMemberById(resultSet.getLong(2)).isPresent()) {
                    servers[i] = apiCollection.getServerById(resultSet.getLong(1)).get();
                    users[i] = apiCollection.getUserById(resultSet.getLong(2)).get();
                    values[i] = resultSet.getLong(3);
                    break;
                }
            }
            i++;
        }

        return new FishingRecords(servers, users, values);
    }

    public static void addCommandUsage(String trigger) throws SQLException {
        String sql = "INSERT INTO CommandUsages VALUES(?, 1) ON DUPLICATE KEY UPDATE usages = usages + 1;";
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setString(1, trigger);
        preparedStatement.execute();
        preparedStatement.close();
    }

    public static int getTotalCommandUsages() throws SQLException {
        int count = 0;

        Statement statement = DBMain.getInstance().statement("SELECT SUM(usages) FROM CommandUsages;");
        ResultSet resultSet = statement.getResultSet();

        if (resultSet.next()) count = resultSet.getInt(1);

        resultSet.close();
        statement.close();

        return count;
    }

    public static void addStatServers(int serverCount) throws SQLException {
        String sql = "INSERT INTO StatsServerCount VALUES(NOW(), ?);";
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setInt(1, serverCount);
        preparedStatement.execute();
        preparedStatement.close();
    }

    public static void addStatCommandUsages() throws SQLException {
        String sql = "INSERT INTO StatsCommandUsages VALUES(NOW(), (SELECT SUM(usages) FROM CommandUsages));";
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.execute();
        preparedStatement.close();
    }

    public static void addStatUpvotes() throws SQLException {
        String sql = "INSERT INTO StatsUpvotes VALUES(NOW(), ?, ?);";
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setInt(1, DiscordbotsAPI.getInstance().getTotalUpvotes());
        preparedStatement.setInt(2, DiscordbotsAPI.getInstance().getMonthlyUpvotes());
        preparedStatement.execute();
        preparedStatement.close();
    }

    public static void updateInactiveServerMembers() {
        for(Server server: DiscordApiCollection.getInstance().getServers()) {
            ArrayList<Long> userIds = new ArrayList<>();
            for(User user: server.getMembers()) {
                userIds.add(user.getId());
            }

            try {
                for(RankingSlot rankingSlot: DBServer.getPowerPlantRankings(server)) {
                    if (!userIds.contains(rankingSlot.getUserId())) {
                        System.out.println("Server: " + server.getId() + ", User: " + rankingSlot.getUserId());
                        DBUser.updateOnServerStatus(server, rankingSlot.getUserId(), false);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}
