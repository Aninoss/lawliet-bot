package mysql.modules.casinotracking;

import java.util.List;
import mysql.DBDataLoad;
import mysql.DBSingleCache;
import mysql.MySQLManager;

public class DBCasinoTracking extends DBSingleCache<CasinoTrackingData> {

    private static final DBCasinoTracking ourInstance = new DBCasinoTracking();

    public static DBCasinoTracking getInstance() {
        return ourInstance;
    }

    private DBCasinoTracking() {
    }

    @Override
    protected CasinoTrackingData loadBean() throws Exception {
        List<Long> casinoTrackingList = new DBDataLoad<Long>("CasinoTracking", "userId", "1")
                .getList(resultSet -> resultSet.getLong(1));

        CasinoTrackingData casinoTrackingData = new CasinoTrackingData(casinoTrackingList);
        casinoTrackingData.getUserList().addListAddListener(list -> list.forEach(this::addAutoTracking))
                .addListRemoveListener(list -> list.forEach(this::removeAutoTracking));

        return casinoTrackingData;
    }

    private void addAutoTracking(long userId) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO CasinoTracking (userId) VALUES (?);", preparedStatement -> preparedStatement.setLong(1, userId));
    }

    private void removeAutoTracking(long userId) {
        MySQLManager.asyncUpdate("DELETE FROM CasinoTracking WHERE userId = ?;", preparedStatement -> preparedStatement.setLong(1, userId));
    }

    @Override
    public Integer getExpirationTimeMinutes() {
        return 1;
    }

}
