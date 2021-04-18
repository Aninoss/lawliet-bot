package mysql.modules.osuaccounts;

import java.util.HashMap;
import core.CustomObservableMap;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBSingleCache;

public class DBOsuAccounts extends DBSingleCache<CustomObservableMap<Long, OsuAccountData>> {

    private static final DBOsuAccounts ourInstance = new DBOsuAccounts();

    public static DBOsuAccounts getInstance() {
        return ourInstance;
    }

    private DBOsuAccounts() {
    }

    @Override
    protected CustomObservableMap<Long, OsuAccountData> loadBean() throws Exception {
        HashMap<Long, OsuAccountData> osuMap = new DBDataLoad<OsuAccountData>("OsuAccounts", "userId, osuId", "1")
                .getHashMap(OsuAccountData::getUserId, resultSet -> new OsuAccountData(resultSet.getLong(1), resultSet.getLong(2)));

        return new CustomObservableMap<>(osuMap)
                .addMapAddListener(this::addOsuAccount)
                .addMapRemoveListener(this::removeOsuAccount);
    }

    private void addOsuAccount(OsuAccountData osuAccountData) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO OsuAccounts (userId, osuId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, osuAccountData.getUserId());
            preparedStatement.setLong(2, osuAccountData.getOsuId());
        });
    }

    private void removeOsuAccount(OsuAccountData osuAccountData) {
        DBMain.getInstance().asyncUpdate("DELETE FROM OsuAccounts WHERE userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, osuAccountData.getUserId());
        });
    }

    @Override
    public Integer getExpirationTimeMinutes() {
        return 5;
    }

}
