package mysql.modules.fisheryusers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import constants.FisheryGear;
import constants.FisheryStatus;
import core.Program;
import core.MainLogger;
import mysql.DBBatch;
import mysql.DBDataLoad;
import mysql.DBIntervalMapCache;
import mysql.DBMain;
import mysql.modules.guild.DBGuild;

public class DBFishery extends DBIntervalMapCache<Long, FisheryGuildData> {

    private static final DBFishery ourInstance = new DBFishery();

    public static DBFishery getInstance() {
        return ourInstance;
    }

    private DBFishery() {
        super(Program.isProductionMode() ? 5 : 1);
    }

    @Override
    protected CacheBuilder<Object, Object> getCacheBuilder() {
        return CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .removalListener(e -> {
                    FisheryGuildData serverBean = (FisheryGuildData) e.getValue();
                    if (e.getCause() == RemovalCause.EXPIRED && isChanged(serverBean)) {
                        removeUpdate(serverBean);
                        save(serverBean);
                    }
                });
    }

    @Override
    protected FisheryGuildData load(Long serverId) throws Exception {
        HashMap<Long, HashMap<Instant, FisheryHourlyIncomeData>> fisheryHourlyIncomeMap = getFisheryHourlyIncomeMap(serverId);
        HashMap<Long, HashMap<Integer, FisheryMemberGearData>> fisheryPowerUpMap = getFisheryPowerUpMap(serverId);

        FisheryGuildData fisheryGuildBean = new FisheryGuildData(
                serverId,
                getIgnoredChannelIds(serverId),
                getRoleIds(serverId),
                getFisheryUsers(serverId, fisheryHourlyIncomeMap, fisheryPowerUpMap)
        );

        fisheryGuildBean.getRoleIds()
                .addListAddListener(list -> list.forEach(roleId -> addRoleId(fisheryGuildBean.getGuildId(), roleId)))
                .addListRemoveListener(list -> list.forEach(roleId -> removeRoleId(fisheryGuildBean.getGuildId(), roleId)));
        fisheryGuildBean.getIgnoredChannelIds()
                .addListAddListener(list -> list.forEach(channelId -> addIgnoredChannelId(fisheryGuildBean.getGuildId(), channelId)))
                .addListRemoveListener(list -> list.forEach(channelId -> removeIgnoredChannelId(fisheryGuildBean.getGuildId(), channelId)));

        return fisheryGuildBean;
    }

