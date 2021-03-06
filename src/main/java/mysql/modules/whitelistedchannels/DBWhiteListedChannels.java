package mysql.modules.whitelistedchannels;

import mysql.DBMapCache;
import mysql.DBDataLoad;
import mysql.DBMain;

import java.sql.SQLException;
import java.util.ArrayList;

public class DBWhiteListedChannels extends DBMapCache<Long, WhiteListedChannelsBean> {

    private static final DBWhiteListedChannels ourInstance = new DBWhiteListedChannels();

    public static DBWhiteListedChannels getInstance() {
        return ourInstance;
    }

    private DBWhiteListedChannels() {
    }

    @Override
    protected WhiteListedChannelsBean load(Long serverId) throws Exception {
        WhiteListedChannelsBean whiteListedChannelsBean = new WhiteListedChannelsBean(
                serverId,
                getChannelIds(serverId)
        );

        whiteListedChannelsBean.getChannelIds()
                .addListAddListener(list -> list.forEach(channelId -> addChannelId(whiteListedChannelsBean.getGuildId(), channelId)))
                .addListRemoveListener(list -> list.forEach(channelI -> removeChannelId(whiteListedChannelsBean.getGuildId(), channelI)));

        return whiteListedChannelsBean;
    }

    @Override
    protected void save(WhiteListedChannelsBean whiteListedChannelsBean) {
    }

    private ArrayList<Long> getChannelIds(long serverId) {
        return new DBDataLoad<Long>("WhiteListedChannels", "channelId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addChannelId(long serverId, long roleId) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO WhiteListedChannels (serverId, channelId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, roleId);
        });
    }

    private void removeChannelId(long serverId, long roleId) {
        DBMain.getInstance().asyncUpdate("DELETE FROM WhiteListedChannels WHERE serverId = ? AND channelId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, roleId);
        });
    }

}
