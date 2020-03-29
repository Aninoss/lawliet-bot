package MySQL.AutoChannel;

import General.Bot;
import General.DiscordApiCollection;
import MySQL.DBArrayListLoad;
import MySQL.DBMain;
import MySQL.DBBeanGenerator;
import MySQL.Server.DBServer;
import org.javacord.api.DiscordApi;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class DBAutoChannel extends DBBeanGenerator<Long, AutoChannelBean> {

    private static DBAutoChannel ourInstance = new DBAutoChannel();
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
                    serverId,
                    DBServer.getInstance().getBean(serverId),
                    resultSet.getLong(1),
                    resultSet.getBoolean(2),
                    resultSet.getString(3),
                    resultSet.getBoolean(4),
                    getChildChannels(serverId)
            );
        } else {
            autoChannelBean = new AutoChannelBean(
                    serverId,
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
    protected void saveBean(AutoChannelBean autoChannelBean) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("REPLACE INTO AutoChannel (serverId, channelId, active, channelName, locked) VALUES (?, ?, ?, ?, ?);");
        preparedStatement.setLong(1, autoChannelBean.getServerId());

        Optional<Long> channelIdOpt = autoChannelBean.getParentChannelId();
        if (channelIdOpt.isPresent()) preparedStatement.setLong(2, channelIdOpt.get());
        else preparedStatement.setNull(2, Types.BIGINT);

        preparedStatement.setBoolean(3, autoChannelBean.isActive());
        preparedStatement.setString(4, autoChannelBean.getNameMask());
        preparedStatement.setBoolean(5, autoChannelBean.isLocked());

        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    private ArrayList<Long> getChildChannels(long serverId) throws SQLException {
        DBArrayListLoad<Long> dbArrayListLoad = new DBArrayListLoad<>("AutoChannelChildChannels", "channelId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId));
        return dbArrayListLoad.getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addChildChannel(long serverId, long channelId) {
        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("INSERT IGNORE INTO AutoChannelChildChannels (serverId, channelId) VALUES (?, ?);");
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, channelId);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void removeChildChannel(long serverId, long channelId) {
        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM AutoChannelChildChannels WHERE serverId = ? AND channelId = ?;");
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, channelId);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void synchronize(DiscordApi api) {
        if (!Bot.isDebug()) {
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
                                e.printStackTrace();
                            }
                        });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayList<Long> getAllChildChannelServerIds() throws SQLException {
        ArrayList<Long> serverIds = new ArrayList<>();

        Statement statement = DBMain.getInstance().statement("SELECT serverId FROM AutoChannelChildChannels;");
        ResultSet resultSet = statement.getResultSet();
        while (resultSet.next()) serverIds.add(resultSet.getLong(1));

        resultSet.close();
        statement.close();

        return serverIds;
    }

}
