package mysql.modules.ticket;

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
        TicketData ticketData = DBMain.getInstance().get(
                "SELECT channelId, counter, memberCanClose FROM Ticket WHERE serverId = ?;",
                preparedStatement -> preparedStatement.setLong(1, serverId),
                resultSet -> {
                    if (resultSet.next()) {
                        return new TicketData(
                                serverId,
                                resultSet.getLong(1),
                                resultSet.getInt(2),
                                resultSet.getBoolean(3),
                                getStaffRoles(serverId),
                                getTicketChannels(serverId)
                        );
                    } else {
                        return new TicketData(
                                serverId,
                                null,
                                0,
                                true,
                                getStaffRoles(serverId),
                                getTicketChannels(serverId)
                        );
                    }
                }
        );

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
        DBMain.getInstance().asyncUpdate("REPLACE INTO Ticket (serverId, channelId, counter, memberCanClose) VALUES (?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, ticketData.getGuildId());

            Optional<Long> channelIdOpt = ticketData.getAnnouncementTextChannelId();
            if (channelIdOpt.isPresent()) {
                preparedStatement.setLong(2, channelIdOpt.get());
            } else {
                preparedStatement.setNull(2, Types.BIGINT);
            }

            preparedStatement.setInt(3, ticketData.getCounter());
            preparedStatement.setBoolean(4, ticketData.memberCanClose());
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
