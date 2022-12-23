package mysql.modules.autosell;

import java.util.Map;
import mysql.DBDataLoad;
import mysql.DBSingleCache;
import mysql.MySQLManager;

public class DBAutoSell extends DBSingleCache<AutoSellData> {

    private static final DBAutoSell ourInstance = new DBAutoSell();

    public static DBAutoSell getInstance() {
        return ourInstance;
    }

    private DBAutoSell() {
    }

    @Override
    protected AutoSellData loadBean() {
        Map<Long, AutoSellSlot> autoSellMap = new DBDataLoad<AutoSellSlot>("AutoSell", "userId, threshold", "threshold IS NOT NULL")
                .getMap(
                        AutoSellSlot::getUserId,
                        resultSet -> new AutoSellSlot(
                                resultSet.getLong(1),
                                resultSet.getInt(2)
                        )
                );

        AutoSellData autoSellData = new AutoSellData(autoSellMap);
        autoSellData.getSlotMap().addMapAddListener(this::addAutoSell)
                .addMapUpdateListener(this::addAutoSell)
                .addMapRemoveListener(slot -> removeAutoSell(slot.getUserId()));

        return autoSellData;
    }

    private void addAutoSell(AutoSellSlot slot) {
        MySQLManager.asyncUpdate("REPLACE INTO AutoSell (userId, threshold) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, slot.getUserId());
            preparedStatement.setInt(2, slot.getThreshold());
        });
    }

    private void removeAutoSell(long userId) {
        MySQLManager.asyncUpdate("DELETE FROM AutoSell WHERE userId = ?;", preparedStatement -> preparedStatement.setLong(1, userId));
    }

    @Override
    public Integer getExpirationTimeMinutes() {
        return 1;
    }

}
