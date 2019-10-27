package MySQL;

import Constants.Permission;
import Constants.PowerPlantStatus;
import Constants.SPAction;
import General.*;
import General.AutoChannel.AutoChannelContainer;
import General.AutoChannel.AutoChannelData;
import General.AutoChannel.TempAutoChannel;
import General.BannedWords.BannedWords;
import General.SPBlock.SPBlock;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class DBServer {
    public static void synchronize(DiscordApi api) throws SQLException, ExecutionException, InterruptedException {
        if (!Bot.isDebug()) {
            System.out.println("Server is getting synchronized...");
            ArrayList<String> dbServerIds = new ArrayList<>();
            Statement statement = DBMain.getInstance().statement("SELECT serverId FROM DServer;");
            ResultSet resultSet = statement.getResultSet();
            while (resultSet.next()) dbServerIds.add(resultSet.getString(1));
            resultSet.close();
            statement.close();

            //Fügt fehlende DB-Einträge hinzu
            for (Server server : api.getServers()) {
                if (!dbServerIds.contains(server.getIdAsString())) {
                    insertServer(server);
                }
            }

            //Manage Auto Channels
            synchronizeAutoChannelChildChannels(api);
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

    public static ArrayList<User> getMutedUsers(DiscordApi api, int groupId) throws SQLException {
        ArrayList<User> users = new ArrayList<>();
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT userId FROM Mute WHERE muteGroupId = ?;");
        preparedStatement.setInt(1, groupId);
        preparedStatement.execute();
        ResultSet resultSet = preparedStatement.getResultSet();
        while(resultSet.next()) {
            long userId = resultSet.getLong(1);
            try {
                users.add(api.getUserById(userId).get());
            } catch (InterruptedException | ExecutionException e) {
                //Ignore
            }
        }
        resultSet.close();
        preparedStatement.close();
        return users;
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
                                User user = server.getApi().getUserById(resultSet.getLong(1)).get();
                                spBlock.addIgnoredUser(user);
                            } catch (SQLException | InterruptedException | ExecutionException e) {
                                //Ignore
                            }
                        }
                        break;

                    case 2:
                        while (resultSet.next()) {
                            try {
                                User user = server.getApi().getUserById(resultSet.getLong(1)).get();
                                spBlock.addLogReciever(user);
                            } catch (SQLException | InterruptedException | ExecutionException e) {
                                //Ignore
                            }
                        }
                        break;

                    case 3:
                        while (resultSet.next()) {
                            if (server.getApi().getServerTextChannelById(resultSet.getLong(1)).isPresent()) {
                                ServerTextChannel serverTextChannel = server.getApi().getServerTextChannelById(resultSet.getLong(1)).get();
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
                                User user = server.getApi().getUserById(resultSet.getLong(1)).get();
                                bannedWords.addIgnoredUser(user);
                            } catch (SQLException | InterruptedException | ExecutionException e) {
                                //Ignore
                            }
                        }
                        break;

                    case 2:
                        while (resultSet.next()) {
                            try {
                                User user = server.getApi().getUserById(resultSet.getLong(1)).get();
                                bannedWords.addLogReciever(user);
                            } catch (SQLException | InterruptedException | ExecutionException e) {
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

            DatabaseCache.getInstance().setBannedWords(server, bannedWords);
        }

        return bannedWords;
    }

    public static WelcomeMessageSetting getWelcomeMessageSettingFromServer(Locale locale, Server server) throws SQLException, IOException {
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
        return false;
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
        ModerationStatus moderationStatus = new ModerationStatus(server, null, true);

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT channelId, question FROM Moderation WHERE serverId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            long channelId = resultSet.getLong(1);
            boolean question = resultSet.getBoolean(2);

            ServerTextChannel channel = null;
            if (server.getTextChannelById(channelId).isPresent()) {
                channel = server.getTextChannelById(channelId).get();
            }

            moderationStatus = new ModerationStatus(server, channel, question);
        }

        resultSet.close();
        preparedStatement.close();

        return moderationStatus;
    }

    public static AutoChannelData getAutoChannelFromServer(Server server) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT channelId, active, channelName FROM AutoChannel WHERE serverId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            long voiceChannelId = resultSet.getLong(1);
            boolean active = resultSet.getBoolean(2);
            String channelName = resultSet.getString(3);

            ServerVoiceChannel channel = null;
            if (server.getVoiceChannelById(voiceChannelId).isPresent()) {
                channel = server.getVoiceChannelById(voiceChannelId).get();
            }

            AutoChannelData autoChannelData = new AutoChannelData(
                    server,
                    channel,
                    active,
                    channelName
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
                "%VCName [%Index]"
        );
    }

    public static void synchronizeAutoChannelChildChannels(DiscordApi api) throws SQLException, ExecutionException, InterruptedException {
        Statement statement = DBMain.getInstance().statement("SELECT AutoChannel.serverId, AutoChannelChildChannels.channelId, AutoChannel.channelId FROM AutoChannelChildChannels LEFT JOIN AutoChannel USING(serverId);");
        ResultSet resultSet = statement.getResultSet();

        while (resultSet.next()) {
            long serverId = resultSet.getLong(1);
            long childChannelId = resultSet.getLong(2);
            long parentChannelId = resultSet.getLong(3);

            Server server = null;
            ServerVoiceChannel childChannel = null;
            ServerVoiceChannel parentChannel = null;


            boolean found = false;

            if (api.getServerById(serverId).isPresent()) {
                server = api.getServerById(serverId).get();
                if (server.getVoiceChannelById(childChannelId).isPresent()) {
                    childChannel = server.getVoiceChannelById(childChannelId).get();

                    if (childChannel.getConnectedUsers().size() > 0 &&
                            server.getVoiceChannelById(parentChannelId).isPresent()) {

                        parentChannel = server.getVoiceChannelById(parentChannelId).get();

                        AutoChannelContainer.getInstance().addVoiceChannel(new TempAutoChannel(parentChannel, childChannel));
                        found = true;

                    }
                }
            }

            if (!found) {
                removeAutoChannelChildChannel(serverId, childChannelId);
                if (childChannel != null && PermissionCheckRuntime.getInstance().botHasPermission(getServerLocale(server), "autochannel", childChannel, Permission.MANAGE_CHANNEL)) childChannel.delete().get();
            }
        }

        resultSet.close();
        statement.close();
    }

    public static ArrayList<ServerTextChannel> getWhiteListedChannels(Server server) throws SQLException {
        ArrayList<ServerTextChannel> channels = DatabaseCache.getInstance().getWhiteListedChannels(server);

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
                    channels.add(serverTextChannel);
                }
            }

            resultSet.close();
            preparedStatement.close();

            DatabaseCache.getInstance().setWhiteListedChannels(server, channels);
        }

        return channels;
    }

    public static ArrayList<Pair<ServerVoiceChannel, String>> getMemberCountDisplays(Server server) throws SQLException {
        ArrayList<Pair<ServerVoiceChannel, String>> displays = DatabaseCache.getInstance().getMemberCountDisplays(server);

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
                    displays.add(new Pair<>(serverVoiceChannel, name));
                }
            }

            resultSet.close();
            preparedStatement.close();

            DatabaseCache.getInstance().setMemberCountDisplays(server, displays);
        }

        return displays;
    }

    public static void addMemberCountDisplay(Pair<ServerVoiceChannel, String> display) throws SQLException {
        Server server = display.getKey().getServer();
        ArrayList<Pair<ServerVoiceChannel, String>> displays = DBServer.getMemberCountDisplays(server);

        if (displays.size() < 5) {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("INSERT INTO MemberCountDisplays VALUES(?, ?, ?);");
            preparedStatement.setLong(1, server.getId());
            preparedStatement.setLong(2, display.getKey().getId());
            preparedStatement.setString(3, display.getValue());
            preparedStatement.execute();
            preparedStatement.close();

            DatabaseCache.getInstance().addMemberCountDisplay(display);
        }
    }

    public static void removeMemberCountDisplay(Pair<ServerVoiceChannel, String> display) throws SQLException {
        Server server = display.getKey().getServer();

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM MemberCountDisplays WHERE serverId = ? AND vcId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, display.getKey().getId());
        preparedStatement.execute();
        preparedStatement.close();

        DatabaseCache.getInstance().removeMemberCountDisplay(display);
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
        DatabaseCache.getInstance().setBannedWords(bannedWords.getServer(), bannedWords);

        //Basis
        String sqlString = "REPLACE INTO BannedWords VALUES (?, ?);" +
                "DELETE FROM BannedWordsIgnoredUsers WHERE serverId = ?;" +
                "DELETE FROM BannedWordsLogRecievers WHERE serverId = ?;" +
                "DELETE FROM BannedWordsWords WHERE serverId = ?;";

        PreparedStatement baseStatement = DBMain.getInstance().preparedStatement(sqlString);
        baseStatement.setLong(1, bannedWords.getServer().getId());
        baseStatement.setBoolean(2, bannedWords.isActive());
        baseStatement.setLong(3, bannedWords.getServer().getId());
        baseStatement.setLong(4, bannedWords.getServer().getId());
        baseStatement.setLong(5, bannedWords.getServer().getId());
        baseStatement.executeUpdate();
        baseStatement.close();

        //Ignorierte User
        if(bannedWords.getIgnoredUser().size() > 0) {
            StringBuilder sql = new StringBuilder();
            for(User user: bannedWords.getIgnoredUser()) {
                sql.append("INSERT IGNORE INTO BannedWordsIgnoredUsers VALUES (").append(bannedWords.getServer().getIdAsString()).append(",").append(user.getIdAsString()).append(");");
            }
            DBMain.getInstance().statement(sql.toString());
        }

        //Log-Empfänger
        if(bannedWords.getLogRecievers().size() > 0) {
            StringBuilder sql = new StringBuilder();
            for(User user: bannedWords.getLogRecievers()) {
                sql.append("INSERT IGNORE INTO BannedWordsLogRecievers VALUES (").append(bannedWords.getServer().getIdAsString()).append(",").append(user.getIdAsString()).append(");");
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
                baseStatement.setLong((i * 2) + 1, bannedWords.getServer().getId());
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
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("REPLACE INTO AutoChannel (serverId, channelId, active, channelName) VALUES (?, ?, ?, ?)");
        preparedStatement.setLong(1, autoChannelData.getServer().getId());

        ServerVoiceChannel serverVoiceChannel = autoChannelData.getVoiceChannel();
        if (serverVoiceChannel != null) preparedStatement.setLong(2, autoChannelData.getVoiceChannel().getId());
        else preparedStatement.setNull(2, Types.BIGINT);

        preparedStatement.setBoolean(3, autoChannelData.isActive());
        preparedStatement.setString(4, autoChannelData.getChannelName());
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
        DatabaseCache.getInstance().setWhiteListedChannels(server, channels);
    }

    public static void saveModeration(ModerationStatus moderationStatus) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("REPLACE INTO Moderation VALUES (?, ?, ?);");
        preparedStatement.setLong(1, moderationStatus.getServer().getId());
        if (moderationStatus.getChannel() != null) preparedStatement.setLong(2, moderationStatus.getChannel().getId());
        else preparedStatement.setNull(2, Types.BIGINT);
        preparedStatement.setBoolean(3, moderationStatus.isQuestion());
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
        int i = 1;
        int rank = i;

        long growthPrevious = -1, joulePrevious = -1, coinsPrevious = -1;

        while(resultSet != null && resultSet.next()) {
            User user = null;
            long userId = resultSet.getLong(1);
            if (server.getMemberById(userId).isPresent())
                user = server.getMemberById(userId).get();
            //int rank = resultSet.getInt(2);
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
        }

        resultSet.close();
        preparedStatement.close();

        return rankingSlots;
    }
}
