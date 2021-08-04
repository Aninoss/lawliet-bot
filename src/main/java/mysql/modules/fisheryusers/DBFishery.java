package mysql.modules.fisheryusers;

import java.util.ArrayList;
import java.util.List;
import core.MainLogger;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBMapCache;
import mysql.DBRedis;
import redis.clients.jedis.Pipeline;

public class DBFishery extends DBMapCache<Long, FisheryGuildData> {

    private static final DBFishery ourInstance = new DBFishery();

    public static DBFishery getInstance() {
        return ourInstance;
    }

    private DBFishery() {
    }

    @Override
    protected FisheryGuildData load(Long serverId) throws Exception {
        FisheryGuildData fisheryGuildBean = new FisheryGuildData(
                serverId,
                getIgnoredChannelIds(serverId),
                getRoleIds(serverId)
        );

        fisheryGuildBean.getRoleIds()
                .addListAddListener(list -> list.forEach(roleId -> addRoleId(fisheryGuildBean.getGuildId(), roleId)))
                .addListRemoveListener(list -> list.forEach(roleId -> removeRoleId(fisheryGuildBean.getGuildId(), roleId)));
        fisheryGuildBean.getIgnoredChannelIds()
                .addListAddListener(list -> list.forEach(channelId -> addIgnoredChannelId(fisheryGuildBean.getGuildId(), channelId)))
                .addListRemoveListener(list -> list.forEach(channelId -> removeIgnoredChannelId(fisheryGuildBean.getGuildId(), channelId)));

        return fisheryGuildBean;
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

    @Override
    public void invalidateGuildId(long guildId) {
        FisheryGuildData fisheryGuildData = retrieve(guildId);
        DBRedis.getInstance().update(jedis -> {
            List<String> accountKeys = DBRedis.getInstance().scan(jedis, "fishery_account:" + guildId + ":*");
            Pipeline pipeline = jedis.pipelined();
            pipeline.del(fisheryGuildData.KEY_RECENT_FISH_GAINS_RAW);
            pipeline.del(fisheryGuildData.KEY_RECENT_FISH_GAINS_PROCESSED);
            pipeline.del(accountKeys.toArray(new String[0]));
            pipeline.sync();
        });
        getCache().invalidate(guildId);
    }

    public List<Long> getGuildIdsForFisheryUser(long userId) {
        ArrayList<Long> guildIds = new ArrayList<>();
        DBRedis.getInstance().update(jedis -> {
            List<String> accountKeys = DBRedis.getInstance().scan(jedis, "fishery_account:*:" + userId);
            for (String accountKey : accountKeys) {
                String[] parts = accountKey.split(":");
                long fisheryGuildId = Long.parseLong(parts[1]);
                long fisheryUserId = Long.parseLong(parts[2]);
                if (fisheryUserId == userId) {
                    guildIds.add(fisheryGuildId);
                } else {
                    MainLogger.get().error("Returning wrong entries for fishery user");
                }
            }
        });
        return guildIds;
    }

}
