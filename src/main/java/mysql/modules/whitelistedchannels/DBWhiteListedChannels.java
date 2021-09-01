package mysql.modules.whitelistedchannels;

import java.util.List;
import mysql.DBDataLoad;
import mysql.DBObserverMapCache;
import mysql.MySQLManager;

public class DBWhiteListedChannels extends DBObserverMapCache<Long, WhiteListedChannelsData> {

    private static final DBWhiteListedChannels ourInstance = new DBWhiteListedChannels();

    public static DBWhiteListedChannels getInstance() {
        return ourInstance;
    }

    private DBWhiteListedChannels() {
    }

    @Override
    protected WhiteListedChannelsData load(Long serverId) throws Exception {
        WhiteListedChannelsData whiteListedChannelsBean = new WhiteListedChannelsData(
                serverId,
                getChannelIds(serverId)
        );

        whiteListedChannelsBean.getChannelIds()
                .addListAddListener(list -> list.forEach(channelId -> addChannelId(whiteListedChannelsBean.getGuildId(), channelId)))
                .addListRemoveListener(list -> list.forEach(channelI -> removeChannelId(whiteListedChannelsBean.getGuildId(), channelI)));

        return whiteListedChannelsBean;
    }

    @Override
    protected void save(WhiteListedChannelsData whiteListedChannelsBean) {
    }

    private List<Long> getChannelIds(long serverId) {
        return new DBDataLoad<Long>("WhiteListedChannels", "channelId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getList(resultSet -> resultSet.getLong(1));
    }

    private void addChannelId(long serverId, long roleId) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO WhiteListedChannels (serverId, channelId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, roleId);
        });
    }

    private void removeChannelId(long serverId, long roleId) {
        MySQLManager.asyncUpdate("DELETE FROM WhiteListedChannels WHERE serverId = ? AND channelId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, roleId);
        });
    }

}
