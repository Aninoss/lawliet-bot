package mysql.modules.tempban;

import java.util.List;
import java.util.Map;
import core.CustomObservableMap;
import mysql.DBDataLoad;
import mysql.DBDataLoadAll;
import mysql.DBMapCache;
import mysql.MySQLManager;

public class DBTempBan extends DBMapCache<Long, CustomObservableMap<Long, TempBanData>> {

    private static final DBTempBan ourInstance = new DBTempBan();

    public static DBTempBan getInstance() {
        return ourInstance;
    }

    private DBTempBan() {
    }

    @Override
    protected CustomObservableMap<Long, TempBanData> load(Long guildId) throws Exception {
        Map<Long, TempBanData> tempBanMap = new DBDataLoad<TempBanData>("TempBans", "serverId, userId, expires", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, guildId)
        ).getMap(
                TempBanData::getMemberId,
                resultSet -> {
                    long serverId = resultSet.getLong(1);
                    return new TempBanData(
                            serverId,
                            resultSet.getLong(2),
                            resultSet.getTimestamp(3).toInstant()
                    );
                }
        );

        return new CustomObservableMap<>(tempBanMap)
                .addMapAddListener(this::addTempBan)
                .addMapUpdateListener(this::addTempBan)
                .addMapRemoveListener(this::removeTempBan);
    }

    public List<TempBanData> retrieveAll() {
        return new DBDataLoadAll<TempBanData>("TempBans", "serverId, userId, expires")
                .getList(
                        resultSet -> {
                            long serverId = resultSet.getLong(1);
                            return new TempBanData(
                                    serverId,
                                    resultSet.getLong(2),
                                    resultSet.getTimestamp(3).toInstant()
                            );
                        }
                );
    }

    private void addTempBan(TempBanData tempBan) {
        MySQLManager.asyncUpdate("REPLACE INTO TempBans (serverId, userId, expires) VALUES (?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, tempBan.getGuildId());
            preparedStatement.setLong(2, tempBan.getMemberId());
            preparedStatement.setString(3, MySQLManager.instantToDateTimeString(tempBan.getExpirationTime()));
        });
    }

    private void removeTempBan(TempBanData tempBan) {
        MySQLManager.asyncUpdate("DELETE FROM TempBans WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, tempBan.getGuildId());
            preparedStatement.setLong(2, tempBan.getMemberId());
        });
    }

}
