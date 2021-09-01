package mysql.modules.servermute;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import core.CustomObservableMap;
import mysql.DBDataLoad;
import mysql.DBDataLoadAll;
import mysql.DBMapCache;
import mysql.MySQLManager;

public class DBServerMute extends DBMapCache<Long, CustomObservableMap<Long, ServerMuteData>> {

    private static final DBServerMute ourInstance = new DBServerMute();

    public static DBServerMute getInstance() {
        return ourInstance;
    }

    private DBServerMute() {
    }

    @Override
    protected CustomObservableMap<Long, ServerMuteData> load(Long guildId) throws Exception {
        Map<Long, ServerMuteData> serverMuteMap = new DBDataLoad<ServerMuteData>("ServerMute", "serverId, userId, expires", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, guildId)
        ).getMap(
                ServerMuteData::getMemberId,
                resultSet -> {
                    Timestamp timestamp = resultSet.getTimestamp(3);
                    return new ServerMuteData(
                            resultSet.getLong(1),
                            resultSet.getLong(2),
                            timestamp != null ? timestamp.toInstant() : null
                    );
                }
        );

        return new CustomObservableMap<>(serverMuteMap)
                .addMapAddListener(this::addServerMute)
                .addMapUpdateListener(this::addServerMute)
                .addMapRemoveListener(this::removeServerMute);
    }

    public List<ServerMuteData> retrieveAll() {
        return new DBDataLoadAll<ServerMuteData>("ServerMute", "serverId, userId, expires")
                .getList(
                        resultSet -> {
                            long serverId = resultSet.getLong(1);
                            Timestamp timestamp = resultSet.getTimestamp(3);
                            return new ServerMuteData(
                                    serverId,
                                    resultSet.getLong(2),
                                    timestamp != null ? timestamp.toInstant() : null
                            );
                        }
                );
    }

    private void addServerMute(ServerMuteData serverMuteData) {
        MySQLManager.asyncUpdate("REPLACE INTO ServerMute (serverId, userId, expires) VALUES (?,?,?);", preparedStatement -> {
            preparedStatement.setLong(1, serverMuteData.getGuildId());
            preparedStatement.setLong(2, serverMuteData.getMemberId());

            Optional<Instant> expirationOpt = serverMuteData.getExpirationTime();
            if (expirationOpt.isPresent()) {
                preparedStatement.setString(3, MySQLManager.instantToDateTimeString(expirationOpt.get()));
            } else {
                preparedStatement.setNull(3, Types.TIMESTAMP);
            }
        });
    }

    private void removeServerMute(ServerMuteData serverMuteData) {
        MySQLManager.asyncUpdate("DELETE FROM ServerMute WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverMuteData.getGuildId());
            preparedStatement.setLong(2, serverMuteData.getMemberId());
        });
    }

}
