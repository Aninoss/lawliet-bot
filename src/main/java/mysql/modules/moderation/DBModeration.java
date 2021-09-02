package mysql.modules.moderation;

import java.sql.Types;
import java.util.List;
import java.util.Optional;
import mysql.DBDataLoad;
import mysql.DBObserverMapCache;
import mysql.MySQLManager;

public class DBModeration extends DBObserverMapCache<Long, ModerationData> {

    private static final DBModeration ourInstance = new DBModeration();

    public static DBModeration getInstance() {
        return ourInstance;
    }

    private DBModeration() {
    }

    @Override
    protected ModerationData load(Long serverId) throws Exception {
        ModerationData moderationData = MySQLManager.get(
                "SELECT channelId, question, muteRoleId, autoKick, autoBan, autoMute, autoJail, autoKickDays, autoBanDays, autoMuteDays, autoJailDays, autoBanDuration, autoMuteDuration, autoJailDuration FROM Moderation WHERE serverId = ?;",
                preparedStatement -> preparedStatement.setLong(1, serverId),
                resultSet -> {
                    if (resultSet.next()) {
                        return new ModerationData(
                                serverId,
                                resultSet.getLong(1),
                                resultSet.getBoolean(2),
                                resultSet.getLong(3),
                                resultSet.getInt(4),
                                resultSet.getInt(5),
                                resultSet.getInt(6),
                                resultSet.getInt(7),
                                resultSet.getInt(8),
                                resultSet.getInt(9),
                                resultSet.getInt(10),
                                resultSet.getInt(11),
                                resultSet.getInt(12),
                                resultSet.getInt(13),
                                resultSet.getInt(14),
                                getJailRoles(serverId)
                        );
                    } else {
                        return new ModerationData(
                                serverId,
                                null,
                                true,
                                null,
                                0,
                                0,
                                0,
                                0,
                                0,
                                0,
                                0,
                                0,
                                0,
                                0,
                                0,
                                getJailRoles(serverId)
                        );
                    }
                }
        );

        moderationData.getJailRoleIds()
                .addListAddListener(list -> list.forEach(roleId -> addJailRole(serverId, roleId)))
                .addListRemoveListener(list -> list.forEach(roleId -> removeJailRole(serverId, roleId)));

        return moderationData;
    }

    @Override
    protected void save(ModerationData moderationBean) {
        MySQLManager.asyncUpdate("REPLACE INTO Moderation (serverId, channelId, question, muteRoleId, autoKick, autoBan, autoMute, autoJail, autoKickDays, autoBanDays, autoMuteDays, autoJailDays, autoBanDuration, autoMuteDuration, autoJailDuration) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, moderationBean.getGuildId());

            Optional<Long> channelIdOpt = moderationBean.getAnnouncementChannelId();
            if (channelIdOpt.isPresent()) {
                preparedStatement.setLong(2, channelIdOpt.get());
            } else {
                preparedStatement.setNull(2, Types.BIGINT);
            }

            preparedStatement.setBoolean(3, moderationBean.isQuestion());

            Optional<Long> muteRoleOpt = moderationBean.getMuteRoleId();
            if (muteRoleOpt.isPresent()) {
                preparedStatement.setLong(4, muteRoleOpt.get());
            } else {
                preparedStatement.setNull(4, Types.BIGINT);
            }

            preparedStatement.setInt(5, moderationBean.getAutoKick());
            preparedStatement.setInt(6, moderationBean.getAutoBan());
            preparedStatement.setInt(7, moderationBean.getAutoMute());
            preparedStatement.setInt(8, moderationBean.getAutoJail());
            preparedStatement.setInt(9, moderationBean.getAutoKickDays());
            preparedStatement.setInt(10, moderationBean.getAutoBanDays());
            preparedStatement.setInt(11, moderationBean.getAutoMuteDays());
            preparedStatement.setInt(12, moderationBean.getAutoJailDays());
            preparedStatement.setInt(13, moderationBean.getAutoBanDuration());
            preparedStatement.setInt(14, moderationBean.getAutoMuteDuration());
            preparedStatement.setInt(15, moderationBean.getAutoJailDuration());
        });
    }

    private List<Long> getJailRoles(long serverId) {
        return new DBDataLoad<Long>("JailRoles", "roleId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getList(resultSet -> resultSet.getLong(1));
    }

    private void addJailRole(long serverId, long roleId) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO JailRoles (serverId, roleId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, roleId);
        });
    }

    private void removeJailRole(long serverId, long roleId) {
        MySQLManager.asyncUpdate("DELETE FROM JailRoles WHERE serverId = ? AND roleId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, roleId);
        });
    }

}
