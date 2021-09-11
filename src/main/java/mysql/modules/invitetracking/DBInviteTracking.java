package mysql.modules.invitetracking;

import java.util.Map;
import mysql.DBDataLoad;
import mysql.DBMapCache;
import mysql.MySQLManager;

public class DBInviteTracking extends DBMapCache<Long, InviteTrackingData> {

    private static final DBInviteTracking ourInstance = new DBInviteTracking();

    public static DBInviteTracking getInstance() {
        return ourInstance;
    }

    private DBInviteTracking() {
    }

    @Override
    protected InviteTrackingData load(Long guildId) throws Exception {
        InviteTrackingData inviteTrackerData = new InviteTrackingData(
                guildId,
                getInviteTrackerSlots(guildId),
                getGuildInvites(guildId)
        );

        inviteTrackerData.getInviteTrackingSlots()
                .addMapAddListener(this::addInviteTrackerSlot)
                .addMapUpdateListener(this::addInviteTrackerSlot)
                .addMapRemoveListener(this::removeInviteTrackerSlot);
        inviteTrackerData.getGuildInvites()
                .addMapAddListener(this::addGuildInvite)
                .addMapRemoveListener(this::removeGuildInvite);

        return inviteTrackerData;
    }

    private Map<Long, InviteTrackingSlot> getInviteTrackerSlots(long guildId) {
        return new DBDataLoad<InviteTrackingSlot>("Invites", "userId, invitedByUserId, date, lastMessage", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, guildId)
        ).getMap(
                InviteTrackingSlot::getMemberId,
                resultSet -> new InviteTrackingSlot(
                        guildId,
                        resultSet.getLong(1),
                        resultSet.getLong(2),
                        resultSet.getDate(3).toLocalDate(),
                        resultSet.getDate(4).toLocalDate()
                )
        );
    }

    private void addInviteTrackerSlot(InviteTrackingSlot slot) {
        MySQLManager.asyncUpdate("REPLACE INTO Invites (serverId, userId, invitedByUserId, date, lastMessage) VALUES (?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, slot.getGuildId());
            preparedStatement.setLong(2, slot.getMemberId());
            preparedStatement.setLong(3, slot.getInviterUserId());
            preparedStatement.setString(4, MySQLManager.localDateToDateString(slot.getInvitedDate()));
            preparedStatement.setString(5, MySQLManager.localDateToDateString(slot.getLastMessage()));
        });
    }

    private void removeInviteTrackerSlot(InviteTrackingSlot slot) {
        MySQLManager.asyncUpdate("DELETE FROM Invites WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, slot.getGuildId());
            preparedStatement.setLong(2, slot.getMemberId());
        });
    }

    private Map<String, GuildInvite> getGuildInvites(long guildId) {
        return new DBDataLoad<GuildInvite>("ServerInvites", "code, userId, usages", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, guildId)
        ).getMap(
                GuildInvite::getCode,
                resultSet -> new GuildInvite(
                        guildId,
                        resultSet.getString(1),
                        resultSet.getLong(2),
                        resultSet.getInt(3)
                )
        );
    }

    private void addGuildInvite(GuildInvite guildInvite) {
        MySQLManager.asyncUpdate("REPLACE INTO ServerInvites (serverId, code, userId, usages) VALUES (?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, guildInvite.getGuildId());
            preparedStatement.setString(2, guildInvite.getCode());
            preparedStatement.setLong(3, guildInvite.getMemberId());
            preparedStatement.setInt(4, guildInvite.getUses());
        });
    }

    private void removeGuildInvite(GuildInvite guildInvite) {
        MySQLManager.asyncUpdate("DELETE FROM ServerInvites WHERE serverId = ? AND code = ?;", preparedStatement -> {
            preparedStatement.setLong(1, guildInvite.getGuildId());
            preparedStatement.setString(2, guildInvite.getCode());
        });
    }

}
