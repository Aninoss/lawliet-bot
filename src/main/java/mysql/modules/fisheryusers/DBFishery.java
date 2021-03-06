package mysql.modules.fisheryusers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import constants.FisheryStatus;
import core.Bot;
import core.MainLogger;
import mysql.*;
import mysql.interfaces.IntervalSave;
import mysql.modules.server.DBServer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class DBFishery extends DBIntervalMapCache<Long, FisheryServerBean> implements IntervalSave {

    private static final DBFishery ourInstance = new DBFishery();

    public static DBFishery getInstance() {
        return ourInstance;
    }

    private DBFishery() {
    }

    @Override
    protected CacheBuilder<Object, Object> getCacheBuilder() {
        return CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .removalListener(e -> {
                    FisheryServerBean serverBean = (FisheryServerBean) e.getValue();
                    if (e.getCause() == RemovalCause.EXPIRED && isChanged(serverBean)) {
                        removeUpdate(serverBean);
                        save(serverBean);
                    }
                });
    }

    @Override
    protected FisheryServerBean load(Long serverId) throws Exception {
        HashMap<Long, HashMap<Instant, FisheryHourlyIncomeBean>> fisheryHourlyIncomeMap = getFisheryHourlyIncomeMap(serverId);
        HashMap<Long, HashMap<Integer, FisheryUserPowerUpBean>> fisheryPowerUpMap = getFisheryPowerUpMap(serverId);

        FisheryServerBean fisheryServerBean = new FisheryServerBean(
                serverId,
                getIgnoredChannelIds(serverId),
                getRoleIds(serverId),
                getFisheryUsers(serverId, fisheryHourlyIncomeMap, fisheryPowerUpMap)
        );

        fisheryServerBean.getRoleIds()
                .addListAddListener(list -> list.forEach(roleId -> addRoleId(fisheryServerBean.getGuildId(), roleId)))
                .addListRemoveListener(list -> list.forEach(roleId -> removeRoleId(fisheryServerBean.getGuildId(), roleId)));
        fisheryServerBean.getIgnoredChannelIds()
                .addListAddListener(list -> list.forEach(channelId -> addIgnoredChannelId(fisheryServerBean.getGuildId(), channelId)))
                .addListRemoveListener(list -> list.forEach(channelId -> removeIgnoredChannelId(fisheryServerBean.getGuildId(), channelId)));

        return fisheryServerBean;
    }

    @Override
    protected synchronized void save(FisheryServerBean fisheryServerBean) {
        try {
            if (fisheryServerBean.getGuildBean().getFisheryStatus() != FisheryStatus.STOPPED && fisheryServerBean.getGuildBean().isSaved()) {
                DBBatch userBatch = new DBBatch("REPLACE INTO PowerPlantUsers (serverId, userId, joule, coins, dailyRecieved, dailyStreak, reminderSent, upvotesUnclaimed, dailyValuesUpdated, dailyVCMinutes, dailyReceivedCoins) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                DBBatch hourlyBatch = new DBBatch("REPLACE INTO PowerPlantUserGained (serverId, userId, time, coinsGrowth) VALUES (?, ?, ?, ?)");
                DBBatch powerUpBatch = new DBBatch("REPLACE INTO PowerPlantUserPowerUp (serverId, userId, categoryId, level) VALUES (?, ?, ?, ?)");

                new ArrayList<>(fisheryServerBean.getUsers().values()).stream()
                        .filter(FisheryUserBean::checkChanged)
                        .forEach(fisheryUserBean -> saveFisheryUserBean(fisheryUserBean, userBatch, hourlyBatch, powerUpBatch));

                userBatch.execute();
                hourlyBatch.execute();
                powerUpBatch.execute();

                if (Bot.isRunning())
                    Thread.sleep(100);
            }
        } catch (Throwable e) {
            update(fisheryServerBean, null);
            MainLogger.get().error("Could not save fishery server {}", fisheryServerBean.getGuildId(), e);
        }
    }

    private void saveFisheryUserBean(FisheryUserBean fisheryUserBean, DBBatch userBatch, DBBatch hourlyBatch, DBBatch powerUpBatch) {
        try {
            userBatch.add(preparedStatement -> {
                preparedStatement.setLong(1, fisheryUserBean.getGuildId());
                preparedStatement.setLong(2, fisheryUserBean.getUserId());
                preparedStatement.setLong(3, fisheryUserBean.getFish());
                preparedStatement.setLong(4, fisheryUserBean.getCoinsRaw());
                preparedStatement.setString(5, DBMain.localDateToDateString(fisheryUserBean.getDailyReceived()));
                preparedStatement.setLong(6, fisheryUserBean.getDailyStreak());
                preparedStatement.setBoolean(7, fisheryUserBean.isReminderSent());
                preparedStatement.setInt(8, fisheryUserBean.getUpvoteStack());
                preparedStatement.setString(9, DBMain.localDateToDateString(fisheryUserBean.getDailyValuesUpdated()));
                preparedStatement.setInt(10, fisheryUserBean.getVcMinutes());
                preparedStatement.setLong(11, fisheryUserBean.getCoinsGiven());
            });

            fisheryUserBean.getAllFishHourlyIncomeChanged().forEach(fisheryHourlyIncomeBean -> saveFisheryHourlyIncomeBean(fisheryHourlyIncomeBean, hourlyBatch));
            fisheryUserBean.getPowerUpMap().values().stream()
                    .filter(FisheryUserPowerUpBean::checkChanged)
                    .forEach(powerUpBean -> saveFisheryUserPowerUpBean(powerUpBean, powerUpBatch));
        } catch (Throwable e) {
            fisheryUserBean.setChanged();
            MainLogger.get().error("Could not save fishery user {} on server {}", fisheryUserBean.getUserId(), fisheryUserBean.getGuildId(), e);
        }
    }

    private void saveFisheryHourlyIncomeBean(FisheryHourlyIncomeBean fisheryHourlyIncomeBean, DBBatch hourlyBatch) {
        try {
            hourlyBatch.add(preparedStatement -> {
                preparedStatement.setLong(1, fisheryHourlyIncomeBean.getServerId());
                preparedStatement.setLong(2, fisheryHourlyIncomeBean.getUserId());
                preparedStatement.setString(3, DBMain.instantToDateTimeString(fisheryHourlyIncomeBean.getTime()));
                preparedStatement.setLong(4, fisheryHourlyIncomeBean.getFishIncome());
            });
        } catch (Throwable e) {
            fisheryHourlyIncomeBean.setChanged();
            MainLogger.get().error("Could not save fishery hourly income", e);
        }
    }

    private void saveFisheryUserPowerUpBean(FisheryUserPowerUpBean fisheryUserPowerUpBean, DBBatch powerUpBatch) {
        try {
            powerUpBatch.add(preparedStatement -> {
                preparedStatement.setLong(1, fisheryUserPowerUpBean.getServerId());
                preparedStatement.setLong(2, fisheryUserPowerUpBean.getUserId());
                preparedStatement.setInt(3, fisheryUserPowerUpBean.getPowerUpId());
                preparedStatement.setInt(4, fisheryUserPowerUpBean.getLevel());
            });
        } catch (Throwable e) {
            fisheryUserPowerUpBean.setChanged();
            MainLogger.get().error("Could not save fishery power up bean", e);
        }
    }

    protected void removeFisheryUserBean(FisheryUserBean fisheryUserBean) {
        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUsers WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, fisheryUserBean.getGuildId());
            preparedStatement.setLong(2, fisheryUserBean.getUserId());
        });

        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUserPowerUp WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, fisheryUserBean.getGuildId());
            preparedStatement.setLong(2, fisheryUserBean.getUserId());
        });

        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUserGained WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, fisheryUserBean.getGuildId());
            preparedStatement.setLong(2, fisheryUserBean.getUserId());
        });
    }

    private HashMap<Long, FisheryUserBean> getFisheryUsers(long serverId, HashMap<Long, HashMap<Instant, FisheryHourlyIncomeBean>> fisheryHourlyIncomeMap, HashMap<Long, HashMap<Integer, FisheryUserPowerUpBean>> fisheryPowerUpMap) throws SQLException, ExecutionException {
        HashMap<Long, FisheryUserBean> usersMap = new HashMap<>();

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT userId, joule, coins, dailyRecieved, dailyStreak, reminderSent, upvotesUnclaimed, dailyValuesUpdated, dailyVCMinutes, dailyReceivedCoins FROM PowerPlantUsers WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        while (resultSet.next()) {
            long userId = resultSet.getLong(1);
            FisheryUserBean fisheryUserBean = new FisheryUserBean(
                    serverId,
                    userId,
                    resultSet.getLong(2),
                    resultSet.getLong(3),
                    resultSet.getDate(4).toLocalDate(),
                    resultSet.getLong(5),
                    resultSet.getBoolean(6),
                    resultSet.getInt(7),
                    resultSet.getDate(8).toLocalDate(),
                    resultSet.getInt(9),
                    resultSet.getLong(10),
                    fisheryHourlyIncomeMap.getOrDefault(userId, new HashMap<>()),
                    fisheryPowerUpMap.getOrDefault(userId, new HashMap<>())
            );
            usersMap.put(userId, fisheryUserBean);
        }

        resultSet.close();
        preparedStatement.close();

        return usersMap;
    }

    private HashMap<Long, HashMap<Instant, FisheryHourlyIncomeBean>> getFisheryHourlyIncomeMap(long serverId) throws SQLException {
        HashMap<Long, HashMap<Instant, FisheryHourlyIncomeBean>> hourlyIncomeMap = new HashMap<>();

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT userId, time, coinsGrowth FROM PowerPlantUserGained WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        while (resultSet.next()) {
            long userId = resultSet.getLong(1);
            Instant time = resultSet.getTimestamp(2).toInstant();

            FisheryHourlyIncomeBean fisheryUserBean = new FisheryHourlyIncomeBean(
                    serverId,
                    userId,
                    time,
                    resultSet.getLong(3)
            );

            hourlyIncomeMap.computeIfAbsent(userId, k -> new HashMap<>()).put(time, fisheryUserBean);
        }

        resultSet.close();
        preparedStatement.close();

        return hourlyIncomeMap;
    }

    private HashMap<Long, HashMap<Integer, FisheryUserPowerUpBean>> getFisheryPowerUpMap(long serverId) throws SQLException {
        HashMap<Long, HashMap<Integer, FisheryUserPowerUpBean>> powerUpMap = new HashMap<>();

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT userId, categoryId, level FROM PowerPlantUserPowerUp WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        while (resultSet.next()) {
            long userId = resultSet.getLong(1);
            int powerUpId = resultSet.getInt(2);
            FisheryUserPowerUpBean fisheryUserPowerUpBean = new FisheryUserPowerUpBean(
                    serverId,
                    userId,
                    powerUpId,
                    resultSet.getInt(3)
            );

            powerUpMap.computeIfAbsent(userId, k -> new HashMap<>()).put(powerUpId, fisheryUserPowerUpBean);
        }

        resultSet.close();
        preparedStatement.close();

        return powerUpMap;
    }

    private ArrayList<Long> getRoleIds(long serverId) {
        return new DBDataLoad<Long>("PowerPlantRoles", "roleId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addRoleId(long serverId, long roleId) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO PowerPlantRoles (serverId, roleId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, roleId);
        });
    }

    private void removeRoleId(long serverId, long roleId) {
        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantRoles WHERE serverId = ? AND roleId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, roleId);
        });
    }

    private ArrayList<Long> getIgnoredChannelIds(long serverId) {
        return new DBDataLoad<Long>("PowerPlantIgnoredChannels", "channelId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addIgnoredChannelId(long serverId, long channelId) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO PowerPlantIgnoredChannels (serverId, channelId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, channelId);
        });
    }

    private void removeIgnoredChannelId(long serverId, long channelId) {
        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantIgnoredChannels WHERE serverId = ? AND channelId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, channelId);
        });
    }

    public void cleanUp() {
        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUserGained WHERE TIMESTAMPDIFF(HOUR, time, NOW()) > 168;", preparedStatement -> {
        });
    }

    public void removePowerPlant(long serverId) {
        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUserPowerUp WHERE serverId = ?;", preparedStatement -> preparedStatement.setLong(1, serverId));
        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUsers WHERE serverId = ?;", preparedStatement -> preparedStatement.setLong(1, serverId));
        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUserGained WHERE serverId = ?;", preparedStatement -> preparedStatement.setLong(1, serverId));

        DBServer.getInstance().retrieve(serverId).setFisheryStatus(FisheryStatus.STOPPED);
        getCache().invalidate(serverId);
        removeUpdateIf(fisheryServerBean -> fisheryServerBean.getGuildId() == serverId);
    }

    @Override
    public int getIntervalMinutes() {
        return Bot.isProductionMode() ? 10 : 1;
    }

}
