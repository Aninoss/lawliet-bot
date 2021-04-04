package mysql.modules.tempban;

import java.util.HashMap;
import java.util.List;
import core.CustomObservableMap;
import core.ShardManager;
import mysql.DBDataLoad;
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

        CustomObservableMap<Long, TempBanSlot> tempBanBean = new CustomObservableMap<>(tempBanMap);
        tempBanBean.addMapAddListener(this::addTempBan)
                .addMapUpdateListener(this::addTempBan)
                .addMapRemoveListener(this::removeTempBan);

        return tempBanBean;
    }

    public List<TempBanSlot> retrieveAll() {
        return new DBDataLoad<TempBanSlot>("TempBans", "serverId, userId, expires", "(serverId >> 22) % ? >= ? AND (serverId >> 22) % ? <= ?",
                preparedStatement -> {
                    preparedStatement.setInt(1, ShardManager.getInstance().getTotalShards());
                    preparedStatement.setInt(2, ShardManager.getInstance().getShardIntervalMin());
                    preparedStatement.setInt(3, ShardManager.getInstance().getTotalShards());
                    preparedStatement.setInt(4, ShardManager.getInstance().getShardIntervalMax());
                }
        ).getArrayList(
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
