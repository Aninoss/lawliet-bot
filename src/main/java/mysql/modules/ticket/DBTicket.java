package mysql.modules.ticket;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
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

        try (PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT channelId, counter FROM Ticket WHERE serverId = ?;")) {
            preparedStatement.setLong(1, serverId);
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            if (resultSet.next()) {
                ticketData = new TicketData(
                        serverId,
                        resultSet.getLong(1),
                        resultSet.getInt(2),
                        getStaffRoles(serverId),
                        getTicketChannels(serverId)
                );
            } else {
                ticketData = new TicketData(
                        serverId,
                        null,
                        0,
                        getStaffRoles(serverId),
                        getTicketChannels(serverId)
                );
            }
        }

        ticketData.getTicketChannels()
                .addMapAddListener(this::addTicketChannel)
                .addMapRemoveListener(this::removeTicketChannel);

        ticketData.getStaffRoleIds()
                .addListAddListener(list -> list.forEach(roleId -> addStaffRole(serverId, roleId)))
                .addListRemoveListener(list -> list.forEach(roleId -> removeStaffRole(serverId, roleId)));

        return ticketData;
    }

    @Override
    protected void save(TicketData ticketData) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO Ticket (serverId, channelId, counter) VALUES (?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, ticketData.getGuildId());

            Optional<Long> channelIdOpt = ticketData.getAnnouncementTextChannelId();
            if (channelIdOpt.isPresent()) {
                preparedStatement.setLong(2, channelIdOpt.get());
            } else {
                preparedStatement.setNull(2, Types.BIGINT);
            }

            preparedStatement.setInt(3, ticketData.getCounter());
        });
    }

    private HashMap<Long, TicketChannel> getTicketChannels(long serverId) {
        return new DBDataLoad<TicketChannel>("TicketOpenChannel", "channelId, userId, messageChannelId, messageMessageId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getHashMap(
                TicketChannel::getTextChannelId,
                resultSet -> new TicketChannel(
                        serverId,
                        resultSet.getLong(1),
                        resultSet.getLong(2),
                        resultSet.getLong(3),
                        resultSet.getLong(4)
                )
        );
    }

    private void addTicketChannel(TicketChannel ticketChannel) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO TicketOpenChannel (serverId, channelId, userId, messageChannelId, messageMessageId) VALUES (?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, ticketChannel.getGuildId());
            preparedStatement.setLong(2, ticketChannel.getTextChannelId());
            preparedStatement.setLong(3, ticketChannel.getMemberId());
            preparedStatement.setLong(4, ticketChannel.getAnnouncementChannelId());
            preparedStatement.setLong(5, ticketChannel.getAnnouncementMessageId());
        });
    }

    private void removeTicketChannel(TicketChannel ticketChannel) {
        DBMain.getInstance().asyncUpdate("DELETE FROM TicketOpenChannel WHERE serverId = ? AND channelId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, ticketChannel.getGuildId());
            preparedStatement.setLong(2, ticketChannel.getTextChannelId());
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
