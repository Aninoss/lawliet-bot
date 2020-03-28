package MySQL;

import Constants.*;
import General.*;
import General.SPBlock.SPBlock;
import General.Warnings.UserWarnings;
import General.Warnings.WarningSlot;
import MySQL.Server.DBServer;
import javafx.util.Pair;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.sql.*;
import java.time.Instant;
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

    public static UserWarnings getWarningsForUser(Server server, User user) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT time, requestorUserId, reason FROM Warnings WHERE serverId = ? AND userId = ? ORDER BY time DESC;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, user.getId());
        preparedStatement.execute();

        UserWarnings userWarnings = new UserWarnings();
        ResultSet resultSet = preparedStatement.getResultSet();
        while(resultSet.next()) {
            Instant time = resultSet.getTimestamp(1).toInstant();
            long requestorUserId = resultSet.getLong(2);
            Optional<User> requestor = DiscordApiCollection.getInstance().getUserById(requestorUserId);
            String reason = resultSet.getString(3);
            if (reason.isEmpty()) reason = null;
            userWarnings.add(new WarningSlot(time, requestor.orElse(null), reason));
        }

        resultSet.close();
        preparedStatement.close();
        return userWarnings;
    }

    public static void removeWarningsForUser(Server server, User user, int n) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM Warnings WHERE serverId = ? AND userId = ? ORDER BY time DESC LIMIT ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, user.getId());
        preparedStatement.setInt(3, n);
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static SPBlock getSPBlockFromServer(Server server) throws SQLException {
        SPBlock spBlock = new SPBlock(server);

        String sql = "SELECT active, action, blockURLName FROM SPBlock WHERE serverId = ?;" +
                "SELECT userId FROM SPBlockIgnoredUsers WHERE serverId = ?;" +
                "SELECT userId FROM SPBlockLogRecievers WHERE serverId = ?;" +
                "SELECT channelId FROM SPBlockIgnoredChannels WHERE serverId = ?;";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setLong(1,server.getId());
        preparedStatement.setLong(2,server.getId());
        preparedStatement.setLong(3,server.getId());
        preparedStatement.setLong(4,server.getId());
        preparedStatement.execute();

        //Basisinhalte des SPBlocks runterladen
        int i=0;
        for(ResultSet resultSet: new DBMultipleResultSet(preparedStatement)) {
                switch (i) {
                    case 0:
                        if (resultSet.next()) {
                            spBlock.setActive(resultSet.getBoolean(1));
                            spBlock.setAction(SPAction.valueOf(resultSet.getString(2)));
                            //spBlock.setBlockName(resultSet.getBoolean(3));
                            spBlock.setBlockName(false);
                        }
                        break;

                    case 1:
                        while (resultSet.next()) {
                            try {
                                Optional<User> userOptional  = server.getMemberById(resultSet.getLong(1));
                                userOptional.ifPresent(spBlock::addIgnoredUser);
                            } catch (SQLException e) {
                                //Ignore
                            }
                        }
                        break;

                    case 2:
                        while (resultSet.next()) {
                            try {
                                Optional<User> userOptional  = server.getMemberById(resultSet.getLong(1));
                                userOptional.ifPresent(spBlock::addLogReciever);
                            } catch (SQLException e) {
                                //Ignore
                            }
                        }
                        break;

                    case 3:
                        while (resultSet.next()) {
                            if (server.getTextChannelById(resultSet.getLong(1)).isPresent()) {
                                ServerTextChannel serverTextChannel = server.getTextChannelById(resultSet.getLong(1)).get();
                                spBlock.addIgnoredChannel(serverTextChannel);
                            }
                        }
                        break;
            }
            i++;
        }
        preparedStatement.close();

        return spBlock;
    }

    public static ArrayList<String> getNSFWFilterFromServer(Server server) throws SQLException {
        ArrayList<String> nsfwFilter = DatabaseCache.getInstance().getNSFWFilter(server);

        if (nsfwFilter == null) {
            nsfwFilter = new ArrayList<>();

            String sql = "SELECT keyword FROM NSFWFilter WHERE serverId = ?;";

            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
            preparedStatement.setLong(1, server.getId());
            preparedStatement.execute();
            ResultSet resultSet = preparedStatement.getResultSet();

            while(resultSet.next()) {
                nsfwFilter.add(resultSet.getString(1));
            }

            resultSet.close();
            preparedStatement.close();

            DatabaseCache.getInstance().setNSFWFilter(server, nsfwFilter);
        }

        return nsfwFilter;
    }

    public static WelcomeMessageSetting getWelcomeMessageSettingFromServer(Locale locale, Server server) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT activated, title, description, channel, goodbye, goodbyeText, goodbyeChannel FROM ServerWelcomeMessage WHERE serverId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            ServerTextChannel[] channel = new ServerTextChannel[2];
            int[] arg = {4, 7};

            for(int i = 0; i < 2; i++) {
                long channelId = resultSet.getLong(arg[i]);
                if (server.getTextChannelById(channelId).isPresent()) {
                    channel[i] = server.getTextChannelById(channelId).get();
                } else {
                    if (server.getSystemChannel().isPresent()) channel[i] = server.getSystemChannel().get();
                    else if (server.getTextChannels().size() > 0) channel[i] = server.getTextChannels().get(0);
                    else channel[i] = null;
                }
            }

            WelcomeMessageSetting welcomeMessageSetting = new WelcomeMessageSetting(
                    server,
                    channel[0] != null && channel[1] != null && resultSet.getBoolean(1),
                    resultSet.getString(2),
                    resultSet.getString(3),
                    channel[0],
                    resultSet.getBoolean(5),
                    resultSet.getString(6),
                    channel[1]
                    );
            resultSet.close();

            return welcomeMessageSetting;
        }
        resultSet.close();
        preparedStatement.close();

        ServerTextChannel postChannel;
        if (server.getSystemChannel().isPresent()) postChannel = server.getSystemChannel().get();
        else {
            if (server.getTextChannels().size() > 0) postChannel = server.getTextChannels().get(0);
            else return null;
        }
        return new WelcomeMessageSetting(
                server,
                false,
                TextManager.getString(locale, TextManager.COMMANDS, "welcome_standard_title"),
                TextManager.getString(locale, TextManager.COMMANDS, "welcome_standard_description"),
                postChannel,
                false,
                TextManager.getString(locale, TextManager.COMMANDS, "welcome_standard_goodbye"),
                postChannel);
    }

    public static ArrayList<Role> getBasicRolesFromServer(Server server) throws SQLException {
        ArrayList<Role> roleList = new ArrayList<>();

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT roleId FROM BasicRole WHERE serverId = ?;");
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

        return roleList;
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

    public static ModerationStatus getModerationFromServer(Server server) throws SQLException {
        ModerationStatus moderationStatus = new ModerationStatus(server, null, true, 0, 0, 30, 30);

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT channelId, question, autoKick, autoBan, autoKickDays, autoBanDays FROM Moderation WHERE serverId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            long channelId = resultSet.getLong(1);
            boolean question = resultSet.getBoolean(2);
            int autoKick = resultSet.getInt(3);
            int autoBan = resultSet.getInt(4);
            int autoKickDays = resultSet.getInt(5);
            int autoBanDays = resultSet.getInt(6);

            ServerTextChannel channel = null;
            if (server.getTextChannelById(channelId).isPresent()) {
                channel = server.getTextChannelById(channelId).get();
            }

            moderationStatus = new ModerationStatus(server, channel, question, autoKick, autoBan, autoKickDays, autoBanDays);
        }

        resultSet.close();
        preparedStatement.close();

        return moderationStatus;
    }

    public static ArrayList<ServerTextChannel> getWhiteListedChannels(Server server) throws SQLException {
        ArrayList<Long> channels = DatabaseCache.getInstance().getWhiteListedChannels(server);
        ArrayList<ServerTextChannel> channelObjects = new ArrayList<>();

        if (channels == null) {
            channels = new ArrayList<>();

            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT channelId FROM WhiteListedChannels WHERE serverId = ?;");
            preparedStatement.setLong(1, server.getId());
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            while (resultSet.next()) {
                long id = resultSet.getLong(1);
                if (id != 0 && server.getChannelById(id).isPresent()) {
                    ServerTextChannel serverTextChannel = server.getTextChannelById(id).get();
                    channels.add(serverTextChannel.getId());
                    channelObjects.add(serverTextChannel);
                }
            }

            resultSet.close();
            preparedStatement.close();

            channels.forEach(channel -> DatabaseCache.getInstance().addWhiteListedChannel(server, channel));
        } else {
            channels.stream()
                    .filter(channelId -> server.getTextChannelById(channelId).isPresent())
                    .map(channelId -> server.getTextChannelById(channelId).get())
                    .forEach(channelObjects::add);
        }

        return channelObjects;
    }

    public static ArrayList<Pair<Long, String>> getMemberCountDisplays(Server server) throws SQLException {
        ArrayList<Pair<Long, String>> displays = DatabaseCache.getInstance().getMemberCountDisplays(server);

        if (displays == null) {
            displays = new ArrayList<>();

            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT vcId, name FROM MemberCountDisplays WHERE serverId = ?;");
            preparedStatement.setLong(1, server.getId());
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            while (resultSet.next()) {
                long vcId = resultSet.getLong(1);
                String name = resultSet.getString(2);
                if (vcId != 0 && server.getVoiceChannelById(vcId).isPresent()) {
                    ServerVoiceChannel serverVoiceChannel = server.getVoiceChannelById(vcId).get();
                    displays.add(new Pair<>(serverVoiceChannel.getId(), name));
                } else {
                    removeMemberCountDisplay(server.getId(), vcId);
                }
            }

            resultSet.close();
            preparedStatement.close();

            DatabaseCache.getInstance().setMemberCountDisplays(server, displays);
        } else {
            for(Pair<Long, String> display: new ArrayList<>(displays)) {
                Optional<ServerVoiceChannel> channelOptional = server.getVoiceChannelById(display.getKey());
                if (!channelOptional.isPresent()) {
                    removeMemberCountDisplay(server, display);
                }
            }
        }

        return displays;
    }

    public static void addMemberCountDisplay(Pair<ServerVoiceChannel, String> display) throws SQLException {
        Server server = display.getKey().getServer();
        ArrayList<Pair<Long, String>> displays = DBServerOld.getMemberCountDisplays(server);

        if (displays.size() < 5) {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("REPLACE INTO MemberCountDisplays VALUES(?, ?, ?);");
            preparedStatement.setLong(1, server.getId());
            preparedStatement.setLong(2, display.getKey().getId());
            preparedStatement.setString(3, display.getValue());
            preparedStatement.execute();
            preparedStatement.close();

            DatabaseCache.getInstance().addMemberCountDisplay(server, new Pair<>(display.getKey().getId(), display.getValue()));
        }
    }

    public static void removeMemberCountDisplay(Pair<ServerVoiceChannel, String> display) throws SQLException {
        removeMemberCountDisplay(display.getKey().getServer(), new Pair<>(display.getKey().getId(), display.getValue()));
    }

    public static void removeMemberCountDisplay(Server server, Pair<Long, String> display) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM MemberCountDisplays WHERE serverId = ? AND vcId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, display.getKey());
        preparedStatement.execute();
        preparedStatement.close();

        DatabaseCache.getInstance().removeMemberCountDisplay(server, display);
    }

    public static void removeMemberCountDisplay(long serverId, long vcId) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM MemberCountDisplays WHERE serverId = ? AND vcId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.setLong(2, vcId);
        preparedStatement.execute();
        preparedStatement.close();

        DatabaseCache.getInstance().removeMemberCountDisplay(serverId, vcId);
    }

    public static void addNSFWFilterKeyword(Server server, String keyword) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("INSERT INTO NSFWFilter VALUES(?, ?);");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setString(2, keyword);
        preparedStatement.execute();
        preparedStatement.close();

        ArrayList<String> nsfwFilter = DatabaseCache.getInstance().getNSFWFilter(server);
        if (nsfwFilter == null) {
            nsfwFilter = new ArrayList<>();
            DatabaseCache.getInstance().setNSFWFilter(server, nsfwFilter);
        }
        nsfwFilter.add(keyword);
    }

    public static void removeNSFWFilterKeyword(Server server, String keyword) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM NSFWFilter WHERE serverId = ? AND keyword = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setString(2, keyword);
        preparedStatement.execute();
        preparedStatement.close();

        ArrayList<String> nsfwFilter = DatabaseCache.getInstance().getNSFWFilter(server);
        if (nsfwFilter != null) nsfwFilter.remove(keyword);
    }

    public static boolean isChannelWhitelisted(ServerTextChannel channel) throws SQLException {
        ArrayList<ServerTextChannel> channels = getWhiteListedChannels(channel.getServer());
        if (channels.size() == 0) return true;
        else return channels.contains(channel);
    }

    public static void saveSPBlock(SPBlock spBlock) throws SQLException {
        //Basis
        String sqlString = "REPLACE INTO SPBlock VALUES (?, ?, ?, ?);" +
                "DELETE FROM SPBlockIgnoredChannels WHERE serverId = ?;" +
                "DELETE FROM SPBlockIgnoredUsers WHERE serverId = ?;" +
                "DELETE FROM SPBlockLogRecievers WHERE serverId = ?;";

        PreparedStatement baseStatement = DBMain.getInstance().preparedStatement(sqlString);
        baseStatement.setLong(1, spBlock.getServer().getId());
        baseStatement.setBoolean(2, spBlock.isActive());
        baseStatement.setString(3, spBlock.getAction().name());
        baseStatement.setBoolean(4, spBlock.isBlockName());
        baseStatement.setBoolean(5, spBlock.isBlockName());
        baseStatement.setBoolean(6, spBlock.isBlockName());
        baseStatement.setBoolean(7, spBlock.isBlockName());
        baseStatement.executeUpdate();
        baseStatement.close();

        //Ignorierte User
        if(spBlock.getIgnoredUser().size() > 0) {
            StringBuilder sql = new StringBuilder();
            for(User user: spBlock.getIgnoredUser()) {
                sql.append("INSERT IGNORE INTO SPBlockIgnoredUsers VALUES (").append(spBlock.getServer().getIdAsString()).append(",").append(user.getIdAsString()).append(");");
            }
            DBMain.getInstance().statement(sql.toString());
        }

        //Log-Empfänger
        if(spBlock.getLogRecievers().size() > 0) {
            StringBuilder sql = new StringBuilder();
            for(User user: spBlock.getLogRecievers()) {
                sql.append("INSERT IGNORE INTO SPBlockLogRecievers VALUES (").append(spBlock.getServer().getIdAsString()).append(",").append(user.getIdAsString()).append(");");
            }
            DBMain.getInstance().statement(sql.toString());
        }

        //Ignorierte Channel
        if(spBlock.getIgnoredChannels().size() > 0) {
            StringBuilder sql = new StringBuilder();
            for(ServerTextChannel channel: spBlock.getIgnoredChannels()) {
                sql.append("INSERT IGNORE INTO SPBlockIgnoredChannels VALUES (").append(spBlock.getServer().getIdAsString()).append(",").append(channel.getIdAsString()).append(");");
            }
            DBMain.getInstance().statement(sql.toString());
        }
    }

    public static void saveWelcomeMessageSetting(WelcomeMessageSetting welcomeMessageSetting) throws SQLException {
        PreparedStatement baseStatement = DBMain.getInstance().preparedStatement("REPLACE INTO ServerWelcomeMessage VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
        baseStatement.setLong(1, welcomeMessageSetting.getServer().getId());
        baseStatement.setBoolean(2, welcomeMessageSetting.isActivated());
        baseStatement.setString(3, welcomeMessageSetting.getTitle());
        baseStatement.setString(4, welcomeMessageSetting.getDescription());
        baseStatement.setLong(5, welcomeMessageSetting.getWelcomeChannel().getId());
        baseStatement.setBoolean(6, welcomeMessageSetting.isGoodbye());
        baseStatement.setString(7, welcomeMessageSetting.getGoodbyeText());
        baseStatement.setLong(8, welcomeMessageSetting.getFarewellChannel().getId());
        baseStatement.executeUpdate();
        baseStatement.close();
    }

    public static void addBasicRoles(Server server, Role role) throws SQLException {
        PreparedStatement baseStatement = DBMain.getInstance().preparedStatement("INSERT IGNORE INTO BasicRole VALUES (?, ?);");
        baseStatement.setLong(1, server.getId());
        baseStatement.setLong(2, role.getId());
        baseStatement.executeUpdate();
        baseStatement.close();
    }

    public static void addPowerPlantRoles(Server server, Role role) throws SQLException {
        PreparedStatement baseStatement = DBMain.getInstance().preparedStatement("INSERT IGNORE INTO PowerPlantRoles VALUES (?, ?);");
        baseStatement.setLong(1, server.getId());
        baseStatement.setLong(2, role.getId());
        baseStatement.executeUpdate();
        baseStatement.close();
    }

    public static void removeWhiteListedChannel(Server server, ServerTextChannel channel) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM WhiteListedChannels WHERE serverId = ? AND channelId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, channel.getId());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        DatabaseCache.getInstance().removeWhiteListedChannel(server, channel.getId());
    }

    public static void addWhiteListedChannel(Server server, ServerTextChannel channel) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("INSERT IGNORE INTO WhiteListedChannels VALUES (?, ?);");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, channel.getId());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        DatabaseCache.getInstance().addWhiteListedChannel(server, channel.getId());
    }

    public static void saveModeration(ModerationStatus moderationStatus) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("REPLACE INTO Moderation (serverId, channelId, question, autoKick, autoBan, autoKickDays, autoBanDays) VALUES (?, ?, ?, ?, ?, ?, ?);");
        preparedStatement.setLong(1, moderationStatus.getServer().getId());
        if (moderationStatus.getChannel().isPresent()) preparedStatement.setLong(2, moderationStatus.getChannel().get().getId());
        else preparedStatement.setNull(2, Types.BIGINT);
        preparedStatement.setBoolean(3, moderationStatus.isQuestion());
        preparedStatement.setInt(4, moderationStatus.getAutoKick());
        preparedStatement.setInt(5, moderationStatus.getAutoBan());
        preparedStatement.setInt(6, moderationStatus.getAutoKickDays());
        preparedStatement.setInt(7, moderationStatus.getAutoBanDays());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static void removeBasicRoles(Server server, Role role) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM BasicRole WHERE serverId = ? AND roleId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, role.getId());
        preparedStatement.executeUpdate();
        preparedStatement.close();
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
            DBServer.getInstance().getServerBean(server.getId()).setFisheryStatus(FisheryStatus.STOPPED);
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

    public static void insertWarning(Server server, User user, User requestor, String reason) throws SQLException {
        PreparedStatement serverStatement = DBMain.getInstance().preparedStatement("INSERT INTO Warnings (serverId, userId, requestorUserId, reason) VALUES (?, ?, ?, ?);");
        serverStatement.setLong(1, server.getId());
        serverStatement.setLong(2, user.getId());
        serverStatement.setLong(3, requestor.getId());
        serverStatement.setString(4, reason);
        serverStatement.executeUpdate();
        serverStatement.close();
    }

}
