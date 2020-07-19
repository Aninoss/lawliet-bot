package MySQL.Modules.AutoChannel;

import Core.Bot;
import Core.DiscordApiCollection;
import MySQL.DBDataLoad;
import MySQL.DBKeySetLoad;
import MySQL.DBMain;
import MySQL.DBBeanGenerator;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.DiscordApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class DBAutoChannel extends DBBeanGenerator<Long, AutoChannelBean> {

    private final static Logger LOGGER = LoggerFactory.getLogger(DBAutoChannel.class);
    private static final DBAutoChannel ourInstance = new DBAutoChannel();
    public static DBAutoChannel getInstance() { return ourInstance; }
    private DBAutoChannel() {}

    @Override
    protected AutoChannelBean loadBean(Long serverId) throws Exception {
        AutoChannelBean autoChannelBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT channelId, active, channelName, locked FROM AutoChannel WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            autoChannelBean = new AutoChannelBean(
                    DBServer.getInstance().getBean(serverId),
                    resultSet.getLong(1),
                    resultSet.getBoolean(2),
                    resultSet.getString(3),
                    resultSet.getBoolean(4),
                    getChildChannels(serverId)
            );
        } else {
            autoChannelBean = new AutoChannelBean(
                    DBServer.getInstance().getBean(serverId),
                    null,
                    false,
                    "%VCName [%Creator]",
                    false,
                    new ArrayList<>()
            );
        }

        resultSet.close();
        preparedStatement.close();

        autoChannelBean.getChildChannels()
                .addListAddListener(list -> list.forEach(channelId -> addChildChannel(autoChannelBean.getServerId(), channelId)))
                .addListRemoveListener(list -> list.forEach(channelId -> removeChildChannel(autoChannelBean.getServerId(), channelId)));

        return autoChannelBean;
    }

    @Override
    protected void saveBean(AutoChannelBean autoChannelBean) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO AutoChannel (serverId, channelId, active, channelName, locked) VALUES (?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, autoChannelBean.getServerId());

            Optional<Long> channelIdOpt = autoChannelBean.getParentChannelId();
            if (channelIdOpt.isPresent()) preparedStatement.setLong(2, channelIdOpt.get());
            else preparedStatement.setNull(2, Types.BIGINT);

            preparedStatement.setBoolean(3, autoChannelBean.isActive());
            preparedStatement.setString(4, autoChannelBean.getNameMask());
            preparedStatement.setBoolean(5, autoChannelBean.isLocked());
        });
    }

    private ArrayList<Long> getChildChannels(long serverId) throws SQLException {
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

    public void synchronize(DiscordApi api) {
        if (Bot.isProductionMode()) {
            try {
                getAllChildChannelServerIds().stream()
                        .filter(serverId -> DiscordApiCollection.getInstance().getResponsibleShard(serverId) == api.getCurrentShard())
                        .map(api::getServerById)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .forEach(server -> {
                            try {
                                getBean(server.getId()).getChildChannels()
                                        .removeIf(childChannelId -> !server.getVoiceChannelById(childChannelId).isPresent());
                            } catch (ExecutionException e) {
                                LOGGER.error("Could not get bean", e);
                            }
                        });
            } catch (SQLException e) {
                LOGGER.error("Error in auto channel synchronization");
            }
        }
    }

    private ArrayList<Long> getAllChildChannelServerIds() throws SQLException {
        return new DBKeySetLoad<Long>("AutoChannelChildChannels", "serverId")
                .get(resultSet -> resultSet.getLong(1));
    }

}
