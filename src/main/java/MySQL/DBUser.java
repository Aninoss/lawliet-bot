package MySQL;

import Constants.CodeBlockColor;
import Constants.Settings;
import General.*;
import General.Fishing.FishingSlot;
import General.Fishing.FishingProfile;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.awt.*;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DBUser {
    public static void synchronize(DiscordApi api) throws Throwable {
        if (!Bot.isDebug()) {
            System.out.println("User werden synchronisiert...");

            new Thread(() -> {
                ArrayList<Long> userList = new ArrayList<>();

                for (Server server : api.getServers()) {
                    for (User user : server.getMembers()) {
                        long id = user.getId();
                        if (!userList.contains(id)) userList.add(id);
                    }
                }

                try {
                    Statement statement = DBMain.getInstance().statement("SELECT userId FROM DUser;");
                    ResultSet resultSet = statement.getResultSet();

                    while (resultSet.next()) {
                        long id = resultSet.getLong(1);
                        userList.remove(id);
                    }

                    resultSet.close();
                    statement.close();

                    //Fügt fehlende DB-Einträge hinzu
                    insertUserIds(userList);

                    System.out.println("User-Synchronisation abgeschlossen!");
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public static void insertUsers(Collection<User> users) throws Throwable {
        insertUsers(new ArrayList<>(users));
    }

    public static void insertUsers(ArrayList<User> users) throws Throwable {
        if (users.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (User user : users) {
                sb.append("INSERT IGNORE INTO DUser (userId) VALUES (").append(user.getIdAsString()).append("); ");
            }

            Statement statement = DBMain.getInstance().statement(sb.toString());
            statement.close();
        }
    }

    public static void insertUserIds(ArrayList<Long> userIds) throws Throwable {
        if (userIds.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (long id: userIds) {
                sb.append("INSERT IGNORE INTO DUser (userId) VALUES (").append(id).append("); ");
            }

            Statement statement = DBMain.getInstance().statement(sb.toString());
            statement.close();
        }
    }

    public static void insertUser(User user) throws Throwable {
        ArrayList<User> users = new ArrayList<>();
        users.add(user);
        insertUsers(users);
    }

    public static void updateOsuUsername(User user, String username) throws Throwable {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("UPDATE DUser SET osuUsername = ? WHERE userId = ?;");
        preparedStatement.setString(1, username);
        preparedStatement.setLong(2, user.getId());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static String getOsuUsername(User user) throws Throwable {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT osuUsername FROM DUser WHERE userId = ?;");
        preparedStatement.setLong(1, user.getId());
        preparedStatement.execute();
        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            String str = resultSet.getString(1);
            resultSet.close();
            return str;
        } else {
            resultSet.close();
            return null;
        }
    }

    public static void addJouleBulk(Map<Long, ServerTextChannel> activities) throws Throwable {
        StringBuilder totalSql = new StringBuilder();

        try {
            for(long userId: activities.keySet()) {
                ServerTextChannel channel = activities.get(userId);

                String sql = "INSERT INTO PowerPlantUserGained " +
                        "VALUES(" +
                        "%s, " +
                        "%u, " +
                        "DATE_FORMAT(NOW(), " +
                        "'%Y-%m-%d %H:00:00'), " +
                        "IF((SELECT powerPlant FROM DServer WHERE serverId = %s) = 'ACTIVE' AND %c NOT IN (SELECT channelId FROM PowerPlantIgnoredChannels WHERE serverId = %s)," +
                        "LEAST(%max, %a)," +
                        "0" +
                        ")) " +
                        "ON DUPLICATE KEY UPDATE coinsGrowth = " +
                        "IF((SELECT powerPlant FROM DServer WHERE serverId = %s) = 'ACTIVE' AND %c NOT IN (SELECT channelId FROM PowerPlantIgnoredChannels WHERE serverId = %s) AND TIMESTAMPDIFF(SECOND, (SELECT lastMessage FROM PowerPlantUsers WHERE serverId = %s and userId = %u), NOW()) >= 20," +
                        "LEAST(%max, coinsGrowth + %a)," +
                        "coinsGrowth" +
                        ");";

                sql +=
                        "INSERT INTO PowerPlantUsers (serverId, userId, onServer, joule) " +
                                "VALUES (" +
                                "%s," +
                                "%u," +
                                "1," +
                                "IF((SELECT powerPlant FROM DServer WHERE serverId = %s) = 'ACTIVE' AND %c NOT IN (SELECT channelId FROM PowerPlantIgnoredChannels WHERE serverId = %s)," +
                                "LEAST(%max, %a)," +
                                "0" +
                                ")) ON DUPLICATE KEY UPDATE joule = IF((SELECT powerPlant FROM DServer WHERE serverId = %s) = 'ACTIVE' AND %c NOT IN (SELECT channelId FROM PowerPlantIgnoredChannels WHERE serverId = %s) AND TIMESTAMPDIFF(SECOND, lastMessage, NOW()) >= 20," +
                                "LEAST(%max, joule + %a)," +
                                "joule" +
                                "), lastMessage = DATE_ADD(NOW(), INTERVAL -MOD(SECOND(NOW()), 20) SECOND), onServer = 1;";

                sql += "SELECT (joule >= 100 AND reminderSent = 0), userId, coins FROM PowerPlantUsers WHERE serverId = %s AND userId = %u;";

                // AND TIMESTAMPDIFF(MINUTE, lastMessage, NOW()) = 0

                long channelId = channel.getId();
                sql = sql
                        .replace("%a", "IFNULL((SELECT (getValue(%s, %u) * b.categoryEffect) FROM PowerPlantUserPowerUp a LEFT JOIN PowerPlantCategories b USING (categoryId) WHERE a.serverId = %s AND a.userId = %u AND a.categoryId = 0), 1)")
                        .replace("%s", channel.getServer().getIdAsString())
                        .replace("%c", String.valueOf(channelId))
                        .replace("%u", String.valueOf(userId))
                        .replace("%max", String.valueOf(Settings.MAX));

                totalSql.append(sql);
            }

            for (ResultSet resultSet : new DBMultipleResultSet(totalSql.toString())) {
                if (resultSet.next() && resultSet.getInt(1) == 1) {
                    long userId = resultSet.getLong(2);
                    long coins = resultSet.getLong(3);
                    ServerTextChannel channel = activities.get(userId);

                    String sql = "UPDATE PowerPlantUsers SET reminderSent = 1 WHERE serverId = ? AND userId = ?;";

                    PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
                    preparedStatement.setLong(1, channel.getServer().getId());
                    preparedStatement.setLong(2, userId);
                    preparedStatement.execute();
                    preparedStatement.close();

                    if (coins == 0 && channel.canYouWrite() && channel.canYouEmbedLinks()) {
                        Server server = channel.getServer();
                        User user = server.getMemberById(userId).orElse(null);

                        if (user != null) {
                            String prefix = DBServer.getPrefix(server);
                            Locale locale = DBServer.getServerLocale(server);

                            channel.sendMessage(new EmbedBuilder()
                                    .setColor(Color.WHITE)
                                    .setAuthor(user)
                                    .setTitle(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_title"))
                                    .setDescription(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_description").replace("%PREFIX", prefix))
                                    .setFooter(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_footer").replace("%PREFIX", prefix)));
                        }

                    }

                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void register(Server server, User user) throws Throwable {
        if (!user.isBot()) {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("INSERT IGNORE INTO PowerPlantUsers (serverId, userId) VALUES (?, ?);");
            preparedStatement.setLong(1, server.getId());
            preparedStatement.setLong(2, user.getId());
            preparedStatement.execute();
            preparedStatement.close();
        }
    }

    public static FishingProfile getFishingProfile(Server server, User user) throws Throwable {
        String sql =
                "SELECT IFNULL(joule, 0) joule, IFNULL(coins, 0) coins, categoryId, IFNULL(level, 0) lvl, categoryStartPrice, categoryPower, categoryEffect " +
                        "FROM (SELECT * FROM PowerPlantUserPowerUp WHERE serverId = %s AND userId = %u) userPowerUp " +
                        "RIGHT JOIN (PowerPlantCategories) USING (categoryId) " +
                        "LEFT JOIN (PowerPlantUsers) ON PowerPlantUsers.serverId = %s AND PowerPlantUsers.userId = %u " +
                        "ORDER BY categoryId;";

        sql = sql.replace("%s", server.getIdAsString()).replace("%u", user.getIdAsString());

        Statement statement = DBMain.getInstance().statement(sql);
        ResultSet resultSet = statement.getResultSet();

        FishingProfile fishingProfile = null;
        while(resultSet.next()) {
            if (fishingProfile == null)
                fishingProfile = new FishingProfile(server, user);

            long fish = resultSet.getLong(1);
            long coins = resultSet.getLong(2);

            if (fish > 0) fishingProfile.setFish(fish);
            if (coins > 0) fishingProfile.setCoins(coins);

            FishingSlot fishingSlot = new FishingSlot(resultSet.getInt(3), resultSet.getLong(4), resultSet.getLong(5), resultSet.getDouble(6), resultSet.getLong(7));
            fishingProfile.insert(fishingSlot);
        }

        resultSet.close();
        statement.close();

        return fishingProfile;
    }

    public static EmbedBuilder addFishingValues(Locale locale, Server server, User user, long fish, long coins) throws Throwable {
        return addFishingValues(locale, server, user, fish, coins, -1);
    }

    public static EmbedBuilder addFishingValues(Locale locale, Server server, User user, long fish, long coins, int dailyBefore) throws Throwable {
        register(server, user);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT joule, coins, growth, (rank+1), IF(%db != -1, %db, dailyStreak) FROM (%ppranks) accountData WHERE serverId = %s AND userId = %u;");

        if (fish > 0) {
            sql.append("INSERT INTO PowerPlantUserGained " +
                    "VALUES(%s, %u, DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'), %j) " +
                    "ON DUPLICATE KEY UPDATE coinsGrowth = coinsGrowth + %j;");
        }

        sql.append("INSERT INTO PowerPlantUsers (serverId, userId, joule, coins) VALUES (%s, %u, LEAST(%max, GREATEST(0, %j)), LEAST(%max, GREATEST(0, %c))) ON DUPLICATE KEY UPDATE joule = LEAST(%max, GREATEST(0,joule+%j)), coins = LEAST(%max, GREATEST(0,coins+%c));" +
                "SELECT joule, coins, growth, (rank+1), dailyStreak FROM (%ppranks) accountData WHERE serverId = %s AND userId = %u;");

        String sqlString = sql.toString().replace("%max", String.valueOf(Settings.MAX)).replace("%ppranks", DBVirtualViews.getPowerPlantUsersRanks(server)).replace("%db", String.valueOf(dailyBefore)).replace("%s", server.getIdAsString()).replace("%u", user.getIdAsString()).replace("%c", String.valueOf(coins)).replace("%j", String.valueOf(fish));

        long[][] progress = new long[5][2]; //Joule, Coins, Growth, Rang, Daily Combo
        String[] progressString = new String[6];
        EmbedBuilder eb;

        int i=0;
        for(ResultSet resultSet: new DBMultipleResultSet(sqlString)) {
            if (i == 0) {
                if (resultSet.next()) {
                    for (int j = 0; j < 5; j++) {
                        progress[j][0] = resultSet.getLong(j + 1);
                    }
                } else return null;
            } else {
                if (resultSet.next()) {
                    for (int j = 0; j < 5; j++) {
                        progress[j][1] = resultSet.getLong(j + 1);

                        String prefix = "";
                        String key = "rankingprogress_update";
                        if (j == 3) {
                            prefix = "#";
                            key = "rankingprogress_update2";
                        }
                        String sign = "";
                        if (progress[j][1] > progress[j][0]) sign = "+";
                        progressString[j] = TextManager.getString(locale, TextManager.GENERAL, key , progress[j][0] != progress[j][1],
                                prefix,
                                Tools.numToString(locale, progress[j][0]),
                                Tools.numToString(locale, progress[j][1]),
                                sign + Tools.numToString(locale, progress[j][1] - progress[j][0])
                        );
                    }
                } else return null;
            }
            resultSet.close();
            i++;
        }

        String descriptionLabel = "rankingprogress_desription";
        if (progress[1][0] == 0) descriptionLabel = "rankingprogress_desription_nocoins";

        eb = EmbedFactory.getEmbed()
                .setAuthor(TextManager.getString(locale, TextManager.GENERAL, "rankingprogress_title", user.getDisplayName(server)), "", user.getAvatar())
                .setThumbnail("http://icons.iconarchive.com/icons/webalys/kameleon.pics/128/Money-Graph-icon.png");

        progressString[5] = CodeBlockColor.WHITE;

        if (fish > 0 || (fish == 0 && coins > 0)) {
            eb.setColor(Color.GREEN);
        } else if (coins <= 0 && (fish < 0 || coins < 0)) {
            eb.setColor(Color.RED);
        }

        if (progress[3][1] > progress[3][0]) progressString[5] = CodeBlockColor.RED;
        else if (progress[3][1] < progress[3][0]) progressString[5] = CodeBlockColor.GREEN;

        eb
                .setDescription(TextManager.getString(locale, TextManager.GENERAL, descriptionLabel, progressString))
                .setThumbnail(user.getAvatar());

        return eb;
    }

    public static void updateOnServerStatus(Server server, User user, boolean onServer) throws Throwable {
        updateOnServerStatus(server, user.getId(), onServer);
    }

    public static void updateOnServerStatus(Server server, long userId, boolean onServer) throws Throwable {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("UPDATE PowerPlantUsers SET onServer = ? WHERE serverId = ? AND userId = ?;");

        preparedStatement.setBoolean(1, onServer);
        preparedStatement.setLong(2, server.getId());
        preparedStatement.setLong(3, userId);
        preparedStatement.execute();

        preparedStatement.close();
    }

    public static void updatePowerUpLevel(Server server, User user, int categoryId, int level) throws Throwable {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("REPLACE INTO PowerPlantUserPowerUp (serverId, userId, categoryId, level) VALUES  (?, ?, ?, ?);");

        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, user.getId());
        preparedStatement.setInt(3, categoryId);
        preparedStatement.setInt(4, level);
        preparedStatement.execute();

        preparedStatement.close();
    }

    public static DailyState daily(Server server, User user) throws Throwable {
        register(server, user);

        String sql = "SELECT dailyRecieved, dailyStreak FROM PowerPlantUsers WHERE serverId = ? AND userId = ?;" +
                "UPDATE PowerPlantUsers SET dailyRecieved = ? WHERE serverId = ? AND userId = ?;";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, user.getId());
        preparedStatement.setString(3, DBMain.instantToDateString(Instant.now()));
        preparedStatement.setLong(4, server.getId());
        preparedStatement.setLong(5, user.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();

        if (resultSet.next()) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(resultSet.getDate(1).getTime());
            int streak = resultSet.getInt(2);
            int streakShow = streak;
            resultSet.close();

            long daysBetween = Math.abs(ChronoUnit.DAYS.between(calendar.toInstant().atZone(calendar.getTimeZone().toZoneId()), Calendar.getInstance().toInstant().atZone(calendar.getTimeZone().toZoneId())));

            boolean streakBreak = false;
            if (daysBetween != 0) {
                if (daysBetween == 1 || streak == 0) {
                    streak++;
                    streakShow = streak;
                }
                else {
                    streak = 1;
                    streakBreak = true;
                }

                sql = "UPDATE PowerPlantUsers SET dailyStreak = ? WHERE serverId = ? AND userId = ?;";
                preparedStatement = DBMain.getInstance().preparedStatement(sql);
                preparedStatement.setInt(1, streak);
                preparedStatement.setLong(2, server.getId());
                preparedStatement.setLong(3, user.getId());
                preparedStatement.execute();
            }

            resultSet.close();
            preparedStatement.close();
            return new DailyState(streakShow, daysBetween != 0, streakBreak);
        } else {
            resultSet.close();
            preparedStatement.close();
            return null;
        }
    }

    public static void increaseUpvotesUnclaimed(long userId, int amount) throws Throwable {
        String sql = "UPDATE PowerPlantUsers a SET upvotesUnclaimed = upvotesUnclaimed + ? WHERE userId = ? AND (SELECT powerPlant FROM DServer WHERE serverId = a.serverId) = 'ACTIVE';";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setInt(1, amount);
        preparedStatement.setLong(2, userId);
        preparedStatement.execute();
        preparedStatement.close();
    }

    public static int getUpvotesUnclaimed(Server server, User user) throws Throwable {
        int amount = 0;

        String sql = "SELECT upvotesUnclaimed FROM PowerPlantUsers WHERE serverId = ? AND userId = ?;" +
                "UPDATE PowerPlantUsers SET upvotesUnclaimed = 0 WHERE serverId = ? AND userId = ?;";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, user.getId());
        preparedStatement.setLong(3, server.getId());
        preparedStatement.setLong(4, user.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            amount = resultSet.getInt(1);
        }
        resultSet.close();
        preparedStatement.close();

        return amount;
    }

    public static void addDonatorStatus(User user, int weeks) throws Throwable {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("INSERT INTO Donators VALUES(?, NOW() + INTERVAL ? WEEK) ON DUPLICATE KEY UPDATE end = end + INTERVAL ? WEEK;");

        preparedStatement.setLong(1, user.getId());
        preparedStatement.setInt(2, weeks);
        preparedStatement.setInt(3, weeks);
        preparedStatement.execute();

        preparedStatement.close();
    }

    public static ArrayList<Long> getDonationEnds() throws Throwable {
        ArrayList<Long> users = new ArrayList<>();
        String sql = "SELECT userId FROM Donators WHERE end <= NOW();";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        while (resultSet.next()) {
            users.add(resultSet.getLong(1));
        }

        resultSet.close();
        preparedStatement.close();

        return users;
    }

    public static void removeDonation(long userId) throws Throwable {
        String sql = "DELETE FROM Donators WHERE userId = ?;";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setLong(1, userId);
        preparedStatement.execute();
        preparedStatement.close();
    }

    public static boolean hasDonated(User user) throws Throwable {
        String sql = "SELECT * FROM Donators WHERE userId = ? AND end > NOW();";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setLong(1, user.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        boolean hasDonated = resultSet.next();

        resultSet.close();
        preparedStatement.close();

        return hasDonated;
    }

    public static ArrayList<Long> getActiveDonators() throws Throwable {
        ArrayList<Long> users = new ArrayList<>();
        String sql = "SELECT userId FROM Donators WHERE end > NOW();";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        while (resultSet.next()) {
            users.add(resultSet.getLong(1));
        }

        resultSet.close();
        preparedStatement.close();

        return users;
    }
}
