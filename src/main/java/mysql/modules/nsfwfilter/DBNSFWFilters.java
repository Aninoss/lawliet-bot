package mysql.modules.nsfwfilter;

import java.util.ArrayList;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBObserverMapCache;

public class DBNSFWFilters extends DBObserverMapCache<Long, NSFWFiltersData> {

    private static final DBNSFWFilters ourInstance = new DBNSFWFilters();

    public static DBNSFWFilters getInstance() {
        return ourInstance;
    }

    private DBNSFWFilters() {
    }

    @Override
    protected NSFWFiltersData load(Long serverId) throws Exception {
        NSFWFiltersData nsfwFiltersBean = new NSFWFiltersData(
                serverId,
                getKeywords(serverId)
        );

        nsfwFiltersBean.getKeywords()
                .addListAddListener(list -> list.forEach(keyword -> addKeyword(serverId, keyword)))
                .addListRemoveListener(list -> list.forEach(keyword -> removeKeyword(serverId, keyword)));

        return nsfwFiltersBean;
    }

    @Override
    protected void save(NSFWFiltersData nsfwFiltersBean) {
    }

    private ArrayList<String> getKeywords(long serverId) {
        return new DBDataLoad<String>("NSFWFilter", "keyword", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getString(1));
    }

    private void addKeyword(long serverId, String keyword) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO NSFWFilter (serverId, keyword) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setString(2, keyword);
        });
    }

    private void removeKeyword(long serverId, String keyword) {
        DBMain.getInstance().asyncUpdate("DELETE FROM NSFWFilter WHERE serverId = ? AND keyword = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setString(2, keyword);
        });
    }

}
