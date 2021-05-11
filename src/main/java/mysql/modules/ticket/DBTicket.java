package mysql.modules.ticket;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Optional;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBObserverMapCache;

public class DBTicket extends DBObserverMapCache<Long, TicketData> {

    private static final DBTicket ourInstance = new DBTicket();

    public static DBTicket getInstance() {
        return ourInstance;
    }

    private DBTicket() {
    }

    @Override
    protected TicketData load(Long serverId) throws Exception {
        TicketData ticketData;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT channelId FROM TicketAnnouncementChannel WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            ticketData = new TicketData(
                    serverId,
                    resultSet.getLong(1),
                    getStaffRoles(serverId),
                    getOpenChannels(serverId)
            );
        } else {
            ticketData = new TicketData(
                    serverId,
                    null,
                    new ArrayList<>(),
                    new ArrayList<>()
            );
        }

        resultSet.close();
        preparedStatement.close();

        ticketData.getOpenTextChannelIds()
                .addListAddListener(list -> list.forEach(channelId -> addOpenChannel(serverId, channelId)))
                .addListRemoveListener(list -> list.forEach(channelId -> removeOpenChannel(serverId, channelId)));

        ticketData.getStaffRoleIds()
                .addListAddListener(list -> list.forEach(roleId -> addStaffRole(serverId, roleId)))
                .addListRemoveListener(list -> list.forEach(roleId -> removeStaffRole(serverId, roleId)));

        return ticketData;
    }

    @Override
    protected void save(TicketData ticketData) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO TicketAnnouncementChannel (serverId, channelId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, ticketData.getGuildId());

            Optional<Long> channelIdOpt = ticketData.getAnnouncementTextChannelId();
            if (channelIdOpt.isPresent()) {
                preparedStatement.setLong(2, channelIdOpt.get());
            } else {
                preparedStatement.setNull(2, Types.BIGINT);
            }
        });
    }

    private ArrayList<Long> getOpenChannels(long serverId) {
        return new DBDataLoad<Long>("TicketOpenChannel", "channelId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addOpenChannel(long serverId, long channelId) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO TicketOpenChannel (serverId, channelId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, channelId);
        });
    }

    private void removeOpenChannel(long serverId, long channelId) {
        DBMain.getInstance().asyncUpdate("DELETE FROM TicketOpenChannel WHERE serverId = ? AND channelId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, channelId);
        });
    }

    private ArrayList<Long> getStaffRoles(long serverId) {
        return new DBDataLoad<Long>("TicketStaffRole", "roleId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addStaffRole(long serverId, long roleId) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO TicketStaffRole (serverId, roleId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, roleId);
        });
    }

    private void removeStaffRole(long serverId, long roleId) {
        DBMain.getInstance().asyncUpdate("DELETE FROM TicketStaffRole WHERE serverId = ? AND roleId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, roleId);
        });
    }

}
