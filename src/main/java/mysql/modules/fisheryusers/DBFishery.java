package mysql.modules.fisheryusers;

import com.google.common.cache.CacheBuilder;
import constants.FisheryStatus;
import core.Bot;
import core.CustomThread;
import core.DiscordApiCollection;
import core.IntervalBlock;
import mysql.DBBeanGenerator;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.interfaces.IntervalSave;
import mysql.modules.bannedusers.DBBannedUsers;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class DBFishery extends DBBeanGenerator<Long, FisheryServerBean> implements IntervalSave {

    private static final DBFishery ourInstance = new DBFishery();

    public static DBFishery getInstance() {
        return ourInstance;
    }

    private DBFishery() {
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(DBFishery.class);
    private final int VC_CHECK_INTERVAL_MIN = 1;

    private boolean vcObserverStarted = false;

    @Override
    protected CacheBuilder<Object, Object> getCacheBuilder() {
        return CacheBuilder.newBuilder()
                //.maximumSize(250 * DiscordApiCollection.getInstance().size())
                .removalListener((element) -> onRemoved((FisheryServerBean) element.getValue()));
    }

    private void onRemoved(FisheryServerBean fisheryServerBean) {
        if (isChanged(fisheryServerBean)) {
            LOGGER.warn("Fishery cache overflow (size: {})", getCache().size());
            removeUpdate(fisheryServerBean);
            saveBean(fisheryServerBean);
        }
    }

    @Override
    protected FisheryServerBean loadBean(Long serverId) throws Exception {
        ServerBean serverBean = DBServer.getInstance().getBean(serverId);

        HashMap<Long, HashMap<Instant, FisheryHourlyIncomeBean>> fisheryHourlyIncomeMap = getFisheryHourlyIncomeMap(serverId);
        HashMap<Long, HashMap<Integer, FisheryUserPowerUpBean>> fisheryPowerUpMap = getFisheryPowerUpMap(serverId);

        FisheryServerBean fisheryServerBean = new FisheryServerBean(
                serverBean,
                getIgnoredChannelIds(serverId),
                getRoleIds(serverId),
                getFisheryUsers(serverId, serverBean, fisheryHourlyIncomeMap, fisheryPowerUpMap)
        );

        fisheryServerBean.getRoleIds()
                .addListAddListener(list -> list.forEach(roleId -> addRoleId(fisheryServerBean.getServerId(), roleId)))
                .addListRemoveListener(list -> list.forEach(roleId -> removeRoleId(fisheryServerBean.getServerId(), roleId)));
        fisheryServerBean.getIgnoredChannelIds()
                .addListAddListener(list -> list.forEach(channelId -> addIgnoredChannelId(fisheryServerBean.getServerId(), channelId)))
                .addListRemoveListener(list -> list.forEach(channelId -> removeIgnoredChannelId(fisheryServerBean.getServerId(), channelId)));

        return fisheryServerBean;
    }

    @Override
    protected synchronized void saveBean(FisheryServerBean fisheryServerBean) {
        try {
            if (fisheryServerBean.getServerBean().getFisheryStatus() != FisheryStatus.STOPPED && fisheryServerBean.getServerBean().isCached()) {
                new ArrayList<>(fisheryServerBean.getUsers().values()).stream()
                        .filter(FisheryUserBean::checkChanged)
                        .forEach(this::saveFisheryUserBean);

                LOGGER.debug("### FISHERY SAVED SERVER {} ###", fisheryServerBean.getServerId());
                if (Bot.isRunning()) Thread.sleep(750);
            }
        } catch (Throwable e) {
            update(fisheryServerBean, null);
            LOGGER.error("Could not save fishery server {}", fisheryServerBean.getServerId(), e);
        }
    }

    private void saveFisheryUserBean(FisheryUserBean fisheryUserBean) {
        try {
            DBMain.getInstance().update("REPLACE INTO PowerPlantUsers (serverId, userId, joule, coins, dailyRecieved, dailyStreak, reminderSent, upvotesUnclaimed) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", preparedStatement -> {
                preparedStatement.setLong(1, fisheryUserBean.getServerId());
                preparedStatement.setLong(2, fisheryUserBean.getUserId());
                preparedStatement.setLong(3, fisheryUserBean.getFish());
                preparedStatement.setLong(4, fisheryUserBean.getCoinsRaw());
                preparedStatement.setString(5, DBMain.localDateToDateString(fisheryUserBean.getDailyReceived()));
                preparedStatement.setLong(6, fisheryUserBean.getDailyStreak());
                preparedStatement.setBoolean(7, fisheryUserBean.isReminderSent());
                preparedStatement.setInt(8, fisheryUserBean.getUpvoteStack());
            });

            fisheryUserBean.getAllFishHourlyIncomeChanged().forEach(this::saveFisheryHourlyIncomeBean);
            fisheryUserBean.getPowerUpMap().values().stream()
                    .filter(FisheryUserPowerUpBean::checkChanged)
                    .forEach(this::saveFisheryUserPowerUpBean);
        } catch (Throwable e) {
            fisheryUserBean.setChanged();
            LOGGER.error("Could not save fishery user {} on server {}", fisheryUserBean.getUserId(), fisheryUserBean.getServerId(), e);
        }
    }

    protected void removeFisheryUserBean(FisheryUserBean fisheryUserBean) {
        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUsers WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, fisheryUserBean.getServerId());
            preparedStatement.setLong(2, fisheryUserBean.getUserId());
        });

        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUserPowerUp WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, fisheryUserBean.getServerId());
            preparedStatement.setLong(2, fisheryUserBean.getUserId());
        });

        DBMain.getInstance().asyncUpdate("DELETE FROM PowerPlantUserGained WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, fisheryUserBean.getServerId());
            preparedStatement.setLong(2, fisheryUserBean.getUserId());
        });
    }

    private void saveFisheryUserPowerUpBean(FisheryUserPowerUpBean fisheryUserPowerUpBean) {
        try {
            DBMain.getInstance().update("REPLACE INTO PowerPlantUserPowerUp (serverId, userId, categoryId, level) VALUES (?, ?, ?, ?);", preparedStatement -> {
                preparedStatement.setLong(1, fisheryUserPowerUpBean.getServerId());
                preparedStatement.setLong(2, fisheryUserPowerUpBean.getUserId());
                preparedStatement.setInt(3, fisheryUserPowerUpBean.getPowerUpId());
                preparedStatement.setInt(4, fisheryUserPowerUpBean.getLevel());
            });
        } catch (Throwable e) {
            fisheryUserPowerUpBean.setChanged();
            LOGGER.error("Could not save fishery power up bean", e);
        }
    }

    private void saveFisheryHourlyIncomeBean(FisheryHourlyIncomeBean fisheryHourlyIncomeBean) {
        try {
            DBMain.getInstance().update("REPLACE INTO PowerPlantUserGained (serverId, userId, time, coinsGrowth) VALUES (?, ?, ?, ?);", preparedStatement -> {
                preparedStatement.setLong(1, fisheryHourlyIncomeBean.getServerId());
                preparedStatement.setLong(2, fisheryHourlyIncomeBean.getUserId());
                preparedStatement.setString(3, DBMain.instantToDateTimeString(fisheryHourlyIncomeBean.getTime()));
                preparedStatement.setLong(4, fisheryHourlyIncomeBean.getFishIncome());
            });
        } catch (Throwable e) {
            fisheryHourlyIncomeBean.setChanged();
            LOGGER.error("Could not save fishery hourly income", e);
        }
    }

    private HashMap<Long, FisheryUserBean> getFisheryUsers(long serverId, ServerBean serverBean, HashMap<Long, HashMap<Instant, FisheryHourlyIncomeBean>> fisheryHourlyIncomeMap, HashMap<Long, HashMap<Integer, FisheryUserPowerUpBean>> fisheryPowerUpMap) throws SQLException, ExecutionException {
        HashMap<Long, FisheryUserBean> usersMap = new HashMap<>();

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT userId, joule, coins, dailyRecieved, dailyStreak, reminderSent, upvotesUnclaimed FROM PowerPlantUsers WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        while (resultSet.next()) {
            long userId = resultSet.getLong(1);
            FisheryUserBean fisheryUserBean = new FisheryUserBean(
                    serverBean,
                    userId,
                    resultSet.getLong(2),
                    resultSet.getLong(3),
                    resultSet.getDate(4).toLocalDate(),
                    resultSet.getLong(5),
                    resultSet.getBoolean(6),
                    resultSet.getInt(7),
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

    private ArrayList<Long> getRoleIds(long serverId) throws SQLException {
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

    private ArrayList<Long> getIgnoredChannelIds(long serverId) throws SQLException {
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

    public void removePowerPlant(long serverId) throws SQLException, InterruptedException {
        DBMain.getInstance().update("DELETE FROM PowerPlantUserPowerUp WHERE serverId = ?;", preparedStatement -> preparedStatement.setLong(1, serverId));
        DBMain.getInstance().update("DELETE FROM PowerPlantUsers WHERE serverId = ?;", preparedStatement -> preparedStatement.setLong(1, serverId));
        DBMain.getInstance().update("DELETE FROM PowerPlantUserGained WHERE serverId = ?;", preparedStatement -> preparedStatement.setLong(1, serverId));

        try {
            DBServer.getInstance().getBean(serverId).setFisheryStatus(FisheryStatus.STOPPED);
        } catch (ExecutionException e) {
            LOGGER.error("Could not get server bean", e);
        }

        getCache().invalidate(serverId);
    }

    public void startVCObserver() {
        if (vcObserverStarted) return;
        vcObserverStarted = true;

        new CustomThread(() -> {
            IntervalBlock intervalBlock = new IntervalBlock(VC_CHECK_INTERVAL_MIN, ChronoUnit.MINUTES);
            while (intervalBlock.block()) {
                LOGGER.info("VC Observer - Start");
                DiscordApiCollection.getInstance().getServers().stream()
                        .filter(server -> {
                            try {
                                return DBServer.getInstance().getBean(server.getId()).getFisheryStatus() == FisheryStatus.ACTIVE;
                            } catch (Throwable e) {
                                LOGGER.error("Could not get server bean", e);
                            }
                            return false;
                        })
                        .forEach(server -> {
                            try {
                                manageVCFish(server);
                            } catch (Throwable e) {
                                LOGGER.error("Could not manage vc fish observer", e);
                            }
                        });
                LOGGER.info("VC Observer - End");
            }
        }, "vc_observer", 1).start();
    }

    private void manageVCFish(Server server) throws ExecutionException {
        FisheryServerBean serverBean = DBFishery.getInstance().getBean(server.getId());

        for (ServerVoiceChannel voiceChannel : server.getVoiceChannels()) {
            try {
                ArrayList<User> validUsers = getValidUsers(server, voiceChannel);
                if (validUsers.size() > 1 &&
                        (server.getAfkChannel().isEmpty() || voiceChannel.getId() != server.getAfkChannel().get().getId())
                ) {
                    validUsers.forEach(user -> {
                        try {
                            serverBean.getUserBean(user.getId()).registerVC(VC_CHECK_INTERVAL_MIN);
                        } catch (ExecutionException e) {
                            LOGGER.error("Exception when registering vc", e);
                        }
                    });
                }
            } catch (Throwable e) {
                LOGGER.error("Error while fetching VC member list", e);
            }
        }
    }

    private ArrayList<User> getValidUsers(Server server, ServerVoiceChannel voiceChannel) {
        ArrayList<User> validUsers = new ArrayList<>();
        for (long userId : voiceChannel.getConnectedUserIds()) {
            server.getMemberById(userId).ifPresent(user -> {
                try {
                    if (!user.isBot() &&
                            !user.isMuted(server) &&
                            !user.isDeafened(server) &&
                            !user.isSelfDeafened(server) &&
                            !user.isSelfMuted(server) &&
                            !DBBannedUsers.getInstance().getBean().getUserIds().contains(user.getId())
                    ) {
                        validUsers.add(user);
                    }
                } catch (SQLException throwables) {
                    LOGGER.error("SQL Error", throwables);
                }
            });
        }

        return validUsers;
    }

    @Override
    public int getIntervalMinutes() {
        return 20;
    }

}
