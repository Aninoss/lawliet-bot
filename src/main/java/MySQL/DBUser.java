package MySQL;

import Constants.CodeBlockColor;
import Constants.FishingCategoryInterface;
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
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DBUser {
    public static void synchronize(DiscordApi api) throws SQLException {
        if (!Bot.isDebug()) {
            System.out.println("User werden synchronisiert...");

            Thread t = new Thread(() -> {
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
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            t.setName("synchr_user");
            t.start();
        }
    }

    public static void insertUsers(Collection<User> users) throws SQLException {
        insertUsers(new ArrayList<>(users));
    }

    public static void insertUsers(ArrayList<User> users) throws SQLException {
        if (users.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (User user : users) {
                sb.append("INSERT IGNORE INTO DUser (userId) VALUES (").append(user.getIdAsString()).append("); ");
            }

            Statement statement = DBMain.getInstance().statement(sb.toString());
            statement.close();
        }
    }

    public static void insertUserIds(ArrayList<Long> userIds) throws SQLException {
        if (userIds.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (long id: userIds) {
                sb.append("INSERT IGNORE INTO DUser (userId) VALUES (").append(id).append("); ");
            }

            Statement statement = DBMain.getInstance().statement(sb.toString());
            statement.close();
        }
    }

    public static void insertUser(User user) throws SQLException {
        ArrayList<User> users = new ArrayList<>();
        users.add(user);
        insertUsers(users);
    }

    /*public static void addMessageFishBulk(Map<Long, Map<Long, ActivityUserData>> activities) throws SQLException {
        StringBuilder sql = new StringBuilder("DROP TABLE IF EXISTS Activities;\n" +
                "CREATE TEMPORARY TABLE `Activities` (\n" +
                " `serverId` bigint(20) unsigned NOT NULL,\n" +
                " `userId` bigint(20) unsigned NOT NULL,\n" +
                " `add` bigint(20) NOT NULL,\n" +
                " PRIMARY KEY (`serverId`,`userId`)\n" +
                ") ENGINE=InnoDB;\n\n");

        for (long serverId : activities.keySet()) {
            for (long userId : activities.get(serverId).keySet()) {
                ActivityUserData activityUserData = activities.get(serverId).get(userId);

                sql.append(("INSERT IGNORE INTO DUser (userId) VALUES (%user);\n" +
                            "INSERT IGNORE INTO PowerPlantUserGained (serverId, userId, time) VALUES (%server, %user, DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'));\n" +
                            "INSERT IGNORE INTO PowerPlantUsers (serverId, userId) VALUES (%server, %user);\n"
                                ).replace("%add", "(SELECT (getValueForCategory(%category, %server, %user) * categoryEffect * %multi) FROM PowerPlantCategories WHERE categoryId = %category)")
                                .replace("%server", String.valueOf(serverId))
                                .replace("%user", String.valueOf(userId)));

                if (activityUserData.getAmountMessage() > 0) {
                    sql.append("INSERT INTO Activities VALUES (%server, %user, %add);\n"
                            .replace("%add", "(SELECT (getValueForCategory(%category, %server, %user) * categoryEffect * %multi) FROM PowerPlantCategories WHERE categoryId = %category)")
                            .replace("%server", String.valueOf(serverId))
                            .replace("%user", String.valueOf(userId))
                            .replace("%multi", String.valueOf(activityUserData.getAmountMessage()))
                            .replace("%category", String.valueOf(FishingCategoryInterface.PER_MESSAGE))
                    );
                }

                if (activityUserData.getAmountVC() > 0) {
                    sql.append(("INSERT INTO Activities VALUES (%server, %user, %add)\n" +
                                "ON DUPLICATE KEY UPDATE `add` = `add` + %add;\n"
                            ).replace("%add", "(SELECT (getValueForCategory(%category, %server, %user) * categoryEffect * %multi) FROM PowerPlantCategories WHERE categoryId = %category)")
                            .replace("%server", String.valueOf(serverId))
                            .replace("%user", String.valueOf(userId))
                            .replace("%multi", String.valueOf(activityUserData.getAmountVC()))
                            .replace("%category", String.valueOf(FishingCategoryInterface.PER_VC))
                    );
                }

                *//*sql.append(("DO SLEEP(0.5);" +
                        "SET @add = (SELECT `add` FROM Activities WHERE (serverId, userId) = (%server, %user));\n" +
                        "INSERT INTO PowerPlantUserGained (serverId, userId, time, coinsGrowth) VALUES (%server, %user, DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'), @add) ON DUPLICATE KEY UPDATE coinsGrowth = coinsGrowth + @add;\n" +
                        "INSERT INTO PowerPlantUsers (serverId, userId, joule) VALUES (%server, %user, @add) ON DUPLICATE KEY UPDATE joule = joule + @add, onServer = 1;\n")
                                .replace("%server", String.valueOf(serverId))
                                .replace("%user", String.valueOf(userId))
                );*//*
            }
        }

        sql.append("UPDATE LOW_PRIORITY PowerPlantUserGained INNER JOIN Activities USING(serverId, userId) SET coinsGrowth = LEAST(%max, coinsGrowth + Activities.add) WHERE time = DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00');\n");
        sql.append("UPDATE LOW_PRIORITY PowerPlantUsers INNER JOIN Activities USING(serverId, userId) SET joule = LEAST(%max, joule + Activities.add), onServer = 1;\n");
        sql.append("SELECT PowerPlantUsers.serverId, PowerPlantUsers.userId, coins FROM PowerPlantUsers INNER JOIN Activities USING(serverId, userId) WHERE joule >= 100 AND reminderSent = 0;");

        String sqlString = sql.toString()
                .replace("%max", String.valueOf(Settings.MAX));

        for(ResultSet resultSet : new DBMultipleResultSet(sqlString)) {
            while (resultSet.next()) {
                try {
                    long serverId = resultSet.getLong(1);
                    long userId = resultSet.getLong(2);
                    long coins = resultSet.getLong(3);
                    ActivityUserData activityUserData = activities.get(serverId).get(userId);

                    if (activityUserData.getChannel().isPresent()) {
                        ServerTextChannel channel = activityUserData.getChannel().get();

                        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("UPDATE PowerPlantUsers SET reminderSent = 1 WHERE serverId = ? AND userId = ?;");
                        preparedStatement.setLong(1, serverId);
                        preparedStatement.setLong(2, userId);
                        preparedStatement.execute();
                        preparedStatement.close();

                        if (coins == 0 && channel.canYouWrite() && channel.canYouEmbedLinks()) {
                            Server server = channel.getServer();
                            User user = server.getMemberById(userId).orElse(null);

                            if (user != null) {
                                String prefix = DBServer.getPrefix(server);
                                Locale locale = DBServer.getServerLocale(server);

                                channel.sendMessage(EmbedFactory.getEmbed()
                                        .setAuthor(user)
                                        .setTitle(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_title"))
                                        .setDescription(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_description").replace("%PREFIX", prefix))
                                        .setFooter(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_footer").replace("%PREFIX", prefix)));
                            }

                        }
                    }
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }*/

    public static void addMessageSingle(long serverId, long userId, ActivityUserData activityUserData) throws SQLException {
        StringBuilder sql = new StringBuilder("INSERT IGNORE INTO DUser (userId) VALUES (%user);\n");

        if (activityUserData.getAmountMessage() > 0) sql.append("SET @add = (SELECT (getValueForCategory(%category0, %server, %user) * categoryEffect * %multi0) FROM PowerPlantCategories WHERE categoryId = %category0);\n");
        if (activityUserData.getAmountVC() > 0) sql.append("SET @add = (SELECT (getValueForCategory(%category1, %server, %user) * categoryEffect * %multi1) FROM PowerPlantCategories WHERE categoryId = %category1);\n");

        sql.append(
                "INSERT INTO PowerPlantUserGained (serverId, userId, time, coinsGrowth) VALUES (%server, %user, DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'), @add) ON DUPLICATE KEY UPDATE coinsGrowth = coinsGrowth + @add;\n" +
                "INSERT INTO PowerPlantUsers (serverId, userId, joule) VALUES (%server, %user, @add) ON DUPLICATE KEY UPDATE joule = joule + @add, onServer = 1;\n"
        );

        if (activityUserData.getChannel().isPresent()) sql.append("SELECT COUNT(*) FROM PowerPlantUsers WHERE joule >= 100 AND reminderSent = 0 AND (serverId, userId) = (%server, %user);\n");

        String sqlString = sql.toString().replace("%server", String.valueOf(serverId))
                        .replace("%user", String.valueOf(userId))
                        .replace("%max", String.valueOf(Settings.MAX))
                        .replace("%category0", String.valueOf(FishingCategoryInterface.PER_MESSAGE))
                        .replace("%multi0", String.valueOf(activityUserData.getAmountMessage()))
                        .replace("%category1", String.valueOf(FishingCategoryInterface.PER_VC))
                        .replace("%multi1", String.valueOf(activityUserData.getAmountVC()));

        if (activityUserData.getChannel().isPresent()) {
            for (ResultSet resultSet : new DBMultipleResultSet(sqlString)) {
                try {
                    if (resultSet.next() && resultSet.getInt(1) > 0) {
                        ServerTextChannel channel = activityUserData.getChannel().get();

                        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("UPDATE PowerPlantUsers SET reminderSent = 1 WHERE serverId = ? AND userId = ?;");
                        preparedStatement.setLong(1, serverId);
                        preparedStatement.setLong(2, userId);
                        preparedStatement.execute();
                        preparedStatement.close();

                        if (channel.canYouWrite() && channel.canYouEmbedLinks()) {
                            Server server = channel.getServer();
                            User user = server.getMemberById(userId).orElse(null);

                            String prefix = DBServer.getPrefix(serverId);
                            Locale locale = DBServer.getServerLocale(serverId);

                            channel.sendMessage(EmbedFactory.getEmbed()
                                    .setAuthor(user)
                                    .setTitle(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_title"))
                                    .setDescription(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_description").replace("%PREFIX", prefix))
                                    .setFooter(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_footer").replace("%PREFIX", prefix)));

                        }
                    }
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Statement statement = DBMain.getInstance().statement(sqlString);
            statement.close();
        }
    }

    public static void register(Server server, User user) throws SQLException {
        if (!user.isBot()) {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("INSERT IGNORE INTO PowerPlantUsers (serverId, userId) VALUES (?, ?);");
            preparedStatement.setLong(1, server.getId());
            preparedStatement.setLong(2, user.getId());
            preparedStatement.execute();
            preparedStatement.close();
        }
    }

    public static FishingProfile getFishingProfile(Server server, User user) throws SQLException {
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

    public static EmbedBuilder addFishingValues(Locale locale, Server server, User user, long fish, long coins) throws SQLException, IOException {
        return addFishingValues(locale, server, user, fish, coins, -1);
    }

    public static EmbedBuilder addFishingValues(Locale locale, Server server, User user, long fish, long coins, int dailyBefore) throws SQLException, IOException {
        register(server, user);
        boolean change = fish != 0 || coins != 0;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT HIGH_PRIORITY joule, coins, getGrowth(%s, %u), getRank(%s, %u), IF(%db != -1, %db, dailyStreak) FROM (SELECT * FROM PowerPlantUsers WHERE serverId = %s and userId = %u) t;");

        if (change) {
            if (fish > 0) {
                sql.append("INSERT INTO PowerPlantUserGained " +
                        "VALUES(%s, %u, DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'), %j) " +
                        "ON DUPLICATE KEY UPDATE coinsGrowth = coinsGrowth + %j;");
            }

            sql.append("INSERT INTO PowerPlantUsers (serverId, userId, joule, coins) VALUES (%s, %u, LEAST(%max, GREATEST(0, %j)), LEAST(%max, GREATEST(0, %c))) ON DUPLICATE KEY UPDATE joule = LEAST(%max, GREATEST(0,joule+%j)), coins = LEAST(%max, GREATEST(0,coins+%c));" +
                    "SELECT joule, coins, getGrowth(%s, %u), getRank(%s, %u), dailyStreak FROM (SELECT * FROM PowerPlantUsers WHERE serverId = %s and userId = %u) t;");
        }


        String sqlString = sql.toString().
                replace("%max", String.valueOf(Settings.MAX)).
                replace("%db", String.valueOf(dailyBefore)).
                replace("%s", server.getIdAsString()).
                replace("%u", user.getIdAsString()).
                replace("%c", String.valueOf(coins)).
                replace("%j", String.valueOf(fish));


        long[][] progress = new long[5][2]; //Joule, Coins, Growth, Rang, Daily Combo
        String[] progressString = new String[6];
        EmbedBuilder eb;

        int i=0;
        for(ResultSet resultSet: new DBMultipleResultSet(sqlString)) {
            if (i == 0) {
                if (resultSet.next()) {
                    for (int j = 0; j < 5; j++) {
                        progress[j][0] = resultSet.getLong(j + 1);
                        if (!change) {
                            progress[j][1] = progress[j][0];

                            String key = "rankingprogress_update";
                            if (j == 3) key = "rankingprogress_update2";

                            progressString[j] = TextManager.getString(locale, TextManager.GENERAL, key , progress[j][0] != progress[j][1],
                                    Tools.numToString(locale, progress[j][0])
                            );
                        }
                    }
                } else return null;
            } else {
                if (resultSet.next()) {
                    for (int j = 0; j < 5; j++) {
                        progress[j][1] = resultSet.getLong(j + 1);

                        String key = "rankingprogress_update";
                        if (j == 3) key = "rankingprogress_update2";

                        String sign = "";
                        if (progress[j][1] > progress[j][0]) sign = "+";
                        progressString[j] = TextManager.getString(locale, TextManager.GENERAL, key , progress[j][0] != progress[j][1],
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

    public static void updateOnServerStatus(Server server, User user, boolean onServer) throws SQLException {
        updateOnServerStatus(server, user.getId(), onServer);
    }

    public static void updateOnServerStatus(Server server, long userId, boolean onServer) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("UPDATE PowerPlantUsers SET onServer = ? WHERE serverId = ? AND userId = ?;");

        preparedStatement.setBoolean(1, onServer);
        preparedStatement.setLong(2, server.getId());
        preparedStatement.setLong(3, userId);
        preparedStatement.execute();

        preparedStatement.close();
    }

    public static void updatePowerUpLevel(Server server, User user, int categoryId, int level) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("REPLACE INTO PowerPlantUserPowerUp (serverId, userId, categoryId, level) VALUES  (?, ?, ?, ?);");

        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, user.getId());
        preparedStatement.setInt(3, categoryId);
        preparedStatement.setInt(4, level);
        preparedStatement.execute();

        preparedStatement.close();
    }

    public static DailyState daily(Server server, User user) throws SQLException {
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

    public static void increaseUpvotesUnclaimed(long userId, int amount) throws SQLException {
        String sql = "UPDATE PowerPlantUsers a SET upvotesUnclaimed = upvotesUnclaimed + ? WHERE userId = ? AND (SELECT powerPlant FROM DServer WHERE serverId = a.serverId) = 'ACTIVE';" +
                "INSERT INTO Upvotes (userId) VALUES (?) ON DUPLICATE KEY UPDATE lastDate = NOW();";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setInt(1, amount);
        preparedStatement.setLong(2, userId);
        preparedStatement.setLong(3, userId);
        preparedStatement.execute();
        preparedStatement.close();
    }

    public static int getUpvotesUnclaimed(Server server, User user) throws SQLException {
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

    public static Instant getNextUpvote(User user) throws SQLException {
        Instant instant = Instant.now();

        String sql = "SELECT DATE_ADD(lastDate, INTERVAL 12 HOUR) FROM Upvotes WHERE userId = ?;";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setLong(1, user.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            instant = resultSet.getTimestamp(1).toInstant();
        }
        resultSet.close();
        preparedStatement.close();

        return instant;
    }

    public static void addDonatorStatus(User user, int weeks) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("INSERT INTO Donators VALUES(?, NOW() + INTERVAL ? WEEK) ON DUPLICATE KEY UPDATE end = end + INTERVAL ? WEEK;");

        preparedStatement.setLong(1, user.getId());
        preparedStatement.setInt(2, weeks);
        preparedStatement.setInt(3, weeks);
        preparedStatement.execute();

        preparedStatement.close();
    }

    public static ArrayList<Long> getDonationEnds() throws SQLException {
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

    public static void removeDonation(long userId) throws SQLException {
        String sql = "DELETE FROM Donators WHERE userId = ?;";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setLong(1, userId);
        preparedStatement.execute();
        preparedStatement.close();
    }

    public static boolean hasDonated(User user) throws SQLException {
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

    public static ArrayList<Long> getActiveDonators() throws SQLException {
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

    public static boolean registerGiveaway(Server server, User user) throws SQLException {
        boolean quit = false;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT * FROM Giveaway WHERE serverId = ? AND userId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, user.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) quit = true;

        resultSet.close();
        preparedStatement.close();

        if (quit) return false;

        preparedStatement = DBMain.getInstance().preparedStatement("INSERT IGNORE INTO Giveaway (serverId, userId) VALUES (?, ?);");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, user.getId());
        preparedStatement.execute();

        return true;
    }

}
