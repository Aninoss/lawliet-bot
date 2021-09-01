package mysql.modules.spblock;

import java.util.List;
import mysql.DBDataLoad;
import mysql.DBObserverMapCache;
import mysql.MySQLManager;

public class DBSPBlock extends DBObserverMapCache<Long, SPBlockData> {

    private static final DBSPBlock ourInstance = new DBSPBlock();

    public static DBSPBlock getInstance() {
        return ourInstance;
    }

    private DBSPBlock() {
    }

    @Override
    protected SPBlockData load(Long serverId) throws Exception {
        SPBlockData spBlockBean = MySQLManager.get(
                "SELECT active, action FROM SPBlock WHERE serverId = ?;",
                preparedStatement -> preparedStatement.setLong(1, serverId),
                resultSet -> {
                    if (resultSet.next()) {
                        return new SPBlockData(
                                serverId,
                                resultSet.getBoolean(1),
                                SPBlockData.ActionList.valueOf(resultSet.getString(2)),
                                getIgnoredUsers(serverId),
                                getIgnoredChannels(serverId),
                                getLogReceivers(serverId)
                        );
                    } else {
                        return new SPBlockData(
                                serverId,
                                false,
                                SPBlockData.ActionList.DELETE_MESSAGE,
                                getIgnoredUsers(serverId),
                                getIgnoredChannels(serverId),
                                getLogReceivers(serverId)
                        );
                    }
                }
        );

        spBlockBean.getIgnoredUserIds()
                .addListAddListener(list -> list.forEach(userId -> addIgnoredUser(serverId, userId)))
                .addListRemoveListener(list -> list.forEach(userId -> removeIgnoredUser(serverId, userId)));
        spBlockBean.getLogReceiverUserIds()
                .addListAddListener(list -> list.forEach(userId -> addLogReceiver(serverId, userId)))
                .addListRemoveListener(list -> list.forEach(userId -> removeLogReceiver(serverId, userId)));
        spBlockBean.getIgnoredChannelIds()
                .addListAddListener(list -> list.forEach(channelId -> addIgnoredChannels(serverId, channelId)))
                .addListRemoveListener(list -> list.forEach(channelId -> removeIgnoredChannels(serverId, channelId)));
        return spBlockBean;
    }

    @Override
    protected void save(SPBlockData spBlockBean) {
        MySQLManager.asyncUpdate("REPLACE INTO SPBlock (serverId, active, action) VALUES (?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, spBlockBean.getGuildId());
            preparedStatement.setBoolean(2, spBlockBean.isActive());
            preparedStatement.setString(3, spBlockBean.getAction().name());
        });
    }

    private List<Long> getIgnoredUsers(long serverId) {
        return new DBDataLoad<Long>("SPBlockIgnoredUsers", "userId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getList(resultSet -> resultSet.getLong(1));
    }

    private void addIgnoredUser(long serverId, long userId) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO SPBlockIgnoredUsers (serverId, userId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
        });
    }

    private void removeIgnoredUser(long serverId, long userId) {
        MySQLManager.asyncUpdate("DELETE FROM SPBlockIgnoredUsers WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
        });
    }

    private List<Long> getLogReceivers(long serverId) {
        return new DBDataLoad<Long>("SPBlockLogRecievers", "userId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getList(resultSet -> resultSet.getLong(1));
    }

    private void addLogReceiver(long serverId, long userId) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO SPBlockLogRecievers (serverId, userId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
        });
    }

    private void removeLogReceiver(long serverId, long userId) {
        MySQLManager.asyncUpdate("DELETE FROM SPBlockLogRecievers WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
        });
    }

    private List<Long> getIgnoredChannels(long serverId) {
        return new DBDataLoad<Long>("SPBlockIgnoredChannels", "channelId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getList(resultSet -> resultSet.getLong(1));
    }

    private void addIgnoredChannels(long serverId, long channelId) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO SPBlockIgnoredChannels (serverId, channelId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, channelId);
        });
    }

    private void removeIgnoredChannels(long serverId, long channelId) {
        MySQLManager.asyncUpdate("DELETE FROM SPBlockIgnoredChannels WHERE serverId = ? AND channelId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, channelId);
        });
    }

}
