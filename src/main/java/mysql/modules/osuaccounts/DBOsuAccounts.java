package mysql.modules.osuaccounts;

import java.util.HashMap;
import core.CustomObservableMap;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBSingleCache;

public class DBOsuAccounts extends DBSingleCache<CustomObservableMap<Long, OsuBeanBean>> {

    private static final DBOsuAccounts ourInstance = new DBOsuAccounts();

    public static DBOsuAccounts getInstance() {
        return ourInstance;
    }

    private DBOsuAccounts() {
    }

    @Override
    protected CustomObservableMap<Long, OsuBeanBean> loadBean() throws Exception {
        HashMap<Long, OsuBeanBean> osuMap = new DBDataLoad<OsuBeanBean>("OsuAccounts", "userId, osuId", "1")
                .getHashMap(OsuBeanBean::getUserId, resultSet -> new OsuBeanBean(resultSet.getLong(1), resultSet.getLong(2)));

        return new CustomObservableMap<>(osuMap)
                .addMapAddListener(this::addOsuAccount)
                .addMapRemoveListener(this::removeOsuAccount);
    }

    private void addOsuAccount(OsuBeanBean osuBeanBean) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO OsuAccounts (userId, osuId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, osuBeanBean.getUserId());
            preparedStatement.setLong(2, osuBeanBean.getOsuId());
        });
    }

    private void removeOsuAccount(OsuBeanBean osuBeanBean) {
        DBMain.getInstance().asyncUpdate("DELETE FROM OsuAccounts WHERE userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, osuBeanBean.getUserId());
        });
    }

    @Override
    public Integer getExpirationTimeMinutes() {
        return 5;
    }

}
