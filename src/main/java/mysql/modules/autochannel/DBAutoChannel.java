package mysql.modules.autochannel;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Optional;
import mysql.DBDataLoad;
import mysql.DBKeySetLoad;
import mysql.DBMain;
import mysql.DBMapCache;

public class DBAutoChannel extends DBMapCache<Long, AutoChannelBean> {

    private static final DBAutoChannel ourInstance = new DBAutoChannel();

    public static DBAutoChannel getInstance() {
        return ourInstance;
    }

    private DBAutoChannel() {
    }

    @Override
    protected AutoChannelBean load(Long serverId) throws Exception {
        AutoChannelBean autoChannelBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT channelId, active, channelName, locked FROM AutoChannel WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            autoChannelBean = new AutoChannelBean(
                    serverId,
                    resultSet.getLong(1),
                    resultSet.getBoolean(2),
                    resultSet.getString(3),
                    resultSet.getBoolean(4),
                    getChildChannels(serverId)
            );
        } else {
            autoChannelBean = new AutoChannelBean(
                    serverId,
                    null,
                    false,
                    "%VCName [%Creator]",
                    false,
                    new ArrayList<>()
            );
        }

        resultSet.close();
        preparedStatement.close();

        autoChannelBean.getChildChannelIds()
                .addListAddListener(list -> list.forEach(channelId -> addChildChannel(autoChannelBean.getGuildId(), channelId)))
                .addListRemoveListener(list -> list.forEach(channelId -> removeChildChannel(autoChannelBean.getGuildId(), channelId)));

        return autoChannelBean;
    }

    @Override
    protected void save(AutoChannelBean autoChannelBean) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO AutoChannel (serverId, channelId, active, channelName, locked) VALUES (?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, autoChannelBean.getGuildId());

            Optional<Long> channelIdOpt = autoChannelBean.getParentChannelId();
            if (channelIdOpt.isPresent()) preparedStatement.setLong(2, channelIdOpt.get());
            else preparedStatement.setNull(2, Types.BIGINT);

            preparedStatement.setBoolean(3, autoChannelBean.isActive());
            preparedStatement.setString(4, autoChannelBean.getNameMask());
            preparedStatement.setBoolean(5, autoChannelBean.isLocked());
        });
    }

    private ArrayList<Long> getChildChannels(long serverId) {
        return new DBDataLoad<Long>("AutoChannelChildChannels", "channelId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addChildChannel(long serverId, long channelId) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO AutoChannelChildChannels (serverId, channelId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, channelId);
        });
    }

    private void removeChildChannel(long serverId, long channelId) {
        DBMain.getInstance().asyncUpdate("DELETE FROM AutoChannelChildChannels WHERE serverId = ? AND channelId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, channelId);
        });
    }

    public ArrayList<Long> retrieveAllChildChannelServerIds() {
        return new DBKeySetLoad<Long>("AutoChannelChildChannels", "serverId")
                        .get(resultSet -> resultSet.getLong(1));
    }

}