    @Override
    protected synchronized void save(FisheryGuildData fisheryGuildBean) {
        try {
            if (fisheryGuildBean != null &&
                    fisheryGuildBean.getGuildBean().getFisheryStatus() != FisheryStatus.STOPPED &&
                    fisheryGuildBean.getGuildBean().isSaved()
            ) {
                DBBatch userBatch = new DBBatch("REPLACE INTO PowerPlantUsers (serverId, userId, joule, coins, dailyRecieved, dailyStreak, reminderSent, upvotesUnclaimed, dailyValuesUpdated, dailyVCMinutes, dailyReceivedCoins, nextWork) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                DBBatch hourlyBatch = new DBBatch("REPLACE INTO PowerPlantUserGained (serverId, userId, time, coinsGrowth) VALUES (?, ?, ?, ?)");
                DBBatch powerUpBatch = new DBBatch("REPLACE INTO PowerPlantUserPowerUp (serverId, userId, categoryId, level) VALUES (?, ?, ?, ?)");

                new ArrayList<>(fisheryGuildBean.getUsers().values()).stream()
                        .filter(FisheryMemberData::checkChanged)
                        .forEach(fisheryUserBean -> saveFisheryUserBean(fisheryUserBean, userBatch, hourlyBatch, powerUpBatch));

                userBatch.execute();
                hourlyBatch.execute();
                powerUpBatch.execute();

                if (Program.isRunning()) {
                    Thread.sleep(100);
                }
            }
        } catch (Throwable e) {
            update(fisheryGuildBean, null);
            MainLogger.get().error("Could not save fishery server {}", fisheryGuildBean.getGuildId(), e);
        }
    }

    private void saveFisheryUserBean(FisheryMemberData fisheryMemberBean, DBBatch userBatch, DBBatch hourlyBatch, DBBatch powerUpBatch) {
        try {
            userBatch.add(preparedStatement -> {
                preparedStatement.setLong(1, fisheryMemberBean.getGuildId());
                preparedStatement.setLong(2, fisheryMemberBean.getMemberId());
                preparedStatement.setLong(3, fisheryMemberBean.getFish());
                preparedStatement.setLong(4, fisheryMemberBean.getCoinsRaw());
                preparedStatement.setString(5, DBMain.localDateToDateString(fisheryMemberBean.getDailyReceived()));
                preparedStatement.setLong(6, fisheryMemberBean.getDailyStreak());
                preparedStatement.setBoolean(7, fisheryMemberBean.isReminderSent());
                preparedStatement.setInt(8, fisheryMemberBean.getUpvoteStack());
                preparedStatement.setString(9, DBMain.localDateToDateString(fisheryMemberBean.getDailyValuesUpdated()));
                preparedStatement.setInt(10, fisheryMemberBean.getVcMinutes());
                preparedStatement.setLong(11, fisheryMemberBean.getCoinsGiven());
                preparedStatement.setString(12, DBMain.instantToDateTimeString(fisheryMemberBean.getNextWork()));
            });

            fisheryMemberBean.getAllFishHourlyIncomeChanged().forEach(fisheryHourlyIncomeData -> saveFisheryHourlyIncomeBean(fisheryHourlyIncomeData, hourlyBatch));
            fisheryMemberBean.getGearMap().values().stream()
                    .filter(FisheryMemberGearData::checkChanged)
                    .forEach(powerUpBean -> saveFisheryUserPowerUpBean(powerUpBean, powerUpBatch));
        } catch (Throwable e) {
            fisheryMemberBean.setChanged();
            MainLogger.get().error("Could not save fishery user {} on server {}", fisheryMemberBean.getMemberId(), fisheryMemberBean.getGuildId(), e);
        }
    }

    private void saveFisheryHourlyIncomeBean(FisheryHourlyIncomeData fisheryHourlyIncomeData, DBBatch hourlyBatch) {
        try {
            hourlyBatch.add(preparedStatement -> {
                preparedStatement.setLong(1, fisheryHourlyIncomeData.getGuildId());
                preparedStatement.setLong(2, fisheryHourlyIncomeData.getMemberId());
                preparedStatement.setString(3, DBMain.instantToDateTimeString(fisheryHourlyIncomeData.getTime()));
                preparedStatement.setLong(4, fisheryHourlyIncomeData.getFishIncome());
            });
        } catch (Throwable e) {
            fisheryHourlyIncomeData.setChanged();
            MainLogger.get().error("Could not save fishery hourly income", e);
        }
    }

    private void saveFisheryUserPowerUpBean(FisheryMemberGearData fisheryMemberGearData, DBBatch powerUpBatch) {
        try {
            powerUpBatch.add(preparedStatement -> {
                preparedStatement.setLong(1, fisheryMemberGearData.getGuildId());
                preparedStatement.setLong(2, fisheryMemberGearData.getMemberId());
                preparedStatement.setInt(3, fisheryMemberGearData.getGear().ordinal());
                preparedStatement.setInt(4, fisheryMemberGearData.getLevel());
            });
        } catch (Throwable e) {
            fisheryMemberGearData.setChanged();
            MainLogger.get().error("Could not save fishery power up bean", e);
        }
    }

    protected void removeFisheryUserBean(FisheryMemberData fisheryMemberBean) {
        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUsers WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, fisheryMemberBean.getGuildId());
            preparedStatement.setLong(2, fisheryMemberBean.getMemberId());
        });

        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUserPowerUp WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, fisheryMemberBean.getGuildId());
            preparedStatement.setLong(2, fisheryMemberBean.getMemberId());
        });

        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUserGained WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, fisheryMemberBean.getGuildId());
            preparedStatement.setLong(2, fisheryMemberBean.getMemberId());
        });
    }

    private HashMap<Long, FisheryMemberData> getFisheryUsers(long serverId, HashMap<Long, HashMap<Instant, FisheryHourlyIncomeData>> fisheryHourlyIncomeMap, HashMap<Long, HashMap<Integer, FisheryMemberGearData>> fisheryPowerUpMap) throws SQLException, ExecutionException {
        HashMap<Long, FisheryMemberData> usersMap = new HashMap<>();

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT userId, joule, coins, dailyRecieved, dailyStreak, reminderSent, upvotesUnclaimed, dailyValuesUpdated, dailyVCMinutes, dailyReceivedCoins, nextWork FROM PowerPlantUsers WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        while (resultSet.next()) {
            long userId = resultSet.getLong(1);
            FisheryMemberData fisheryMemberBean = new FisheryMemberData(
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
                    resultSet.getTimestamp(11).toInstant(),
                    fisheryHourlyIncomeMap.getOrDefault(userId, new HashMap<>()),
                    fisheryPowerUpMap.getOrDefault(userId, new HashMap<>())
            );
            usersMap.put(userId, fisheryMemberBean);
        }

        resultSet.close();
        preparedStatement.close();

        return usersMap;
    }

    private HashMap<Long, HashMap<Instant, FisheryHourlyIncomeData>> getFisheryHourlyIncomeMap(long serverId) throws SQLException {
        HashMap<Long, HashMap<Instant, FisheryHourlyIncomeData>> hourlyIncomeMap = new HashMap<>();

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT userId, time, coinsGrowth FROM PowerPlantUserGained WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        while (resultSet.next()) {
            long userId = resultSet.getLong(1);
            Instant time = resultSet.getTimestamp(2).toInstant();

            FisheryHourlyIncomeData fisheryUserBean = new FisheryHourlyIncomeData(
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

    private HashMap<Long, HashMap<Integer, FisheryMemberGearData>> getFisheryPowerUpMap(long serverId) throws SQLException {
        HashMap<Long, HashMap<Integer, FisheryMemberGearData>> powerUpMap = new HashMap<>();

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT userId, categoryId, level FROM PowerPlantUserPowerUp WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        while (resultSet.next()) {
            long userId = resultSet.getLong(1);
            int powerUpId = resultSet.getInt(2);
            FisheryMemberGearData fisheryMemberGearData = new FisheryMemberGearData(
                    serverId,
                    userId,
                    FisheryGear.values()[powerUpId],
                    resultSet.getInt(3)
            );

            powerUpMap.computeIfAbsent(userId, k -> new HashMap<>()).put(powerUpId, fisheryMemberGearData);
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
        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUserGained WHERE TIMESTAMPDIFF(HOUR, time, NOW()) > 168;");
    }

    public void removePowerPlant(long serverId) {
        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUserPowerUp WHERE serverId = ?;", preparedStatement -> preparedStatement.setLong(1, serverId));
        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUsers WHERE serverId = ?;", preparedStatement -> preparedStatement.setLong(1, serverId));
        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUserGained WHERE serverId = ?;", preparedStatement -> preparedStatement.setLong(1, serverId));

        DBGuild.getInstance().retrieve(serverId).setFisheryStatus(FisheryStatus.STOPPED);
        getCache().invalidate(serverId);
        removeUpdateIf(fisheryServerBean -> fisheryServerBean.getGuildId() == serverId);
    }

}
