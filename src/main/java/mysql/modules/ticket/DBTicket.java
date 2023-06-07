package mysql.modules.ticket;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mysql.DBDataLoad;
import mysql.DBObserverMapCache;
import mysql.MySQLManager;

public class DBTicket extends DBObserverMapCache<Long, TicketData> {

    private static final DBTicket ourInstance = new DBTicket();

    public static DBTicket getInstance() {
        return ourInstance;
    }

    private DBTicket() {
    }

    @Override
    protected TicketData load(Long serverId) throws Exception {
        TicketData ticketData = MySQLManager.get(
                "SELECT channelId, counter, memberCanClose, createMessage, assignToAll, protocol, ping, userMessages, autoCloseHours, deleteChannelOnTicketClose FROM Ticket WHERE serverId = ?;",
                preparedStatement -> preparedStatement.setLong(1, serverId),
                resultSet -> {
                    if (resultSet.next()) {
                        Integer autoCloseHours = resultSet.getInt(9);
                        if (resultSet.wasNull()) {
                            autoCloseHours = null;
                        }
                        return new TicketData(
                                serverId,
                                resultSet.getLong(1),
                                resultSet.getInt(2),
                                resultSet.getBoolean(3),
                                resultSet.getString(4),
                                TicketData.TicketAssignmentMode.values()[resultSet.getInt(5)],
                                resultSet.getBoolean(6),
                                resultSet.getBoolean(7),
                                resultSet.getBoolean(8),
                                autoCloseHours,
                                resultSet.getBoolean(10),
                                getStaffRoles(serverId),
                                getTicketChannels(serverId)
                        );
                    } else {
                        return new TicketData(
                                serverId,
                                null,
                                0,
                                true,
                                null,
                                TicketData.TicketAssignmentMode.MANUAL,
                                false,
                                true,
                                true,
                                null,
                                true,
                                getStaffRoles(serverId),
                                getTicketChannels(serverId)
                        );
                    }
                }
        );

        ticketData.getTicketChannels()
                .addMapAddListener(this::addTicketChannel)
                .addMapUpdateListener(this::addTicketChannel)
                .addMapRemoveListener(this::removeTicketChannel);

        ticketData.getStaffRoleIds()
                .addListAddListener(list -> list.forEach(roleId -> addStaffRole(serverId, roleId)))
                .addListRemoveListener(list -> list.forEach(roleId -> removeStaffRole(serverId, roleId)));

        return ticketData;
    }

    @Override
    protected void save(TicketData ticketData) {
        MySQLManager.asyncUpdate("REPLACE INTO Ticket (serverId, channelId, counter, memberCanClose, createMessage, assignToAll, protocol, ping, userMessages, autoCloseHours, deleteChannelOnTicketClose) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, ticketData.getGuildId());

            Optional<Long> channelIdOpt = ticketData.getAnnouncementTextChannelId();
            if (channelIdOpt.isPresent()) {
                preparedStatement.setLong(2, channelIdOpt.get());
            } else {
                preparedStatement.setNull(2, Types.BIGINT);
            }

            preparedStatement.setInt(3, ticketData.getCounter());
            preparedStatement.setBoolean(4, ticketData.memberCanClose());

            Optional<String> createMessageOpt = ticketData.getCreateMessage();
            if (createMessageOpt.isPresent()) {
                preparedStatement.setString(5, createMessageOpt.get());
            } else {
                preparedStatement.setNull(5, Types.VARCHAR);
            }

            preparedStatement.setInt(6, ticketData.getTicketAssignmentMode().ordinal());
            preparedStatement.setBoolean(7, ticketData.getProtocol());
            preparedStatement.setBoolean(8, ticketData.getPingStaff());
            preparedStatement.setBoolean(9, ticketData.getUserMessages());

            if (ticketData.getAutoCloseHours() != null) {
                preparedStatement.setInt(10, ticketData.getAutoCloseHours());
            } else {
                preparedStatement.setNull(10, Types.INTEGER);
            }

            preparedStatement.setBoolean(11, ticketData.getDeleteChannelOnTicketClose());
        });
    }

    private Map<Long, TicketChannel> getTicketChannels(long serverId) {
        return new DBDataLoad<TicketChannel>("TicketOpenChannel", "channelId, userId, messageChannelId, messageMessageId, assigned, starterMessageId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getMap(
                TicketChannel::getTextChannelId,
                resultSet -> new TicketChannel(
                        serverId,
                        resultSet.getLong(1),
                        resultSet.getLong(2),
                        resultSet.getLong(3),
                        resultSet.getLong(4),
                        resultSet.getBoolean(5),
                        resultSet.getLong(6)
                )
        );
    }

    private void addTicketChannel(TicketChannel ticketChannel) {
        MySQLManager.asyncUpdate("REPLACE INTO TicketOpenChannel (serverId, channelId, userId, messageChannelId, messageMessageId, assigned, starterMessageId) VALUES (?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, ticketChannel.getGuildId());
            preparedStatement.setLong(2, ticketChannel.getTextChannelId());
            preparedStatement.setLong(3, ticketChannel.getMemberId());
            preparedStatement.setLong(4, ticketChannel.getAnnouncementChannelId());
            preparedStatement.setLong(5, ticketChannel.getAnnouncementMessageId());
            preparedStatement.setBoolean(6, ticketChannel.isAssigned());
            preparedStatement.setLong(7, ticketChannel.getStarterMessageId());
        });
    }

    private void removeTicketChannel(TicketChannel ticketChannel) {
        MySQLManager.asyncUpdate("DELETE FROM TicketOpenChannel WHERE serverId = ? AND channelId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, ticketChannel.getGuildId());
            preparedStatement.setLong(2, ticketChannel.getTextChannelId());
        });
    }

    private List<Long> getStaffRoles(long serverId) {
        return new DBDataLoad<Long>("TicketStaffRole", "roleId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getList(resultSet -> resultSet.getLong(1));
    }

    private void addStaffRole(long serverId, long roleId) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO TicketStaffRole (serverId, roleId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, roleId);
        });
    }

    private void removeStaffRole(long serverId, long roleId) {
        MySQLManager.asyncUpdate("DELETE FROM TicketStaffRole WHERE serverId = ? AND roleId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, roleId);
        });
    }

}
