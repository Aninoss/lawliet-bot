package MySQL.Modules.Server;

import Constants.Locales;
import Constants.FisheryStatus;
import Core.DiscordApiCollection;
import MySQL.DBKeySetLoad;
import MySQL.DBMain;
import MySQL.DBBeanGenerator;
import MySQL.Modules.Tracker.DBTracker;
import MySQL.Modules.Tracker.TrackerBeanSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

public class DBServer extends DBBeanGenerator<Long, ServerBean> {

    final static Logger LOGGER = LoggerFactory.getLogger(DBServer.class);
    private static final DBServer ourInstance = new DBServer();
    public static DBServer getInstance() { return ourInstance; }
    private DBServer() {}

    private final ArrayList<Long> removedServerIds = new ArrayList<>();

    @Override
    protected ServerBean loadBean(Long serverId) throws Exception {
        boolean serverPresent = DiscordApiCollection.getInstance().getServerById(serverId).isPresent();
        if (serverPresent) removedServerIds.remove(serverId);

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
            if (serverPresent) insertBean(serverBean);
        }

        resultSet.close();
        preparedStatement.close();

        return serverBean;
    }

    public boolean containsServerId(long serverId) {
        return !removedServerIds.contains(serverId);
    }

    private void insertBean(ServerBean serverBean) throws SQLException, InterruptedException {
        DBMain.getInstance().update("INSERT INTO DServer (serverId, prefix, locale, powerPlant, powerPlantSingleRole, powerPlantAnnouncementChannelId, powerPlantTreasureChests, powerPlantReminders, powerPlantRoleMin, powerPlantRoleMax, webhookUrl) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
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
        });
    }

    @Override
    protected void saveBean(ServerBean serverBean) {
        DBMain.getInstance().asyncUpdate("UPDATE DServer SET prefix = ?, locale = ?, powerPlant = ?, powerPlantSingleRole = ?, powerPlantAnnouncementChannelId = ?, powerPlantTreasureChests = ?, powerPlantReminders = ?, powerPlantRoleMin = ?, powerPlantRoleMax = ?, webhookUrl = ? WHERE serverId = ?;", preparedStatement -> {
            preparedStatement.setLong(11, serverBean.getServerId());

            preparedStatement.setString(1, serverBean.getPrefix());
            preparedStatement.setString(2, serverBean.getLocale().getDisplayName());
            preparedStatement.setString(3, serverBean.getFisheryStatus().name());
            preparedStatement.setBoolean(4, serverBean.isFisherySingleRoles());

            Optional<Long> announcementChannelIdOpt = serverBean.getFisheryAnnouncementChannelId();
            if (announcementChannelIdOpt.isPresent()) preparedStatement.setLong(5, announcementChannelIdOpt.get());
            else preparedStatement.setNull(5, Types.BIGINT);

            preparedStatement.setBoolean(6, serverBean.isFisheryTreasureChests());
            preparedStatement.setBoolean(7, serverBean.isFisheryReminders());
            preparedStatement.setLong(8, serverBean.getFisheryRoleMin());
            preparedStatement.setLong(9, serverBean.getFisheryRoleMax());

            Optional<String> webhookOpt = serverBean.getWebhookUrl();
            if (webhookOpt.isPresent()) preparedStatement.setString(10, webhookOpt.get());
            else preparedStatement.setNull(10, Types.VARCHAR);
        });
    }

    public void remove(long serverId) {
        removedServerIds.add(serverId);
        DBMain.getInstance().asyncUpdate("DELETE FROM DServer WHERE serverId = ?;", preparedStatement -> preparedStatement.setLong(1, serverId));
        try {
            DBTracker.getInstance().getBean().getMap().values().stream()
                    .filter(slot -> slot.getServerId() == serverId)
                    .forEach(TrackerBeanSlot::stop);
        } catch (SQLException e) {
            LOGGER.error("Could not fetch tracker bean", e);
        }
        getCache().invalidate(serverId);
        new File(String.format("data/welcome_backgrounds/%d.png", serverId)).delete();
    }

    public ArrayList<Long> getAllServerIds() throws SQLException {
        return new DBKeySetLoad<Long>("DServers", "serverId")
                .get(resultSet -> resultSet.getLong(1));
    }

}
