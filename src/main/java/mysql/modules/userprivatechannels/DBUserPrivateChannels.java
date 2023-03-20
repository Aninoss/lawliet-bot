package mysql.modules.userprivatechannels;

import java.util.Map;
import core.CustomObservableMap;
import core.ShardManager;
import mysql.DBDataLoad;
import mysql.DBSingleCache;
import mysql.MySQLManager;

public class DBUserPrivateChannels extends DBSingleCache<CustomObservableMap<Long, PrivateChannelData>> {

    private static final DBUserPrivateChannels ourInstance = new DBUserPrivateChannels();

    public static DBUserPrivateChannels getInstance() {
        return ourInstance;
    }

    private DBUserPrivateChannels() {
    }

    @Override
    protected CustomObservableMap<Long, PrivateChannelData> loadBean() throws Exception {
        Map<Long, PrivateChannelData> map = new DBDataLoad<PrivateChannelData>("UserPrivateChannels", "userId, privateChannelId", "botId = ?",
                preparedStatement -> preparedStatement.setLong(1, ShardManager.getSelfId())
        ).getMap(
                PrivateChannelData::getUserId,
                resultSet -> new PrivateChannelData(
                        resultSet.getLong(1),
                        resultSet.getLong(2)
                )
        );

        return new CustomObservableMap<>(map)
                .addMapAddListener(this::addPrivateChannel)
                .addMapUpdateListener(this::addPrivateChannel);
    }

    private void addPrivateChannel(PrivateChannelData privateChannelData) {
        MySQLManager.asyncUpdate(
                "REPLACE INTO UserPrivateChannels (botId, userId, privateChannelId) VALUES (?, ?, ?);", preparedStatement -> {
                    preparedStatement.setLong(1, ShardManager.getSelfId());
                    preparedStatement.setLong(2, privateChannelData.getUserId());
                    preparedStatement.setLong(3, privateChannelData.getPrivateChannelId());
                }
        );
    }

    @Override
    public Integer getExpirationTimeMinutes() {
        return 1;
    }

}
