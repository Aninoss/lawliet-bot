package MySQL.Server;

import Constants.Locales;
import Constants.FisheryStatus;
import MySQL.DBMain;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class DBServer implements Observer {

    private static DBServer ourInstance = new DBServer();
    public static DBServer getInstance() { return ourInstance; }
    private DBServer() {}

    private LoadingCache<Long, ServerBean> cache = CacheBuilder.newBuilder().build(
            new CacheLoader<Long, ServerBean>() {
            @Override
                public ServerBean load(@NonNull Long serverId) throws SQLException {
                    ServerBean serverBean;

                    PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT prefix, locale, powerPlant, powerPlantSingleRole, powerPlantAnnouncementChannelId, powerPlantTreasureChests, powerPlantReminders, powerPlantRoleMin, powerPlantRoleMax, webhookUrl FROM DServer WHERE serverId = ?;");
                    preparedStatement.setLong(1, serverId);
                    preparedStatement.execute();

                    ResultSet resultSet = preparedStatement.getResultSet();
                    if (resultSet.next()) {
                        serverBean = new ServerBean(
                                serverId,
                                resultSet.getString(1),
                                new Locale(resultSet.getString(2)),
                                FisheryStatus.valueOf(resultSet.getString(3)),
                                resultSet.getBoolean(4),
                                resultSet.getLong(5),
                                resultSet.getBoolean(6),
                                resultSet.getBoolean(7),
                                resultSet.getLong(8),
                                resultSet.getLong(9),
                                resultSet.getString(10)
                        );
                    } else {
                        serverBean = new ServerBean(
                                serverId,
                                "L.",
                                new Locale(Locales.EN),
                                FisheryStatus.STOPPED,
                                false,
                                null,
                                true,
                                true,
                                50000,
                                800000000,
                                null
                        );
                        saveBean(serverBean);
                    }

                    resultSet.close();
                    preparedStatement.close();

                    serverBean.addObserver(DBServer.getInstance());
                    return serverBean;
                }
            });

    public ServerBean getServerBean(long serverId) throws ExecutionException {
        return cache.get(serverId);
    }

    @Override
    public void update(@NonNull Observable o, Object arg) {
        try {
            saveBean((ServerBean) o);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveBean(ServerBean serverBean) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("REPLACE INTO DServer (serverId, prefix, locale, powerPlant, powerPlantSingleRole, powerPlantAnnouncementChannelId, powerPlantTreasureChests, powerPlantReminders, powerPlantRoleMin, powerPlantRoleMax, webhookUrl) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
        preparedStatement.setLong(1, serverBean.getServerId());
        preparedStatement.setString(2, serverBean.getPrefix());
        preparedStatement.setString(3, serverBean.getLocale().getDisplayName());
        preparedStatement.setString(4, serverBean.getFisheryStatus().name());
        preparedStatement.setBoolean(5, serverBean.isFisherySingleRoles());

        Optional<Long> announcementChannelIdOpt = serverBean.getFisheryAnnouncementChannelId();
        if (announcementChannelIdOpt.isPresent()) preparedStatement.setLong(6, announcementChannelIdOpt.get());
        else preparedStatement.setNull(6, Types.BIGINT);

        preparedStatement.setBoolean(7, serverBean.isFisheryTreasureChests());
        preparedStatement.setBoolean(8, serverBean.isFisheryReminders());
        preparedStatement.setLong(9, serverBean.getFisheryRoleMin());
        preparedStatement.setLong(10, serverBean.getFisheryRoleMax());

        Optional<String> webhookOpt = serverBean.getWebhookUrl();
        if (webhookOpt.isPresent()) preparedStatement.setString(11, webhookOpt.get());
        else preparedStatement.setNull(11, Types.VARCHAR);

        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

}
