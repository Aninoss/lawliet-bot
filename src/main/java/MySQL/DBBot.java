package MySQL;

import General.Bot;
import General.DiscordApiCollection;
import General.RankingSlot;
import General.StringTools;
import General.Tracker.TrackerData;
import General.Tracker.TrackerManager;
import ServerStuff.TopGG;
import org.javacord.api.DiscordApi;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

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
            if (StringTools.stringIsDouble(versions[i].replace(".", ""))) {
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

    public static void fisheryCleanUp() throws SQLException {
        String sql = "DELETE FROM PowerPlantUserGained WHERE TIMESTAMPDIFF(HOUR, time, NOW()) > 168;";
        DBMain.getInstance().statement(sql);
    }

    public static synchronized void startTrackers(DiscordApi api) throws SQLException {
        if (!Bot.isDebug()) {
            for (TrackerData trackerData : getTracker(api)) {
                System.out.printf("Starting tracker %s in server id %d...\n", trackerData.getCommand(), trackerData.getChannelId());
                TrackerManager.startTracker(trackerData);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
        preparedStatement.setInt(1, TopGG.getInstance().getTotalUpvotes());
        preparedStatement.setInt(2, TopGG.getInstance().getMonthlyUpvotes());
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
                for(RankingSlot rankingSlot: DBServerOld.getPowerPlantRankings(server)) {
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
