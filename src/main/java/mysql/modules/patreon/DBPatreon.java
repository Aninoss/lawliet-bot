package mysql.modules.patreon;

import mysql.DBDataLoad;
import mysql.DBSingleBeanGenerator;
import java.util.HashMap;

public class DBPatreon extends DBSingleBeanGenerator<HashMap<Long, PatreonBean>> {

    private static final DBPatreon ourInstance = new DBPatreon();
    public static DBPatreon getInstance() { return ourInstance; }
    private DBPatreon() {}

    private HashMap<Long, PatreonBean> patreonMap;

    @Override
    protected HashMap<Long, PatreonBean> loadBean() throws Exception {
        return new DBDataLoad<PatreonBean>("Patreon", "userId, tier, expires", "1",
                preparedStatement -> {}
        ).getHashMap(
                PatreonBean::getUserId,
                resultSet -> new PatreonBean(
                        resultSet.getLong(1),
                        resultSet.getInt(2),
                        resultSet.getDate(3).toLocalDate()
                )
        );
    }

}
