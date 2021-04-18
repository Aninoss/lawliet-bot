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

public class DBServerMute extends DBMapCache<Long, CustomObservableMap<Long, ServerMuteData>> {

    private static final DBServerMute ourInstance = new DBServerMute();

    public static DBServerMute getInstance() {
        return ourInstance;
    }

    private DBServerMute() {
    }

    @Override
    protected CustomObservableMap<Long, ServerMuteData> load(Long guildId) throws Exception {
        HashMap<Long, ServerMuteData> serverMuteMap = new DBDataLoad<ServerMuteData>("ServerMute", "serverId, userId, expires", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, guildId)
        ).getHashMap(
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
                .getArrayList(
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
        DBMain.getInstance().asyncUpdate("REPLACE INTO ServerMute (serverId, userId, expires) VALUES (?,?,?);", preparedStatement -> {
            preparedStatement.setLong(1, serverMuteData.getGuildId());
            preparedStatement.setLong(2, serverMuteData.getMemberId());

            Optional<Instant> expirationOpt = serverMuteData.getExpirationTime();
            if (expirationOpt.isPresent()) {
                preparedStatement.setString(3, DBMain.instantToDateTimeString(expirationOpt.get()));
            } else {
                preparedStatement.setNull(3, Types.TIMESTAMP);
            }
        });
    }

    private void removeServerMute(ServerMuteData serverMuteData) {
        DBMain.getInstance().asyncUpdate("DELETE FROM ServerMute WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverMuteData.getGuildId());
            preparedStatement.setLong(2, serverMuteData.getMemberId());
        });
    }

}
