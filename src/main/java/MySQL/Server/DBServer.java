package MySQL.Server;

import Constants.Locales;
import Constants.FisheryStatus;
import MySQL.DBMain;
import MySQL.DBBeanGenerator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;
import java.util.Optional;

public class DBServer extends DBBeanGenerator<Long, ServerBean> {

    private static DBServer ourInstance = new DBServer();
    public static DBServer getInstance() { return ourInstance; }
    private DBServer() {}

    @Override
    protected ServerBean loadBean(Long serverId) throws Exception {
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

        return serverBean;
    }

    @Override
    protected void saveBean(ServerBean serverBean) throws SQLException {
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
