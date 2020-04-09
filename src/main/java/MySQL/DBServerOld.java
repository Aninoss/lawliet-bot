package MySQL;

import Constants.*;
import Core.*;
import Modules.RankingSlot;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class DBServerOld {

    public static void synchronize(DiscordApi api) throws SQLException, ExecutionException, InterruptedException {
        if (!Bot.isDebug()) {
            System.out.println("Servers are getting synchronized...");
            ArrayList<String> dbServerIds = new ArrayList<>();
            Statement statement = DBMain.getInstance().statement("SELECT serverId FROM DServer;");
            ResultSet resultSet = statement.getResultSet();
            while (resultSet.next()) dbServerIds.add(resultSet.getString(1));
            resultSet.close();
            statement.close();

            DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();

            //Inserts missing database entries
            for (Server server : api.getServers()) {
                if (!dbServerIds.contains(server.getIdAsString())) {
                    insertServer(server);
                }
            }
        }
    }

    public static void insertServer(Server server) throws SQLException {
        PreparedStatement serverStatement = DBMain.getInstance().preparedStatement("INSERT IGNORE INTO DServer (serverId) VALUES (?);");
        serverStatement.setString(1, server.getIdAsString());
        serverStatement.executeUpdate();
        serverStatement.close();
    }

    public static ArrayList<Role> getPowerPlantRolesFromServer(Server server) throws SQLException {
        ArrayList<Role> roleList = new ArrayList<>();

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT roleId FROM PowerPlantRoles WHERE serverId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        while (resultSet.next()) {
            long id = resultSet.getLong(1);
            if (server.getRoleById(id).isPresent()) {
                roleList.add(server.getRoleById(id).get());
            }
        }
        resultSet.close();
        preparedStatement.close();

        roleList.sort(Comparator.comparingInt(Role::getPosition));
        return roleList;
    }

    public static ArrayList<ServerTextChannel> getPowerPlantIgnoredChannelsFromServer(Server server) throws SQLException {
        ArrayList<ServerTextChannel> channelList = new ArrayList<>();

        for(Long id: getPowerPlantIgnoredChannelIdsFromServer(server)) {
            if (server.getTextChannelById(id).isPresent())
                channelList.add(server.getTextChannelById(id).get());
        }

        return channelList;
    }

    public static ArrayList<Long> getPowerPlantIgnoredChannelIdsFromServer(Server server) throws SQLException {
        ArrayList<Long> channelIds = DatabaseCache.getInstance().getPowerPlantIgnoredChannels(server);

        if (channelIds == null) {
            channelIds = new ArrayList<>();

            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT channelId FROM PowerPlantIgnoredChannels WHERE serverId = ?;");
            preparedStatement.setLong(1, server.getId());
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            while (resultSet.next()) {
                channelIds.add(resultSet.getLong(1));
            }
            resultSet.close();
            preparedStatement.close();

            DatabaseCache.getInstance().setPowerPlantIgnoredChannels(server, channelIds);
        }

        return channelIds;
    }

    public static void addPowerPlantRoles(Server server, Role role) throws SQLException {
        PreparedStatement baseStatement = DBMain.getInstance().preparedStatement("INSERT IGNORE INTO PowerPlantRoles VALUES (?, ?);");
        baseStatement.setLong(1, server.getId());
        baseStatement.setLong(2, role.getId());
        baseStatement.executeUpdate();
        baseStatement.close();
    }

    public static void removePowerPlantRoles(Server server, Role role) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM PowerPlantRoles WHERE serverId = ? AND roleId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, role.getId());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }


    public static void removePowerPlant(Server server) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM PowerPlantUserPowerUp WHERE serverId = ?;" +
                "DELETE FROM PowerPlantUsers WHERE serverId = ?;" +
                "DELETE FROM PowerPlantUserGained WHERE serverId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, server.getId());
        preparedStatement.setLong(3, server.getId());
        preparedStatement.executeUpdate();

        try {
            DBServer.getInstance().getBean(server.getId()).setFisheryStatus(FisheryStatus.STOPPED);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void savePowerPlantIgnoredChannels(Server server, ArrayList<ServerTextChannel> channels) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM PowerPlantIgnoredChannels WHERE serverId = ").append(server.getIdAsString()).append(";");

        for(ServerTextChannel channel: channels) {
            sql.append("INSERT IGNORE INTO PowerPlantIgnoredChannels VALUES (").append(server.getIdAsString()).append(",").append(channel.getIdAsString()).append(");");
        }

        DBMain.getInstance().statement(sql.toString());


        ArrayList<Long> channelIds = new ArrayList<>();
        channels.forEach(channel -> channelIds.add(channel.getId()));

        DatabaseCache.getInstance().setPowerPlantIgnoredChannels(server, channelIds);
    }

    public static ArrayList<RankingSlot> getPowerPlantRankings(Server server) throws SQLException {
        String sql = "SELECT userId, getGrowth(serverId, userId) growth, coins, joule FROM (SELECT * FROM PowerPlantUsers WHERE serverId = ? AND onServer = true) serverMembers ORDER BY growth DESC, joule DESC, coins DESC;";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setLong(1, server.getId());
        preparedStatement.execute();
        ResultSet resultSet = preparedStatement.getResultSet();

        ArrayList<RankingSlot> rankingSlots = new ArrayList<>();
        ArrayList<Long> missingUserId = new ArrayList<>();

        int i = 1;
        int rank = i;

        long growthPrevious = -1, joulePrevious = -1, coinsPrevious = -1;

        while(resultSet != null && resultSet.next()) {
            long userId = resultSet.getLong(1);
            Optional<User> userOptional = server.getMemberById(userId);

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                long growth = resultSet.getLong(2);
                long coins = resultSet.getLong(3);
                long joule = resultSet.getLong(4);

                if (growth != growthPrevious || joule != joulePrevious || coins != coinsPrevious) {
                    growthPrevious = growth;
                    joulePrevious = joule;
                    coinsPrevious = coins;
                    rank = i;
                }

                rankingSlots.add(new RankingSlot(rank, joule, coins, growth, user, userId));
                i++;
            } else {
                missingUserId.add(userId);
            }
        }

        Thread t = new Thread(() -> {
            for(long userId: missingUserId) {
                try {
                    DBUser.updateOnServerStatus(server, userId, false);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
        t.setName("users_setinactive");
        t.setPriority(1);
        t.start();

        resultSet.close();
        preparedStatement.close();

        return rankingSlots;
    }

}
