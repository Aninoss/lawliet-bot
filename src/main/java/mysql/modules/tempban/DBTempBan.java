package mysql.modules.tempban;

import java.util.HashMap;
import java.util.List;
import core.CustomObservableMap;
import mysql.DBDataLoad;
import mysql.DBDataLoadAll;
import mysql.DBMain;
import mysql.DBMapCache;

public class DBTempBan extends DBMapCache<Long, CustomObservableMap<Long, TempBanSlot>> {

    private static final DBTempBan ourInstance = new DBTempBan();

    public static DBTempBan getInstance() {
        return ourInstance;
    }

    private DBTempBan() {
    }

    @Override
    protected CustomObservableMap<Long, TempBanSlot> load(Long guildId) throws Exception {
        HashMap<Long, TempBanSlot> tempBanMap = new DBDataLoad<TempBanSlot>("TempBans", "serverId, userId, expires", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, guildId)
        ).getHashMap(
                TempBanSlot::getMemberId,
                resultSet -> {
                    long serverId = resultSet.getLong(1);
                    return new TempBanSlot(
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

    public List<TempBanSlot> retrieveAll() {
        return new DBDataLoadAll<TempBanSlot>("TempBans", "serverId, userId, expires")
                .getArrayList(
                        resultSet -> {
                            long serverId = resultSet.getLong(1);
                            return new TempBanSlot(
                                    serverId,
                                    resultSet.getLong(2),
                                    resultSet.getTimestamp(3).toInstant()
                            );
                        }
                );
    }

    private void addTempBan(TempBanSlot tempBan) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO TempBans (serverId, userId, expires) VALUES (?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, tempBan.getGuildId());
            preparedStatement.setLong(2, tempBan.getMemberId());
            preparedStatement.setString(3, DBMain.instantToDateTimeString(tempBan.getExpirationTime()));
        });
    }

    private void removeTempBan(TempBanSlot tempBan) {
        DBMain.getInstance().asyncUpdate("DELETE FROM TempBans WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, tempBan.getGuildId());
            preparedStatement.setLong(2, tempBan.getMemberId());
        });
    }

}
