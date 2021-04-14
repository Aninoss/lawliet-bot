package mysql.modules.servermute;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import core.CustomObservableMap;
import mysql.DBDataLoad;
import mysql.DBDataLoadAll;
import mysql.DBMain;
import mysql.DBMapCache;

public class DBServerMute extends DBMapCache<Long, CustomObservableMap<Long, ServerMuteSlot>> {

    private static final DBServerMute ourInstance = new DBServerMute();

    public static DBServerMute getInstance() {
        return ourInstance;
    }

    private DBServerMute() {
    }

    @Override
    protected CustomObservableMap<Long, ServerMuteSlot> load(Long guildId) throws Exception {
        HashMap<Long, ServerMuteSlot> serverMuteMap = new DBDataLoad<ServerMuteSlot>("ServerMute", "serverId, userId, expires", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, guildId)
        ).getHashMap(
                ServerMuteSlot::getMemberId,
                resultSet -> {
                    Timestamp timestamp = resultSet.getTimestamp(3);
                    return new ServerMuteSlot(
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

    public List<ServerMuteSlot> retrieveAll() {
        return new DBDataLoadAll<ServerMuteSlot>("ServerMute", "serverId, userId, expires")
                .getArrayList(
                        resultSet -> {
                            long serverId = resultSet.getLong(1);
                            Timestamp timestamp = resultSet.getTimestamp(3);
                            return new ServerMuteSlot(
                                    serverId,
                                    resultSet.getLong(2),
                                    timestamp != null ? timestamp.toInstant() : null
                            );
                        }
                );
    }

    private void addServerMute(ServerMuteSlot serverMuteSlot) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO ServerMute (serverId, userId, expires) VALUES (?,?,?);", preparedStatement -> {
            preparedStatement.setLong(1, serverMuteSlot.getGuildId());
            preparedStatement.setLong(2, serverMuteSlot.getMemberId());

            Optional<Instant> expirationOpt = serverMuteSlot.getExpirationTime();
            if (expirationOpt.isPresent()) {
                preparedStatement.setString(3, DBMain.instantToDateTimeString(expirationOpt.get()));
            } else {
                preparedStatement.setNull(3, Types.TIMESTAMP);
            }
        });
    }

    private void removeServerMute(ServerMuteSlot serverMuteSlot) {
        DBMain.getInstance().asyncUpdate("DELETE FROM ServerMute WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverMuteSlot.getGuildId());
            preparedStatement.setLong(2, serverMuteSlot.getMemberId());
        });
    }

}
