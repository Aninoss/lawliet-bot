package MySQL;

import Constants.FishingCategoryInterface;
import Constants.Permission;
import Constants.PowerPlantStatus;
import Constants.SPAction;
import General.*;
import General.AutoChannel.AutoChannelContainer;
import General.AutoChannel.AutoChannelData;
import General.AutoChannel.TempAutoChannel;
import General.BannedWords.BannedWords;
import General.SPBlock.SPBlock;
import General.Warnings.UserWarnings;
import General.Warnings.WarningSlot;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class DBServer {

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

    public static boolean serverIsInDatabase(Server server) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT serverId FROM DServer WHERE serverId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.execute();
        ResultSet resultSet = preparedStatement.getResultSet();

        boolean exists = resultSet.next();

        resultSet.close();
        preparedStatement.close();

        return exists;
    }

    public static String getPrefix(Server server) throws SQLException {
        return getPrefix(server.getId());
    }

    public static String getPrefix(long serverId) throws SQLException {
        String prefix = DatabaseCache.getInstance().getPrefix(serverId);

        if (prefix == null) {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT prefix FROM DServer WHERE serverId = ?;");
            preparedStatement.setLong(1, serverId);
            preparedStatement.execute();
            ResultSet resultSet = preparedStatement.getResultSet();
            if (resultSet.next()) prefix = resultSet.getString(1);
            resultSet.close();
            preparedStatement.close();

            DatabaseCache.getInstance().setPrefix(serverId, prefix);
        }

        return prefix;
    }

    public static void setPrefix(Server server, String prefix) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("UPDATE DServer SET prefix = ? WHERE serverId = ?;");
        preparedStatement.setString(1, prefix);
        preparedStatement.setLong(2, server.getId());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        DatabaseCache.getInstance().setPrefix(server.getId(), prefix);
    }

    public static void insertServer(Server server) throws SQLException {
        PreparedStatement serverStatement = DBMain.getInstance().preparedStatement("INSERT IGNORE INTO DServer (serverId) VALUES (?);");
        serverStatement.setString(1, server.getIdAsString());
        serverStatement.executeUpdate();
        serverStatement.close();
    }

    public static void removeServer(Server server) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM DServer WHERE serverId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static Locale getServerLocale(Server server) throws SQLException {
        return getServerLocale(server.getId());
    }

    public static Locale getServerLocale(long serverId) throws SQLException {
        Locale locale = DatabaseCache.getInstance().getLocale(serverId);

        if (locale == null) {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT locale FROM DServer WHERE serverId = ?;");
            preparedStatement.setLong(1, serverId);
            preparedStatement.execute();
            ResultSet resultSet = preparedStatement.getResultSet();
            if (resultSet.next()) locale = new Locale(resultSet.getString(1));
            resultSet.close();
            preparedStatement.close();

            DatabaseCache.getInstance().setLocale(serverId, locale);
        }

        return locale;
    }

    public static void setServerLocale(Server server, Locale locale) throws SQLException {
        DatabaseCache.getInstance().setLocale(server.getId(), locale);

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("UPDATE DServer SET locale = ? WHERE serverId = ?;");
        preparedStatement.setString(1, locale.getDisplayName());
        preparedStatement.setLong(2, server.getId());
        preparedStatement.executeUpdate();
        preparedStatement.close();
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

    public static ArrayList<User> getMutedUsers(int groupId) throws SQLException {
        ArrayList<User> users = new ArrayList<>();
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT userId FROM Mute WHERE muteGroupId = ?;");
        preparedStatement.setInt(1, groupId);
        preparedStatement.execute();
        ResultSet resultSet = preparedStatement.getResultSet();
        while(resultSet.next()) {
            long userId = resultSet.getLong(1);
            Optional<User> userOptional = DiscordApiCollection.getInstance().getUserById(userId);
            userOptional.ifPresent(users::add);
        }
        resultSet.close();
        preparedStatement.close();
        return users;
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

    public static BannedWords getBannedWordsFromServer(Server server) throws SQLException {
        BannedWords bannedWords = DatabaseCache.getInstance().getBannedWords(server);

        if (bannedWords == null) {
            bannedWords = new BannedWords(server);

            String sql = "SELECT active FROM BannedWords WHERE serverId = ?;" +
                    "SELECT userId FROM BannedWordsIgnoredUsers WHERE serverId = ?;" +
                    "SELECT userId FROM BannedWordsLogRecievers WHERE serverId = ?;" +
                    "SELECT word FROM BannedWordsWords WHERE serverId = ?;";

            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
            preparedStatement.setLong(1, server.getId());
            preparedStatement.setLong(2, server.getId());
            preparedStatement.setLong(3, server.getId());
            preparedStatement.setLong(4, server.getId());
            preparedStatement.execute();

            //Basisinhalte der Banned Words runterladen
            int i = 0;
            for (ResultSet resultSet : new DBMultipleResultSet(preparedStatement)) {
                switch (i) {
                    case 0:
                        if (resultSet.next()) {
                            bannedWords.setActive(resultSet.getBoolean(1));
                        }
                        break;

                    case 1:
                        while (resultSet.next()) {
                            try {
                                Optional<User> userOptional  = server.getMemberById(resultSet.getLong(1));
                                userOptional.ifPresent(bannedWords::addIgnoredUser);
                            } catch (SQLException e) {
                                //Ignore
                            }
                        }
                        break;

                    case 2:
                        while (resultSet.next()) {
                            try {
                                Optional<User> userOptional  = server.getMemberById(resultSet.getLong(1));
                                userOptional.ifPresent(bannedWords::addLogReciever);
                            } catch (SQLException e) {
                                //Ignore
                            }
                        }
                        break;

                    case 3:
                        while (resultSet.next()) {
                            try {
                                String word = resultSet.getString(1);
                                bannedWords.addWord(word);
                            } catch (SQLException e) {
                                //Ignore
                            }
                        }
                        break;
                }
                i++;
            }
            preparedStatement.close();

            DatabaseCache.getInstance().setBannedWords(server.getId(), bannedWords);
        }

        return bannedWords;
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
                    else channel[i] = server.getTextChannels().get(0);
                }
            }

            WelcomeMessageSetting welcomeMessageSetting = new WelcomeMessageSetting(
                    server,
                    resultSet.getBoolean(1),
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



    public static PowerPlantStatus getPowerPlantStatusFromServer(Server server) throws SQLException {
        PowerPlantStatus powerPlantStatus = DatabaseCache.getInstance().getPowerPlantStatus(server);

        if (powerPlantStatus == null) {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT powerPlant FROM DServer WHERE serverId = ?;");
            preparedStatement.setLong(1, server.getId());
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            if (resultSet.next()) {
                powerPlantStatus = PowerPlantStatus.valueOf(resultSet.getString(1));
                DatabaseCache.getInstance().setPowerPlantStatus(server, powerPlantStatus);
            }

            resultSet.close();
            preparedStatement.close();
        }

        return powerPlantStatus;
    }

    public static boolean getPowerPlantTreasureChestsFromServer(Server server) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT powerPlantTreasureChests FROM DServer WHERE serverId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            boolean treasureChests = resultSet.getBoolean(1);
            resultSet.close();
            preparedStatement.close();
            return treasureChests;
        }

        resultSet.close();
        preparedStatement.close();
        return true;
    }

    public static boolean getPowerPlantRemindersFromServer(Server server) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT powerPlantReminders FROM DServer WHERE serverId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            boolean reminders = resultSet.getBoolean(1);
            resultSet.close();
            preparedStatement.close();
            return reminders;
        }

        resultSet.close();
        preparedStatement.close();
        return true;
    }

    public static boolean getPowerPlantSingleRoleFromServer(Server server) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT powerPlantSingleRole FROM DServer WHERE serverId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            boolean singleRole = resultSet.getBoolean(1);
            resultSet.close();
            preparedStatement.close();
            return singleRole;
        }

        resultSet.close();
        preparedStatement.close();
        return false;
    }

    public static ServerTextChannel getPowerPlantAnnouncementChannelFromServer(Server server) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT powerPlantAnnouncementChannelId FROM DServer WHERE serverId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            long channelId = resultSet.getLong(1);
            resultSet.close();
            preparedStatement.close();

            if (server.getTextChannelById(channelId).isPresent())
                return server.getTextChannelById(channelId).get();
            else return null;
        }

        resultSet.close();
        preparedStatement.close();
        return null;
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

    public static AutoChannelData getAutoChannelFromServer(Server server) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT channelId, active, channelName, creatorCanDisconnect, locked FROM AutoChannel WHERE serverId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            long voiceChannelId = resultSet.getLong(1);
            boolean active = resultSet.getBoolean(2);
            String channelName = resultSet.getString(3);
            //boolean creatorCanDisconnect = resultSet.getBoolean(4);
            boolean creatorCanDisconnect = false;
            boolean locked = resultSet.getBoolean(5);

            ServerVoiceChannel channel = null;
            if (server.getVoiceChannelById(voiceChannelId).isPresent()) {
                channel = server.getVoiceChannelById(voiceChannelId).get();
            }

            AutoChannelData autoChannelData = new AutoChannelData(
                    server,
                    channel,
                    active,
                    channelName,
                    creatorCanDisconnect,
                    locked
            );

            resultSet.close();
            preparedStatement.close();

            return autoChannelData;
        }

        resultSet.close();
        preparedStatement.close();

        return new AutoChannelData(
                server,
                null,
                false,
                "%VCName [%Index]",
                false,
                false
        );
    }

    public static boolean getAutoQuoteFromServer(Server server) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT active FROM AutoQuote WHERE serverId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            boolean active = resultSet.getBoolean(1);
            resultSet.close();
            preparedStatement.close();
            return active;
        }

        resultSet.close();
        preparedStatement.close();

        return true;
    }

    public static void synchronizeAutoChannelChildChannels(DiscordApi api) throws SQLException {
        if (!Bot.isDebug()) {
            Statement statement = DBMain.getInstance().statement("SELECT AutoChannel.serverId, AutoChannelChildChannels.channelId, AutoChannel.channelId FROM AutoChannelChildChannels LEFT JOIN AutoChannel USING(serverId);");
            ResultSet resultSet = statement.getResultSet();

            while (resultSet.next()) {
                long serverId = resultSet.getLong(1);
                long childChannelId = resultSet.getLong(2);
                long parentChannelId = resultSet.getLong(3);

                Server server;
                ServerVoiceChannel childChannel = null;
                ServerVoiceChannel parentChannel;


                boolean found = false;

                Optional<Server> serverOptional = api.getServerById(serverId);
                if (serverOptional.isPresent()) {
                    server = serverOptional.get();
                    Optional<ServerVoiceChannel> optionalServerVoiceChannel = server.getVoiceChannelById(childChannelId);
                    if (optionalServerVoiceChannel.isPresent()) {
                        childChannel = optionalServerVoiceChannel.get();

                        if (childChannel.getConnectedUsers().size() > 0 &&
                                server.getVoiceChannelById(parentChannelId).isPresent()
                        ) {
                            parentChannel = server.getVoiceChannelById(parentChannelId).get();

                            AutoChannelContainer.getInstance().addVoiceChannel(new TempAutoChannel(parentChannel, childChannel));
                            found = true;
                        }
                    }

                    if (!found) {
                        try {
                            if (childChannel != null && PermissionCheckRuntime.getInstance().botHasPermission(getServerLocale(server), "autochannel", childChannel, Permission.MANAGE_CHANNEL))
                                childChannel.delete().get();
                        } catch (ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        removeAutoChannelChildChannel(serverId, childChannelId);
                    }
                }
            }

            resultSet.close();
            statement.close();
        }
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

            DatabaseCache.getInstance().setWhiteListedChannels(server, channels);
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
        ArrayList<Pair<Long, String>> displays = DBServer.getMemberCountDisplays(server);

        if (displays.size() < 5) {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("INSERT INTO MemberCountDisplays VALUES(?, ?, ?);");
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

    public static void saveBannedWords(BannedWords bannedWords) throws SQLException {
        DatabaseCache.getInstance().setBannedWords(bannedWords.getServerId(), bannedWords);

        //Basis
        String sqlString = "REPLACE INTO BannedWords VALUES (?, ?);" +
                "DELETE FROM BannedWordsIgnoredUsers WHERE serverId = ?;" +
                "DELETE FROM BannedWordsLogRecievers WHERE serverId = ?;" +
                "DELETE FROM BannedWordsWords WHERE serverId = ?;";

        PreparedStatement baseStatement = DBMain.getInstance().preparedStatement(sqlString);
        baseStatement.setLong(1, bannedWords.getServerId());
        baseStatement.setBoolean(2, bannedWords.isActive());
        baseStatement.setLong(3, bannedWords.getServerId());
        baseStatement.setLong(4, bannedWords.getServerId());
        baseStatement.setLong(5, bannedWords.getServerId());
        baseStatement.executeUpdate();
        baseStatement.close();

        //Ignorierte User
        if(bannedWords.getIgnoredUserIds().size() > 0) {
            StringBuilder sql = new StringBuilder();
            for(User user: bannedWords.getIgnoredUserIds()) {
                sql.append("INSERT IGNORE INTO BannedWordsIgnoredUsers VALUES (").append(bannedWords.getServerId()).append(",").append(user.getIdAsString()).append(");");
            }
            DBMain.getInstance().statement(sql.toString());
        }

        //Log-Empfänger
        if(bannedWords.getLogRecieverIds().size() > 0) {
            StringBuilder sql = new StringBuilder();
            for(User user: bannedWords.getLogRecieverIds()) {
                sql.append("INSERT IGNORE INTO BannedWordsLogRecievers VALUES (").append(bannedWords.getServerId()).append(",").append(user.getIdAsString()).append(");");
            }
            DBMain.getInstance().statement(sql.toString());
        }

        //Words
        if(bannedWords.getWords().size() > 0) {
            StringBuilder sql = new StringBuilder();
            for(int i = 0; i < bannedWords.getWords().size(); i++) {
                sql.append("INSERT IGNORE INTO BannedWordsWords VALUES (?, ?);");
            }

            baseStatement = DBMain.getInstance().preparedStatement(sql.toString());
            for(int i = 0; i < bannedWords.getWords().size(); i++) {
                String word = bannedWords.getWords().get(i);
                baseStatement.setLong((i * 2) + 1, bannedWords.getServerId());
                baseStatement.setString((i * 2) + 2, word);
            }
            baseStatement.executeUpdate();
            baseStatement.close();
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

    public static void saveAutoChannel(AutoChannelData autoChannelData) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("REPLACE INTO AutoChannel (serverId, channelId, active, channelName, creatorCanDisconnect, locked) VALUES (?, ?, ?, ?, ?, ?)");
        preparedStatement.setLong(1, autoChannelData.getServer().getId());

        ServerVoiceChannel serverVoiceChannel = autoChannelData.getVoiceChannel();
        if (serverVoiceChannel != null) preparedStatement.setLong(2, autoChannelData.getVoiceChannel().getId());
        else preparedStatement.setNull(2, Types.BIGINT);

        preparedStatement.setBoolean(3, autoChannelData.isActive());
        preparedStatement.setString(4, autoChannelData.getChannelName());
        preparedStatement.setBoolean(5, autoChannelData.isCreatorCanDisconnect());
        preparedStatement.setBoolean(6, autoChannelData.isLocked());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static void saveAutoQuote(Server server, boolean active) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("REPLACE INTO AutoQuote (serverId, active) VALUES (?, ?)");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setBoolean(2, active);
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static void addAutoChannelChildChannel(ServerVoiceChannel channel) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("INSERT INTO AutoChannelChildChannels VALUES (?, ?)");
        preparedStatement.setLong(1, channel.getServer().getId());
        preparedStatement.setLong(2, channel.getId());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static void removeAutoChannelChildChannel(ServerVoiceChannel channel) throws SQLException {
        removeAutoChannelChildChannel(channel.getServer().getId(), channel.getId());
    }

    public static void removeAutoChannelChildChannel(long serverId, long channelId) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM AutoChannelChildChannels WHERE serverId = ? AND channelId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.setLong(2, channelId);
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static void saveWhiteListedChannels(Server server, ArrayList<ServerTextChannel> channels) throws SQLException {
        StringBuilder sql = new StringBuilder("DELETE FROM WhiteListedChannels WHERE serverId = " + server.getIdAsString() + ";\n");
        for(ServerTextChannel channel: channels) {
            sql
                    .append("INSERT IGNORE INTO WhiteListedChannels VALUES (")
                    .append(server.getIdAsString())
                    .append(",")
                    .append(channel.getIdAsString())
                    .append(");\n");
        }

        DBMain.getInstance().statement(sql.toString());
        DatabaseCache.getInstance().setWhiteListedChannels(server, channels.stream().map(DiscordEntity::getId).collect(Collectors.toCollection(ArrayList::new)));
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

    public static void savePowerPlantStatusSetting(Server server, PowerPlantStatus status) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("UPDATE DServer SET powerPlant = ? WHERE serverId = ?;");
        preparedStatement.setString(1, status.name());
        preparedStatement.setLong(2, server.getId());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        DatabaseCache.getInstance().setPowerPlantStatus(server, status);
    }

    public static void savePowerPlantTreasureChestsSetting(Server server, boolean treasureChests) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("UPDATE DServer SET powerPlantTreasureChests = ? WHERE serverId = ?;");
        preparedStatement.setBoolean(1, treasureChests);
        preparedStatement.setLong(2, server.getId());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static void savePowerPlantRemindersSetting(Server server, boolean reminders) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("UPDATE DServer SET powerPlantReminders = ? WHERE serverId = ?;");
        preparedStatement.setBoolean(1, reminders);
        preparedStatement.setLong(2, server.getId());
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

        savePowerPlantStatusSetting(server, PowerPlantStatus.STOPPED);
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

    public static void savePowerPlantSingleRole(Server server, boolean singleRole) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("UPDATE DServer SET powerPlantSingleRole = ? WHERE serverId = ?;");
        preparedStatement.setBoolean(1, singleRole);
        preparedStatement.setLong(2, server.getId());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static void savePowerPlantAnnouncementChannel(Server server, ServerTextChannel channel) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("UPDATE DServer SET powerPlantAnnouncementChannelId = ? WHERE serverId = ?;");
        if (channel != null) preparedStatement.setLong(1, channel.getId());
        else preparedStatement.setNull(1, Types.BIGINT);
        preparedStatement.setLong(2, server.getId());
        preparedStatement.executeUpdate();
        preparedStatement.close();
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

    public static Pair<Long, Long> getFisheryRolePrices(Server server) throws SQLException {
        Pair<Long, Long> prices = null;
        String sql = "SELECT powerPlantRoleMin, powerPlantRoleMax FROM DServer WHERE serverId = ?;";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setLong(1, server.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            prices = new Pair<>(resultSet.getLong(1), resultSet.getLong(2));
        }

        return prices;
    }

    public static void savePowerPlantRolePrices(Server server, Pair<Long, Long> prices) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("UPDATE DServer SET powerPlantRoleMin = ?, powerPlantRoleMax = ? WHERE serverId = ?;");
        preparedStatement.setLong(1, prices.getKey());
        preparedStatement.setLong(2, prices.getValue());
        preparedStatement.setLong(3, server.getId());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static double getRoleExp() throws SQLException {
        double exp = 1;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT categoryPower FROM PowerPlantCategories WHERE categoryId = ?;");
        preparedStatement.setInt(1, FishingCategoryInterface.ROLE);
        preparedStatement.execute();
        ResultSet resultSet = preparedStatement.getResultSet();

        if (resultSet.next()) {
            exp = resultSet.getDouble(1);
        }

        return exp;
    }

}
