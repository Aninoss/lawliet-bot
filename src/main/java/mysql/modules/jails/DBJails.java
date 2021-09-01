package mysql.modules.jails;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import core.CustomObservableMap;
import core.MainLogger;
import mysql.*;

public class DBJails extends DBMapCache<Long, CustomObservableMap<Long, JailData>> {

    private static final DBJails ourInstance = new DBJails();

    public static DBJails getInstance() {
        return ourInstance;
    }

    private DBJails() {
    }

    @Override
    protected CustomObservableMap<Long, JailData> load(Long guildId) throws Exception {
        Map<Long, JailData> jailMap = new DBDataLoad<JailData>("Jails", "userId, expires", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, guildId)
        ).getMap(
                JailData::getMemberId,
                resultSet -> {
                    long userId = resultSet.getLong(1);
                    Timestamp timestamp = resultSet.getTimestamp(2);
                    return new JailData(
                            guildId,
                            userId,
                            timestamp != null ? timestamp.toInstant() : null,
                            getJailPreviousRoles(guildId, userId)
                    );
                }
        );

        return new CustomObservableMap<>(jailMap)
                .addMapAddListener(this::addJail)
                .addMapUpdateListener(this::addJail)
                .addMapRemoveListener(this::removeJail);
    }

    public List<JailData> retrieveAll() {
        return new DBDataLoadAll<JailData>("Jails", "serverId, userId, expires")
                .getList(
                        resultSet -> {
                            long serverId = resultSet.getLong(1);
                            long userId = resultSet.getLong(2);
                            Timestamp timestamp = resultSet.getTimestamp(3);
                            return new JailData(
                                    serverId,
                                    userId,
                                    timestamp != null ? timestamp.toInstant() : null,
                                    getJailPreviousRoles(serverId, userId)
                            );
                        }
                );
    }

    private void addJail(JailData jailData) {
        MySQLManager.asyncUpdate("REPLACE INTO Jails (serverId, userId, expires) VALUES (?,?,?);", preparedStatement -> {
            preparedStatement.setLong(1, jailData.getGuildId());
            preparedStatement.setLong(2, jailData.getMemberId());

            Optional<Instant> expirationOpt = jailData.getExpirationTime();
            if (expirationOpt.isPresent()) {
                preparedStatement.setString(3, MySQLManager.instantToDateTimeString(expirationOpt.get()));
            } else {
                preparedStatement.setNull(3, Types.TIMESTAMP);
            }
        }).thenAccept(i -> addJailPreviousRoles(jailData));
    }

    private void removeJail(JailData jailData) {
        MySQLManager.asyncUpdate("DELETE FROM Jails WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, jailData.getGuildId());
            preparedStatement.setLong(2, jailData.getMemberId());
        });
    }

    private List<Long> getJailPreviousRoles(long serverId, long userId) {
        return new DBDataLoad<Long>("JailRemainRoles", "roleId", "serverId = ? AND userId = ?", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
        }).getList(resultSet -> resultSet.getLong(1));
    }

    private void addJailPreviousRoles(JailData jailData) {
        try {
            MySQLManager.update("DELETE FROM JailRemainRoles WHERE serverId = ? AND userId = ?;", preparedStatement -> {
                preparedStatement.setLong(1, jailData.getGuildId());
                preparedStatement.setLong(2, jailData.getMemberId());
            });
        } catch (SQLException | InterruptedException e) {
            MainLogger.get().error("SQL Exception", e);
        }

        if (jailData.getPreviousRoleIds().size() > 0) {
            try (DBBatch batch = new DBBatch("INSERT IGNORE INTO JailRemainRoles (serverId, userId, roleId) VALUES (?, ?, ?)")) {
                for (long roleId : jailData.getPreviousRoleIds()) {
                    batch.add(preparedStatement -> {
                        preparedStatement.setLong(1, jailData.getGuildId());
                        preparedStatement.setLong(2, jailData.getMemberId());
                        preparedStatement.setLong(3, roleId);
                    });
                }
                batch.execute();
            } catch (SQLException e) {
                MainLogger.get().error("SQL Exception", e);
            }
        }
    }

}
